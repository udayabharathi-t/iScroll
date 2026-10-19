package app.lapse.tracking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import app.lapse.LapseApp
import app.lapse.data.DayReport
import app.lapse.engine.EngineEffect
import app.lapse.engine.Feed
import app.lapse.engine.Observation
import app.lapse.engine.Packages
import app.lapse.engine.TrackerEngine
import app.lapse.engine.plural
import java.time.LocalDate

/**
 * Counts foreground time and scrolls inside Instagram, YouTube, and Reddit.
 *
 * Events from every other package are used only to see that the user left.
 * Their node trees are never read. Labels from the three tracked apps are
 * discarded after the counter decides whether a reel, Short, or post changed.
 */
class LapseAccessibilityService : AccessibilityService() {
    private val engine = TrackerEngine()
    private val reader = SnapshotReader()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notifier: TrackingNotifier
    private lateinit var chip: CounterChip

    private var screenOn = true
    private var lastPackage: String? = null
    private var lastSignals = emptyList<app.lapse.engine.NodeSignal>()
    private var lastWalkAt = 0L
    private var chipFeed = Feed.NONE
    private var chipFeedUntil = 0L
    private var receiverRegistered = false
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var started = false

    private val beat = object : Runnable {
        override fun run() {
            try {
                onHeartbeat()
            } finally {
                handler.postDelayed(this, HEARTBEAT_MS)
            }
        }
    }

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    apply(engine.onPauseOrScreenOff(System.currentTimeMillis()))
                    chip.hide()
                }
                Intent.ACTION_SCREEN_ON -> screenOn = true
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifier = TrackingNotifier(this)
        chip = CounterChip(this)
        notifier.ensureChannel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as LapseApp
        screenOn = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        prefListener = app.preferences.listen { onPreferencesChanged() }
        registerScreenReceiver()
        handler.removeCallbacks(beat)
        handler.post(beat)
        started = true
        if (DeviceStatus.canPostForegroundNotification(this)) {
            ContextCompat.startForegroundService(this, Intent(this, javaClass))
        }
        Log.i(TAG, "Accessibility connected")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DeviceStatus.canPostForegroundNotification(this)) {
            becomeForeground()
        }
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !started) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        val now = System.currentTimeMillis()
        val prefs = (application as LapseApp).preferences
        if (!screenOn || prefs.paused) {
            apply(
                engine.observe(Observation(packageName, now, screenOn = screenOn), paused = prefs.paused),
                touchChip = false,
            )
            clearChip()
            return
        }
        if (!Packages.isTracked(packageName)) {
            lastPackage = packageName
            apply(engine.observe(Observation(packageName, now, screenOn = true), paused = false), touchChip = false)
            clearChip()
            return
        }
        lastPackage = packageName
        val scroll = if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            reader.readScroll(
                source = event.source,
                eventFromIndex = event.fromIndex,
                eventToIndex = event.toIndex,
                itemCount = event.itemCount,
                scrollDeltaY = event.scrollDeltaY,
                includeKeys = packageName == Packages.REDDIT,
            )
        } else {
            null
        }
        val signals = if (shouldWalk(event.eventType, now)) {
            lastWalkAt = now
            reader.read(rootInActiveWindow).also { lastSignals = it }
        } else {
            lastSignals
        }
        apply(
            engine.observe(
                Observation(
                    packageName = packageName,
                    atMs = now,
                    signals = signals,
                    scroll = scroll,
                    screenOn = true,
                ),
                paused = false,
            ),
        )
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun onHeartbeat() {
        if (!started) return
        val now = System.currentTimeMillis()
        val prefs = (application as LapseApp).preferences
        val foreground = if (screenOn && !prefs.paused) foregroundPackage() else null
        apply(engine.heartbeat(now, foreground, prefs.paused, screenOn), touchChip = false)
        if (!screenOn || prefs.paused || foreground == null || !Packages.isTracked(foreground)) {
            clearChip()
        }
    }

    private fun onPreferencesChanged() {
        handler.post {
            if (!started) return@post
            val prefs = (application as LapseApp).preferences
            if (prefs.paused) {
                apply(engine.onPauseOrScreenOff(System.currentTimeMillis()))
                chip.hide()
            }
            if (!prefs.overlayEnabled) chip.hide()
            if (DeviceStatus.canPostForegroundNotification(this)) {
                runCatching { notifier.notify(prefs.paused) }
            }
            PauseTileService.requestRefresh(this)
        }
    }

    private fun foregroundPackage(): String? {
        val root = rootInActiveWindow
        val seen = try {
            root?.packageName?.toString()
        } catch (_: RuntimeException) {
            null
        } finally {
            root?.recycle()
        }
        if (seen.isNullOrBlank() || seen == packageName) return lastPackage
        lastPackage = seen
        return seen
    }

    private fun apply(effect: EngineEffect, touchChip: Boolean = true) {
        if (effect.hasAnything) {
            runCatching {
                (application as LapseApp).store.add(
                    day = LocalDate.now().toString(),
                    instagramMs = effect.time.instagramMs,
                    youtubeMs = effect.time.youtubeMs,
                    redditMs = effect.time.redditMs,
                    reels = effect.reels,
                    shorts = effect.shorts,
                    posts = effect.posts,
                )
            }.onFailure { error ->
                Log.w(TAG, "Could not write the daily report", error)
            }
        }
        if (touchChip) renderChip(effect.feed)
    }

    private fun renderChip(feed: Feed) {
        val prefs = (application as LapseApp).preferences
        val now = System.currentTimeMillis()
        val shown = if (feed != Feed.NONE) {
            chipFeed = feed
            chipFeedUntil = now + CHIP_HOLD_MS
            feed
        } else if (now < chipFeedUntil) {
            chipFeed
        } else {
            chipFeed = Feed.NONE
            Feed.NONE
        }
        if (!prefs.overlayEnabled || prefs.paused || !screenOn || shown == Feed.NONE) {
            if (!prefs.overlayEnabled || prefs.paused || !screenOn) clearChip()
            else chip.hide()
            return
        }
        val report = runCatching { (application as LapseApp).store.today() }.getOrNull() ?: return
        val text = chipText(shown, report) ?: return
        chip.show(text)
    }

    private fun clearChip() {
        chipFeed = Feed.NONE
        chipFeedUntil = 0L
        if (::chip.isInitialized) chip.hide()
    }

    private fun shouldWalk(eventType: Int, now: Long): Boolean {
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return true
        }
        return now - lastWalkAt >= WALK_GAP_MS
    }

    private fun becomeForeground() {
        val paused = (application as LapseApp).preferences.paused
        val notification = notifier.build(paused)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    TrackingNotifier.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(TrackingNotifier.NOTIFICATION_ID, notification)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Foreground notification unavailable", error)
        }
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private fun teardown() {
        if (!started && !receiverRegistered) return
        started = false
        handler.removeCallbacks(beat)
        prefListener?.let { listener ->
            runCatching { (application as? LapseApp)?.preferences?.stopListening(listener) }
        }
        prefListener = null
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        if (::chip.isInitialized) chip.hide()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        Log.i(TAG, "Accessibility stopped")
    }

    companion object {
        private const val TAG = "Lapse"
        private const val HEARTBEAT_MS = 2_000L
        private const val WALK_GAP_MS = 200L
        private const val CHIP_HOLD_MS = 1_500L

        fun chipText(feed: Feed, report: DayReport): String? = when (feed) {
            Feed.INSTAGRAM_REELS -> plural(report.reels, "reel", "reels")
            Feed.YOUTUBE_SHORTS -> plural(report.shorts, "short", "shorts")
            Feed.REDDIT_FEED -> plural(report.posts, "post", "posts")
            Feed.NONE -> null
        }
    }
}
