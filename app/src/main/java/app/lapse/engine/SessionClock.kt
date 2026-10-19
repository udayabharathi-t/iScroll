package app.lapse.engine

/**
 * Foreground time for one tracked app. Gaps longer than [maxGapMs] are dropped
 * so a frozen process does not invent a long session when it wakes up.
 */
class SessionClock(
    private val maxGapMs: Long = 8_000,
) {
    private var surface = Surface.NONE
    private var lastTick = 0L

    fun tick(next: Surface, atMs: Long): TimeDelta {
        if (next == Surface.NONE) return stop(atMs)
        if (surface == Surface.NONE || lastTick == 0L) {
            surface = next
            lastTick = atMs
            return TimeDelta()
        }
        val credited = credit(atMs)
        surface = next
        lastTick = atMs
        return credited
    }

    fun stop(atMs: Long): TimeDelta {
        val credited = if (surface == Surface.NONE) TimeDelta() else credit(atMs)
        surface = Surface.NONE
        lastTick = 0L
        return credited
    }

    private fun credit(atMs: Long): TimeDelta {
        val gap = atMs - lastTick
        if (gap <= 0L || gap > maxGapMs || surface == Surface.NONE) return TimeDelta()
        return when (surface) {
            Surface.INSTAGRAM -> TimeDelta(instagramMs = gap)
            Surface.YOUTUBE -> TimeDelta(youtubeMs = gap)
            Surface.REDDIT -> TimeDelta(redditMs = gap)
            Surface.NONE -> TimeDelta()
        }
    }
}
