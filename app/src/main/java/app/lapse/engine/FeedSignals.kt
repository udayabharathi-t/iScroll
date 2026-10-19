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

    private val reelBy = Regex("""(?i)\breel by\s+@?([^\n,.]{1,60})""")
    private val shortBy = Regex("""(?i)\bshorts?\s+by\s+@?([^\n,.]{1,60})""")
    private val commentHeader = Regex("""(?i)^comments?$""")

    fun classify(packageName: String, signals: List<NodeSignal>, scroll: ScrollSignal?): Feed {
        return when (Packages.surfaceOf(packageName)) {
            Surface.INSTAGRAM -> if (isInstagramReels(signals)) Feed.INSTAGRAM_REELS else Feed.NONE
            Surface.YOUTUBE -> if (isYoutubeShorts(signals)) Feed.YOUTUBE_SHORTS else Feed.NONE
            Surface.REDDIT -> if (isRedditFeed(signals, scroll)) Feed.REDDIT_FEED else Feed.NONE
            Surface.NONE -> Feed.NONE
        }
    }

    fun instagramSignature(signals: List<NodeSignal>): String? {
        val match = onScreenText(signals).firstNotNullOfOrNull { reelBy.find(it) } ?: return null
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
        val suffix = scroll.viewIdSuffix
        val known = when (feed) {
            Feed.INSTAGRAM_REELS -> suffix != null && suffix in instagramReelIds
            Feed.YOUTUBE_SHORTS -> suffix != null && suffix in youtubeShortIds
            else -> false
        }
        if (known) return true
        val pager = scroll.className?.contains("ViewPager") == true
        return pager && scroll.viewportHeight >= 400
    }

    private fun isInstagramReels(signals: List<NodeSignal>): Boolean {
        if (signals.any { it.onScreen && it.viewIdSuffix in instagramReelIds }) return true
        if (signals.any { it.onScreen && it.className.orEmpty().contains("ClipsViewer", ignoreCase = true) }) {
            return true
        }
        return onScreenText(signals).any { reelBy.containsMatchIn(it) }
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

    private fun onScreenText(signals: List<NodeSignal>): Sequence<String> {
        return signals.asSequence()
            .filter { it.onScreen }
            .flatMap { sequenceOf(it.description, it.text) }
            .filterNotNull()
            .filter { it.isNotBlank() }
    }
}
