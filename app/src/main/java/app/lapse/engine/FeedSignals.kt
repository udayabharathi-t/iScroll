package app.lapse.engine

/**
 * Decides which surface is on screen from view ids, class names, and short labels.
 * Labels are used only as an in-memory signature. Callers must not store them.
 */
object FeedSignals {
    val instagramReelIds = setOf(
        "clips_viewer_view_pager",
        "clips_viewer_container",
        "clips_viewer",
        "reel_viewer_container",
        "clips_video_container",
        "clips_media_component",
        "clips_item_container",
    )

    val youtubeShortIds = setOf(
        "reel_player_page_container",
        "reel_player_underlay",
        "reel_recycler",
        "reel_watch_fragment_root",
        "shorts_video_pager",
        "shorts_player",
        "reel_player_page",
        "reel_player_overlay_container",
    )

    val redditCommentHints = listOf(
        "comment_composer",
        "comments_list",
        "comment_layout",
        "reply_list",
        "comment_reply",
    )

    private val reelBy = Regex("""(?i)\breels?\s+by\s+@?([^\n,.]{1,60})""")
    private val reelWord = Regex("""(?i)\breels?\b""")
    private val shortBy = Regex("""(?i)\bshorts?\s+by\s+@?([^\n,.]{1,60})""")
    private val commentHeader = Regex("""(?i)^comments?$""")

    fun classify(packageName: String, signals: List<NodeSignal>, scroll: ScrollSignal?): Feed {
        return when (Packages.surfaceOf(packageName)) {
            Surface.INSTAGRAM -> if (isInstagramReels(signals, scroll)) Feed.INSTAGRAM_REELS else Feed.NONE
            Surface.YOUTUBE -> if (isYoutubeShorts(signals)) Feed.YOUTUBE_SHORTS else Feed.NONE
            Surface.REDDIT -> if (isRedditFeed(signals, scroll)) Feed.REDDIT_FEED else Feed.NONE
            Surface.NONE -> Feed.NONE
        }
    }

    fun instagramSignature(signals: List<NodeSignal>): String? {
        val match = viewerText(signals).firstNotNullOfOrNull { reelBy.find(it) } ?: return null
        return "ig:" + match.groupValues[1].trim().lowercase()
    }

    fun youtubeSignature(signals: List<NodeSignal>): String? {
        val match = onScreenText(signals).firstNotNullOfOrNull { shortBy.find(it) } ?: return null
        return "yt:" + match.groupValues[1].trim().lowercase()
    }

    /**
     * A scroll only advances a reel or Short when it comes from the viewer itself,
     * not from a home-feed list that happens to be open nearby.
     */
    fun scrollConfirmsPager(feed: Feed, scroll: ScrollSignal?): Boolean {
        if (scroll == null) return false
        if (feed != Feed.INSTAGRAM_REELS && feed != Feed.YOUTUBE_SHORTS) return false
        val suffix = scroll.viewIdSuffix
        val known = when (feed) {
            Feed.INSTAGRAM_REELS -> suffix != null && (suffix in instagramReelIds || reelViewerId(suffix))
            Feed.YOUTUBE_SHORTS -> suffix != null && suffix in youtubeShortIds
            Feed.REDDIT_FEED, Feed.NONE -> false
        }
        if (known) return !sideways(scroll)
        if (sideways(scroll)) return false
        val className = scroll.className.orEmpty()
        if (className.contains("ViewPager") && scroll.viewportHeight >= 400 && scroll.scrollDeltaY != 0) {
            return true
        }
        if (feed != Feed.INSTAGRAM_REELS) return false
        val span = if (scroll.fromIndex >= 0 && scroll.toIndex >= scroll.fromIndex) {
            scroll.toIndex - scroll.fromIndex
        } else {
            Int.MAX_VALUE
        }
        val threshold = (scroll.viewportHeight * 0.55f).toInt()
        return className.contains("RecyclerView") &&
            scroll.viewportHeight >= 700 &&
            span <= 1 &&
            kotlin.math.abs(scroll.scrollDeltaY) >= threshold
    }

