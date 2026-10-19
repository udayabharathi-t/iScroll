package app.lapse.engine

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0m"
    if (ms < 60_000L) {
        val seconds = (ms / 1_000L).coerceAtLeast(1L)
        return "${seconds}s"
    }
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

fun plural(count: Int, singular: String, plural: String): String {
    return "$count ${if (count == 1) singular else plural}"
}

/** Label for the on-screen chip. [count] is this visit, not the daily total. */
fun sessionChip(feed: Feed, count: Int): String? = when (feed) {
    Feed.INSTAGRAM_REELS -> plural(count, "reel", "reels")
    Feed.YOUTUBE_SHORTS -> plural(count, "short", "shorts")
    Feed.REDDIT_FEED -> plural(count, "post", "posts")
    Feed.NONE -> null
}

fun formatHeadingDate(day: LocalDate, locale: Locale = Locale.US): String {
    return day.format(DateTimeFormatter.ofPattern("EEEE, MMM d", locale))
}

fun formatHistoryDate(day: LocalDate, today: LocalDate, locale: Locale = Locale.US): String {
    val pattern = if (day.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy"
    return day.format(DateTimeFormatter.ofPattern(pattern, locale))
}
