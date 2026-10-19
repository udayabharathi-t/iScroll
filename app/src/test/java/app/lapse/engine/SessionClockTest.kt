package app.lapse.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionClockTest {
    private val clock = SessionClock(maxGapMs = 8_000)

    @Test
    fun timeStartsAtTheFirstTick() {
        assertEquals(0L, clock.tick(Surface.INSTAGRAM, 1_000).instagramMs)
        assertEquals(5_000L, clock.tick(Surface.INSTAGRAM, 6_000).instagramMs)
    }

    @Test
    fun switchingAppsGivesTheGapToTheAppThatWasOpen() {
        clock.tick(Surface.INSTAGRAM, 1_000)
        val switched = clock.tick(Surface.YOUTUBE, 4_000)
        assertEquals(3_000L, switched.instagramMs)
        assertEquals(0L, switched.youtubeMs)
        assertEquals(2_000L, clock.tick(Surface.YOUTUBE, 6_000).youtubeMs)
    }

    @Test
    fun aLongGapIsDropped() {
        clock.tick(Surface.REDDIT, 1_000)
        assertEquals(0L, clock.tick(Surface.REDDIT, 30_000).redditMs)
    }

    @Test
    fun stopCreditsTheOpenSegment() {
        clock.tick(Surface.YOUTUBE, 1_000)
        assertEquals(2_500L, clock.stop(3_500).youtubeMs)
        assertEquals(0L, clock.stop(9_000).youtubeMs)
    }
}
