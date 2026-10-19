package app.lapse.engine

/**
 * Counts a move to another reel or Short.
 *
 * The first confirmed frame of a visit counts as the item on screen. A later
 * label for that same item does not count again. A new label, a page change,
 * or enough vertical movement to cross the viewer counts as one more. Small
 * scroll events are added together, because a reel swipe often arrives as many
 * short deltas. Events closer than [minGapMs] collapse into a single count.
 */
class ItemAdvanceDetector(
    private val minGapMs: Long = 450,
    private val holdMs: Long = 1_500,
) {
    private var inFeed = false
    private var lastSignature: String? = null
    private var lastCountAt: Long? = null
    private var lastActiveAt = 0L
    private var awaitingSignature = false
    private var lastFrom = -1
    private var scrollAccum = 0

    /** Reels or Shorts counted since this visit started. */
    var session: Int = 0
        private set

    fun onSample(
        active: Boolean,
        signature: String?,
        scrollDeltaY: Int,
        viewportHeight: Int,
        scrollConfirms: Boolean,
        nowMs: Long,
        fromIndex: Int = -1,
        scrollDeltaX: Int = 0,
    ): Int {
        val signatureChanged = !signature.isNullOrBlank() && signature != lastSignature
        if (!active) {
            if (inFeed && nowMs - lastActiveAt < holdMs) {
                if (signatureChanged && awaitingSignature) {
                    lastSignature = signature
                    awaitingSignature = false
                    return 0
                }
                if (signatureChanged) {
                    lastSignature = signature
                    return count(nowMs)
                }
                return 0
            }
            reset()
            return 0
        }

        lastActiveAt = nowMs
        val vertical = kotlin.math.abs(scrollDeltaY) >= kotlin.math.abs(scrollDeltaX)
        val largeScroll = scrollConfirms && vertical &&
            viewportHeight >= 400 &&
            kotlin.math.abs(scrollDeltaY) >= (viewportHeight * 0.45f).toInt()
        val indexStep = indexStep(fromIndex, vertical, scrollConfirms)

        if (!inFeed) {
            inFeed = true
            if (!signature.isNullOrBlank()) lastSignature = signature
            awaitingSignature = signature.isNullOrBlank()
            scrollAccum = 0
            return count(nowMs, 1)
        }

        if (signatureChanged && awaitingSignature && !largeScroll && indexStep == 0) {
            lastSignature = signature
            awaitingSignature = false
            return 0
        }

        var pages = if (signatureChanged) 1 else 0
        if (indexStep > pages) pages = indexStep
        if (largeScroll && pages < 1) pages = 1
        if (!largeScroll) {
            val scrolled = accumulatedPages(scrollConfirms, vertical, scrollDeltaY, viewportHeight)
            if (scrolled > pages) pages = scrolled
        }
        if (!signature.isNullOrBlank()) lastSignature = signature
        if (pages > 0) {
            awaitingSignature = false
            return count(nowMs, pages)
        }
        return 0
    }

    fun reset() {
        inFeed = false
        lastSignature = null
        awaitingSignature = false
        lastFrom = -1
        scrollAccum = 0
        session = 0
    }

    private fun indexStep(fromIndex: Int, vertical: Boolean, scrollConfirms: Boolean): Int {
        val step = if (scrollConfirms && vertical && fromIndex >= 0 && lastFrom >= 0) {
            (fromIndex - lastFrom).coerceIn(0, 4)
        } else {
            0
        }
        if (fromIndex >= 0) lastFrom = fromIndex
        return step
    }

    private fun accumulatedPages(
        scrollConfirms: Boolean,
        vertical: Boolean,
        scrollDeltaY: Int,
        viewportHeight: Int,
    ): Int {
        if (!scrollConfirms || !vertical || viewportHeight < 400 || scrollDeltaY == 0) return 0
        scrollAccum += kotlin.math.abs(scrollDeltaY)
        val threshold = (viewportHeight * 0.5f).toInt().coerceAtLeast(1)
        if (scrollAccum < threshold) return 0
        val pages = (scrollAccum / threshold).coerceIn(1, 3)
        scrollAccum %= threshold
        return pages
    }

    private fun count(nowMs: Long, amount: Int = 1): Int {
        if (amount <= 0) return 0
        val previous = lastCountAt
        if (previous != null && nowMs - previous < minGapMs) return 0
        lastCountAt = nowMs
        session += amount
        scrollAccum = 0
        return amount
    }
}
