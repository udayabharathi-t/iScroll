package app.lapse.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PostPassCounterTest {
    private val counter = PostPassCounter()

    @Test
    fun theFirstScreenIsNotScrolledPast() {
        assertEquals(0, counter.onFeed(true, fromIndex = 0, visibleKeys = emptyList(), nowMs = 1_000))
    }

    @Test
    fun movingTheFirstVisibleIndexCountsThePostsThatLeft() {
        counter.onFeed(true, fromIndex = 0, visibleKeys = emptyList(), nowMs = 1_000)
        assertEquals(3, counter.onFeed(true, fromIndex = 3, visibleKeys = emptyList(), nowMs = 2_000))
    }

    @Test
    fun scrollingBackUpDoesNotCount() {
        counter.onFeed(true, fromIndex = 4, visibleKeys = emptyList(), nowMs = 1_000)
        counter.onFeed(true, fromIndex = 8, visibleKeys = emptyList(), nowMs = 2_000)
        assertEquals(0, counter.onFeed(true, fromIndex = 5, visibleKeys = emptyList(), nowMs = 3_000))
    }

    @Test
    fun aWildIndexJumpIsCapped() {
        counter.onFeed(true, fromIndex = 0, visibleKeys = emptyList(), nowMs = 1_000)
        assertEquals(12, counter.onFeed(true, fromIndex = 80, visibleKeys = emptyList(), nowMs = 2_000))
    }

    @Test
    fun keysCountPostsThatLeaveTheTopWhenIndexesAreMissing() {
        counter.onFeed(true, fromIndex = -1, visibleKeys = listOf("a", "b", "c"), nowMs = 1_000)
        assertEquals(2, counter.onFeed(true, fromIndex = -1, visibleKeys = listOf("c", "d", "e"), nowMs = 2_000))
    }

    @Test
    fun aShortGapDoesNotForgetThePlaceInTheFeed() {
        counter.onFeed(true, fromIndex = 2, visibleKeys = emptyList(), nowMs = 1_000)
        counter.onFeed(false, fromIndex = -1, visibleKeys = emptyList(), nowMs = 1_400)
        assertEquals(2, counter.onFeed(true, fromIndex = 4, visibleKeys = emptyList(), nowMs = 2_000))
    }
}
