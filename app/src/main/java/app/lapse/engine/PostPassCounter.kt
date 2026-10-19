package app.lapse.engine

/**
 * Counts Reddit posts that leave the top of the feed.
 * The posts already visible when the feed opens have not been scrolled past.
 */
class PostPassCounter(
    private val holdMs: Long = 1_200,
) {
    private var lastFrom = -1
    private var primed = false
    private var lastKeys: List<String> = emptyList()
    private var lastActiveAt = 0L

    fun onFeed(
        active: Boolean,
        fromIndex: Int,
        visibleKeys: List<String>,
        nowMs: Long,
    ): Int {
        if (!active) {
            if (primed && nowMs - lastActiveAt < holdMs) return 0
            reset()
            return 0
        }
        lastActiveAt = nowMs
        if (!primed) {
            primed = true
            if (fromIndex >= 0) lastFrom = fromIndex
            if (visibleKeys.isNotEmpty()) lastKeys = visibleKeys
            return 0
        }
        if (fromIndex >= 0 && lastFrom >= 0) {
            val delta = (fromIndex - lastFrom).coerceIn(0, 12)
            lastFrom = fromIndex
            if (visibleKeys.isNotEmpty()) lastKeys = visibleKeys
            return delta
        }
        if (fromIndex >= 0) {
            lastFrom = fromIndex
            if (visibleKeys.isNotEmpty()) lastKeys = visibleKeys
            return 0
        }
        if (visibleKeys.isNotEmpty() && lastKeys.isNotEmpty()) {
            val passed = passedKeys(lastKeys, visibleKeys)
            lastKeys = visibleKeys
            return passed
        }
        if (visibleKeys.isNotEmpty()) lastKeys = visibleKeys
        return 0
    }

    fun reset() {
        lastFrom = -1
        primed = false
        lastKeys = emptyList()
    }

    private fun passedKeys(previous: List<String>, current: List<String>): Int {
        if (current.isEmpty()) return 0
        val anchor = previous.indexOf(current.first())
        val passed = if (anchor >= 0) anchor else previous.count { it !in current }
        return passed.coerceIn(0, 12)
    }
}
