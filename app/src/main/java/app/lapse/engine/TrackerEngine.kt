package app.lapse.engine

/**
 * Turns screen observations into time and scroll counts.
 * All state lives here so the accessibility service stays a thin adapter.
 */
class TrackerEngine(
    private val clock: SessionClock = SessionClock(),
    private val reels: ItemAdvanceDetector = ItemAdvanceDetector(),
    private val shorts: ItemAdvanceDetector = ItemAdvanceDetector(),
    private val posts: PostPassCounter = PostPassCounter(),
) {
    fun observe(observation: Observation, paused: Boolean): EngineEffect {
        if (paused || !observation.screenOn) return stop(observation.atMs)
        val surface = Packages.surfaceOf(observation.packageName)
        val time = clock.tick(surface, observation.atMs)
        if (surface == Surface.NONE) {
            leaveFeeds(observation.atMs)
            return effect(time = time)
        }
        val feed = FeedSignals.classify(observation.packageName, observation.signals, observation.scroll)
        return count(surface, feed, observation, time)
    }

    fun heartbeat(atMs: Long, packageName: String?, paused: Boolean, screenOn: Boolean): EngineEffect {
        if (paused || !screenOn || packageName == null) return stop(atMs)
        val surface = Packages.surfaceOf(packageName)
        val time = clock.tick(surface, atMs)
        if (surface == Surface.NONE) leaveFeeds(atMs)
        return effect(time = time, surface = surface)
    }

    fun onPauseOrScreenOff(atMs: Long): EngineEffect = stop(atMs)

    private fun stop(atMs: Long): EngineEffect {
        val time = clock.stop(atMs)
        reels.reset()
        shorts.reset()
        posts.reset()
        return effect(time = time)
    }

    private fun leaveFeeds(atMs: Long) {
        reels.onSample(false, null, 0, 0, false, atMs)
        shorts.onSample(false, null, 0, 0, false, atMs)
        posts.onFeed(false, -1, emptyList(), atMs)
    }

    private fun count(
        surface: Surface,
        feed: Feed,
        observation: Observation,
        time: TimeDelta,
    ): EngineEffect {
        val scroll = observation.scroll
        val reelSignature = if (feed == Feed.INSTAGRAM_REELS) {
            FeedSignals.instagramSignature(observation.signals)
        } else {
            null
        }
        val shortSignature = if (feed == Feed.YOUTUBE_SHORTS) {
            FeedSignals.youtubeSignature(observation.signals)
        } else {
            null
        }
        val useKeys = scroll == null || scroll.fromIndex < 0
        val reelDelta = reels.onSample(
            active = feed == Feed.INSTAGRAM_REELS,
            signature = reelSignature,
            scrollDeltaY = scroll?.scrollDeltaY ?: 0,
            viewportHeight = scroll?.viewportHeight ?: 0,
            scrollConfirms = FeedSignals.scrollConfirmsPager(Feed.INSTAGRAM_REELS, scroll),
            nowMs = observation.atMs,
        )
        val shortDelta = shorts.onSample(
            active = feed == Feed.YOUTUBE_SHORTS,
            signature = shortSignature,
            scrollDeltaY = scroll?.scrollDeltaY ?: 0,
            viewportHeight = scroll?.viewportHeight ?: 0,
            scrollConfirms = FeedSignals.scrollConfirmsPager(Feed.YOUTUBE_SHORTS, scroll),
            nowMs = observation.atMs,
        )
        val postDelta = posts.onFeed(
            active = feed == Feed.REDDIT_FEED,
            fromIndex = scroll?.fromIndex ?: -1,
            visibleKeys = if (useKeys) scroll?.visibleItemKeys.orEmpty() else emptyList(),
            nowMs = observation.atMs,
        )
        return effect(
            time = time,
            reels = reelDelta,
            shorts = shortDelta,
            posts = postDelta,
            surface = surface,
            feed = feed,
        )
    }

    private fun effect(
        time: TimeDelta = TimeDelta(),
        reels: Int = 0,
        shorts: Int = 0,
        posts: Int = 0,
        surface: Surface = Surface.NONE,
        feed: Feed = Feed.NONE,
    ): EngineEffect = EngineEffect(
        time = time,
        reels = reels,
        shorts = shorts,
        posts = posts,
        surface = surface,
        feed = feed,
        sessionReels = this.reels.session,
        sessionShorts = this.shorts.session,
        sessionPosts = this.posts.session,
    )
}
