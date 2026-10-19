package app.lapse.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerEngineTest {
    private val engine = TrackerEngine()

    @Test
    fun instagramTimeAndAReelLandTogether() {
        val open = engine.observe(instagram(atMs = 1_000, author = "ada"), paused = false)
        assertEquals(1, open.reels)
        assertEquals(0L, open.time.instagramMs)

        val stay = engine.observe(instagram(atMs = 4_000, author = "ada"), paused = false)
        assertEquals(0, stay.reels)
        assertEquals(3_000L, stay.time.instagramMs)

        val next = engine.observe(instagram(atMs = 6_000, author = "bob"), paused = false)
        assertEquals(1, next.reels)
        assertEquals(Feed.INSTAGRAM_REELS, next.feed)
    }

    @Test
    fun pauseKeepsTimeUpToThatMomentAndThenStops() {
        engine.observe(instagram(atMs = 1_000, author = "ada"), paused = false)
        val paused = engine.onPauseOrScreenOff(3_000)
        assertEquals(2_000L, paused.time.instagramMs)

        val during = engine.heartbeat(8_000, Packages.INSTAGRAM, paused = true, screenOn = true)
        assertEquals(0L, during.time.instagramMs)
        assertEquals(0, during.reels)
    }

    @Test
    fun screenOffEndsTheSession() {
        engine.observe(youtubeShort(atMs = 1_000), paused = false)
        val off = engine.observe(youtubeShort(atMs = 5_000).copy(screenOn = false), paused = false)
        assertEquals(4_000L, off.time.youtubeMs)
        val later = engine.heartbeat(9_000, Packages.YOUTUBE, paused = false, screenOn = false)
        assertEquals(0L, later.time.youtubeMs)
    }

    @Test
    fun leavingForAnotherAppStopsTheClock() {
        engine.observe(instagram(atMs = 1_000, author = "ada"), paused = false)
        val left = engine.observe(Observation("com.android.chrome", atMs = 3_500), paused = false)
        assertEquals(2_500L, left.time.instagramMs)
        assertEquals(Surface.NONE, left.surface)
    }

    @Test
    fun heartbeatAddsTimeWithoutAddingScrolls() {
        engine.observe(instagram(atMs = 1_000, author = "ada"), paused = false)
        val beat = engine.heartbeat(3_000, Packages.INSTAGRAM, paused = false, screenOn = true)
        assertEquals(2_000L, beat.time.instagramMs)
        assertEquals(0, beat.reels)
    }

    @Test
    fun redditCountsPostsScrolledPastAndNotTheOpeningScreen() {
        val open = engine.observe(reddit(atMs = 1_000, fromIndex = 0), paused = false)
        assertEquals(0, open.posts)
        val passed = engine.observe(reddit(atMs = 3_000, fromIndex = 4), paused = false)
        assertEquals(4, passed.posts)
        assertEquals(2_000L, passed.time.redditMs)
    }

    @Test
    fun youtubeOutsideShortsCountsTimeOnly() {
        val open = engine.observe(
            Observation(Packages.YOUTUBE, atMs = 1_000, signals = listOf(NodeSignal(text = "Home", visible = true, width = 100, height = 40))),
            paused = false,
        )
        assertEquals(0, open.shorts)
        val later = engine.heartbeat(4_000, Packages.YOUTUBE, paused = false, screenOn = true)
        assertEquals(3_000L, later.time.youtubeMs)
        assertEquals(0, later.shorts)
    }

    private fun instagram(atMs: Long, author: String) = Observation(
        packageName = Packages.INSTAGRAM,
        atMs = atMs,
        signals = listOf(
            NodeSignal(
                viewId = "com.instagram.android:id/clips_viewer_view_pager",
                description = "Reel by $author",
                visible = true,
                width = 1080,
                height = 2000,
            ),
        ),
    )

    private fun youtubeShort(atMs: Long) = Observation(
        packageName = Packages.YOUTUBE,
        atMs = atMs,
        signals = listOf(
            NodeSignal(
                viewId = "com.google.android.youtube:id/reel_recycler",
                description = "Short by Channel",
                visible = true,
                width = 1080,
                height = 2000,
            ),
        ),
    )

    private fun reddit(atMs: Long, fromIndex: Int) = Observation(
        packageName = Packages.REDDIT,
        atMs = atMs,
        scroll = ScrollSignal(
            className = "androidx.recyclerview.widget.RecyclerView",
            fromIndex = fromIndex,
            toIndex = fromIndex + 3,
            itemCount = 50,
            viewportHeight = 2000,
        ),
    )
}
