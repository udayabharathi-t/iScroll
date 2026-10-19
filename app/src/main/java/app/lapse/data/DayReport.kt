package app.lapse.data

data class DayReport(
    val day: String,
    val instagramMs: Long = 0,
    val youtubeMs: Long = 0,
    val redditMs: Long = 0,
    val reels: Int = 0,
    val shorts: Int = 0,
    val posts: Int = 0,
) {
    val totalMs: Long
        get() = instagramMs + youtubeMs + redditMs

    companion object {
        fun empty(day: String) = DayReport(day)
    }
}