    private fun isInstagramReels(signals: List<NodeSignal>, scroll: ScrollSignal?): Boolean {
        if (signals.any { viewerNode(it) && (it.viewIdSuffix in instagramReelIds || reelViewerId(it.viewIdSuffix)) }) {
            return true
        }
        if (signals.any { viewerNode(it) && it.className.orEmpty().contains("ClipsViewer", ignoreCase = true) }) {
            return true
        }
        if (viewerText(signals).any { reelBy.containsMatchIn(it) }) return true
        if (signals.any { signal ->
                viewerNode(signal) &&
                    signal.height >= 700 &&
                    reelWord.containsMatchIn((signal.description ?: signal.text).orEmpty())
            }
        ) {
            return true
        }
        return scrollConfirmsPager(Feed.INSTAGRAM_REELS, scroll)
    }

    /** Full-screen viewer. Instagram often reports that pager as not visible. */
    private fun viewerNode(signal: NodeSignal): Boolean {
        if (signal.width < 200 || signal.height < 400) return false
        if (signal.visible) return true
        return signal.top in 0..800
    }

    private fun reelViewerId(suffix: String?): Boolean {
        if (suffix.isNullOrBlank()) return false
        val id = suffix.lowercase()
        val blocked = listOf("direct", "inbox", "comment", "message", "thread", "story", "tab", "title", "search")
        if (blocked.any { id.contains(it) }) return false
        return id.contains("clips") || id.contains("reel")
    }

    private fun sideways(scroll: ScrollSignal): Boolean {
        return kotlin.math.abs(scroll.scrollDeltaX) > kotlin.math.abs(scroll.scrollDeltaY) &&
            kotlin.math.abs(scroll.scrollDeltaX) > 48
    }

    private fun isYoutubeShorts(signals: List<NodeSignal>): Boolean {
        if (signals.any { it.onScreen && it.viewIdSuffix in youtubeShortIds }) return true
        return signals.any {
            it.onScreen && it.className.orEmpty().let { name ->
                name.contains("ReelWatch", ignoreCase = true) ||
                    name.contains("ShortsPlayer", ignoreCase = true)
            }
        }
    }

    private fun isRedditFeed(signals: List<NodeSignal>, scroll: ScrollSignal?): Boolean {
        if (isRedditComments(signals, scroll)) return false
        if (scroll != null && scroll.fromIndex >= 0 && scroll.itemCount > 1) return true
        val suffix = scroll?.viewIdSuffix.orEmpty()
        return suffix.contains("post") || suffix.contains("link_list") || suffix.contains("listing")
    }

    private fun isRedditComments(signals: List<NodeSignal>, scroll: ScrollSignal?): Boolean {
        val ids = buildList {
            signals.mapNotNull { it.viewIdSuffix }.forEach(::add)
            scroll?.viewIdSuffix?.let(::add)
        }
        if (ids.any { id -> redditCommentHints.any { hint -> id.contains(hint) } }) return true
        return signals.any { signal ->
            signal.onScreen &&
                signal.top < 240 &&
                commentHeader.matches((signal.text ?: signal.description).orEmpty().trim())
        }
    }

    private fun viewerText(signals: List<NodeSignal>): Sequence<String> {
        return signals.asSequence()
            .filter { viewerNode(it) }
            .flatMap { sequenceOf(it.description, it.text) }
            .filterNotNull()
            .filter { it.isNotBlank() }
    }

    private fun onScreenText(signals: List<NodeSignal>): Sequence<String> {
        return signals.asSequence()
            .filter { it.onScreen }
            .flatMap { sequenceOf(it.description, it.text) }
            .filterNotNull()
            .filter { it.isNotBlank() }
    }
}
