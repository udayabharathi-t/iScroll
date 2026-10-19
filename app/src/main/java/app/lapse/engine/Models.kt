package app.lapse.engine

enum class Surface {
    NONE,
    INSTAGRAM,
    YOUTUBE,
    REDDIT,
}

enum class Feed {
    NONE,
    INSTAGRAM_REELS,
    YOUTUBE_SHORTS,
    REDDIT_FEED,
}

data class NodeSignal(
    val viewId: String? = null,
    val className: String? = null,
    val description: String? = null,
    val text: String? = null,
    val visible: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val top: Int = Int.MAX_VALUE,
) {
    val viewIdSuffix: String?
        get() = viewId?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

    /** On screen and large enough that a hidden, zero-size leftover does not count. */
    val onScreen: Boolean
        get() = visible && width > 0 && height > 0
}

data class ScrollSignal(
    val viewId: String? = null,
    val className: String? = null,
    val fromIndex: Int = -1,
    val toIndex: Int = -1,
    val itemCount: Int = -1,
    val scrollDeltaY: Int = 0,
    val viewportHeight: Int = 0,
    val visibleItemKeys: List<String> = emptyList(),
) {
    val viewIdSuffix: String?
        get() = viewId?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
}

data class Observation(
    val packageName: String,
    val atMs: Long,
    val signals: List<NodeSignal> = emptyList(),
    val scroll: ScrollSignal? = null,
    val screenOn: Boolean = true,
)

data class TimeDelta(
    val instagramMs: Long = 0,
    val youtubeMs: Long = 0,
    val redditMs: Long = 0,
) {
    val isZero: Boolean
        get() = instagramMs == 0L && youtubeMs == 0L && redditMs == 0L
}

data class EngineEffect(
    val time: TimeDelta = TimeDelta(),
    val reels: Int = 0,
    val shorts: Int = 0,
    val posts: Int = 0,
    val surface: Surface = Surface.NONE,
    val feed: Feed = Feed.NONE,
) {
    val hasAnything: Boolean
        get() = !time.isZero || reels > 0 || shorts > 0 || posts > 0
}
