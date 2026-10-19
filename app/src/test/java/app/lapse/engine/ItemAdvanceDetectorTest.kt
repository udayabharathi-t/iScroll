package app.lapse.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemAdvanceDetectorTest {
    private val detector = ItemAdvanceDetector(minGapMs = 450, holdMs = 1_500)

    @Test
    fun openingTheViewerCountsTheItemOnScreen() {
        assertEquals(1, sample(active = true, signature = "ig:ada", now = 1_000))
    }

    @Test
    fun theSameSignatureDoesNotCountAgain() {
        sample(active = true, signature = "ig:ada", now = 1_000)
        assertEquals(0, sample(active = true, signature = "ig:ada", now = 2_000))
    }

    @Test
    fun aNewSignatureCounts() {
        sample(active = true, signature = "ig:ada", now = 1_000)
        assertEquals(1, sample(active = true, signature = "ig:bob", now = 2_000))
    }

    @Test
    fun aLateLabelForTheFirstItemDoesNotDoubleCount() {
        assertEquals(1, sample(active = true, signature = null, now = 1_000))
        assertEquals(0, sample(active = true, signature = "ig:ada", now = 1_400))
        assertEquals(0, sample(active = true, signature = "ig:ada", now = 3_000))
    }

    @Test
    fun aLargePagerScrollCounts() {
        sample(active = true, signature = null, now = 1_000)
        assertEquals(
            1,
            sample(active = true, signature = null, delta = 1_200, height = 2_000, confirms = true, now = 2_000),
        )
    }

    @Test
    fun aSmallJitterScrollDoesNotCount() {
        sample(active = true, signature = "ig:ada", now = 1_000)
        assertEquals(
            0,
            sample(active = true, signature = "ig:ada", delta = 40, height = 2_000, confirms = true, now = 2_000),
        )
    }

    @Test
    fun aScrollOutsideTheViewerDoesNotCount() {
        sample(active = true, signature = "ig:ada", now = 1_000)
        assertEquals(
            0,
            sample(active = true, signature = "ig:ada", delta = 1_800, height = 2_000, confirms = false, now = 2_000),
        )
    }

    @Test
    fun twoSignalsFromOneSwipeCollapse() {
        assertEquals(1, sample(active = true, signature = "ig:ada", now = 1_000))
        assertEquals(
            0,
            sample(active = true, signature = "ig:bob", delta = 1_800, height = 2_000, confirms = true, now = 1_200),
        )
    }

    @Test
    fun aBriefDetectionGapDoesNotCountTheSameItemAgain() {
        sample(active = true, signature = "ig:ada", now = 1_000)
        sample(active = false, signature = null, now = 1_400)
        assertEquals(0, sample(active = true, signature = "ig:ada", now = 1_800))
    }

    @Test
    fun leavingAndComingBackLaterCountsANewVisit() {
        sample(active = true, signature = "ig:ada", now = 1_000)
        sample(active = false, signature = null, now = 4_000)
        assertEquals(1, sample(active = true, signature = "ig:ada", now = 5_000))
    }

    private fun sample(
        active: Boolean,
        signature: String?,
        delta: Int = 0,
        height: Int = 0,
        confirms: Boolean = false,
        now: Long,
    ): Int = detector.onSample(active, signature, delta, height, confirms, now)
}
