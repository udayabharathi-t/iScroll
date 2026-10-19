package app.lapse.engine

/**
 * Counts a move to another reel or Short.
 *
 * The first confirmed frame of a visit counts as the item on screen. A later
 * label for that same item does not count again. A new label, or a pager scroll
 * that covers most of the viewer, counts as one more. Events closer than
 * [minGapMs] collapse into a single count so one swipe is not recorded twice.
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
        val largeScroll = scrollConfirms &&
            viewportHeight >= 400 &&
            kotlin.math.abs(scrollDeltaY) >= (viewportHeight * 0.45f).toInt()

        if (!inFeed) {
            inFeed = true
            if (!signature.isNullOrBlank()) lastSignature = signature
            awaitingSignature = signature.isNullOrBlank()
            return count(nowMs)
        }

        if (signatureChanged && awaitingSignature && !largeScroll) {
            lastSignature = signature
            awaitingSignature = false
            return 0
        }

        if (!signature.isNullOrBlank()) lastSignature = signature
        if (signatureChanged || largeScroll) {
            awaitingSignature = false
            return count(nowMs)
        }
        return 0
    }

    fun reset() {
        inFeed = false
        lastSignature = null
        awaitingSignature = false
        session = 0
    }

    private fun count(nowMs: Long): Int {
        val previous = lastCountAt
        if (previous != null && nowMs - previous < minGapMs) return 0
        lastCountAt = nowMs
        session += 1
        return 1
    }
}
