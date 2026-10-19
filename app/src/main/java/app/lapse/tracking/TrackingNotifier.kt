package app.lapse.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.lapse.MainActivity
import app.lapse.R

class TrackingNotifier(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Pause or resume Lapse while it is counting."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(paused: Boolean): Notification {
        val actionIntent = Intent(context, TrackingActionReceiver::class.java).apply {
            action = if (paused) ACTION_RESUME else ACTION_PAUSE
        }
        val action = NotificationCompat.Action.Builder(
            0,
            if (paused) "Resume" else "Pause",
            pendingBroadcast(actionIntent),
        ).build()
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lapse)
            .setContentTitle(if (paused) "Lapse is paused" else "Lapse is tracking")
            .setContentText(
                if (paused) {
                    "Time and scrolls are waiting until you resume."
                } else {
                    "Instagram, YouTube, and Reddit."
                },
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(action)
            .build()
    }

    fun notify(paused: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, build(paused))
    }

    private fun pendingBroadcast(intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            if (intent.action == ACTION_RESUME) 2 else 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "lapse_tracking"
        const val NOTIFICATION_ID = 41
        const val ACTION_PAUSE = "app.lapse.action.PAUSE"
        const val ACTION_RESUME = "app.lapse.action.RESUME"
    }
}
