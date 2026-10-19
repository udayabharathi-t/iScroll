package app.lapse.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedSignalsTest {
    @Test
    fun zeroSizeInstagramPagerIsNotReels() {
        val signals = listOf(
            NodeSignal(
                viewId = "com.instagram.android:id/clips_viewer_view_pager",
                visible = true,
                width = 0,
                height = 0,
            ),
        )
        assertEquals(Feed.NONE, FeedSignals.classify(Packages.INSTAGRAM, signals, null))
    }

    @Test
    fun visibleInstagramPagerIsReels() {
        val signals = listOf(reelPager())
        assertEquals(Feed.INSTAGRAM_REELS, FeedSignals.classify(Packages.INSTAGRAM, signals, null))
    }

    @Test
    fun reelByLabelIsReelsAndSignatureKeepsTheAuthor() {
        val signals = listOf(
            NodeSignal(
                description = "Reel by Ada. A long caption that should not be stored.",
                visible = true,
                width = 1080,
                height = 1920,
            ),
        )
        assertEquals(Feed.INSTAGRAM_REELS, FeedSignals.classify(Packages.INSTAGRAM, signals, null))
        assertEquals("ig:ada", FeedSignals.instagramSignature(signals))
    }

    @Test
    fun reelsTabLabelAloneIsNotTheViewer() {
        val signals = listOf(
            NodeSignal(text = "Reels", visible = true, width = 200, height = 80),
        )
        assertEquals(Feed.NONE, FeedSignals.classify(Packages.INSTAGRAM, signals, null))
        assertNull(FeedSignals.instagramSignature(signals))
    }

    @Test
    fun youtubeShortsPlayerIdIsShorts() {
        val signals = listOf(
            NodeSignal(
                viewId = "com.google.android.youtube:id/reel_player_page_container",
                visible = true,
                width = 1080,
                height = 2000,
            ),
        )
        assertEquals(Feed.YOUTUBE_SHORTS, FeedSignals.classify(Packages.YOUTUBE, signals, null))
    }

    @Test
    fun youtubeWatchPageIsNotShorts() {
        val signals = listOf(
            NodeSignal(
                viewId = "com.google.android.youtube:id/watch_player",
                visible = true,
                width = 1080,
                height = 600,
            ),
        )
        assertEquals(Feed.NONE, FeedSignals.classify(Packages.YOUTUBE, signals, null))
    }

    @Test
    fun redditFeedIndexIsTheFeed() {
        val scroll = ScrollSignal(fromIndex = 4, toIndex = 7, itemCount = 40, className = "androidx.recyclerview.widget.RecyclerView")
        assertEquals(Feed.REDDIT_FEED, FeedSignals.classify(Packages.REDDIT, emptyList(), scroll))
    }

    @Test
    fun redditCommentListIsNotTheFeed() {
        val signals = listOf(
            NodeSignal(
                viewId = "com.reddit.frontpage:id/comments_list",
                visible = true,
                width = 1080,
                height = 1600,
            ),
        )
        val scroll = ScrollSignal(fromIndex = 1, toIndex = 6, itemCount = 20, viewId = "com.reddit.frontpage:id/comments_list")
        assertEquals(Feed.NONE, FeedSignals.classify(Packages.REDDIT, signals, scroll))
    }

    @Test
    fun otherAppsAreIgnored() {
        assertEquals(Surface.NONE, Packages.surfaceOf("com.android.chrome"))
        assertEquals(Feed.NONE, FeedSignals.classify("com.android.chrome", listOf(reelPager()), null))
    }

    private fun reelPager() = NodeSignal(
        viewId = "com.instagram.android:id/clips_viewer_view_pager",
        className = "androidx.viewpager2.widget.ViewPager2",
        visible = true,
        width = 1080,
        height = 2000,
    )
}
