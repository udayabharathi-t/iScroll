package app.lapse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lapse.data.DayReport
import app.lapse.engine.formatHeadingDate
import app.lapse.engine.plural
import app.lapse.ui.theme.InstagramMark
import app.lapse.ui.theme.RedditMark
import app.lapse.ui.theme.YoutubeMark
import java.time.LocalDate

@Composable
fun TodayScreen(
    today: DayReport,
    history: List<DayReport>,
    paused: Boolean,
    accessibilityOn: Boolean,
    instagramInstalled: Boolean,
    youtubeInstalled: Boolean,
    redditInstalled: Boolean,
    onPause: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = runCatching { LocalDate.parse(today.day) }.getOrElse { LocalDate.now() }
    val apps = listOf(
        TrackedApp(
            name = "Instagram",
            timeMs = today.instagramMs,
            detail = scrollDetail(today.reels, today.instagramMs, "reel", "reels", "No reels yet"),
            mark = InstagramMark,
            installed = instagramInstalled,
        ),
        TrackedApp(
            name = "YouTube",
            timeMs = today.youtubeMs,
            detail = scrollDetail(today.shorts, today.youtubeMs, "short", "shorts", "No shorts yet"),
            mark = YoutubeMark,
            installed = youtubeInstalled,
        ),
        TrackedApp(
            name = "Reddit",
            timeMs = today.redditMs,
            detail = scrollDetail(today.posts, today.redditMs, "post", "posts", "No posts yet"),
            mark = RedditMark,
            installed = redditInstalled,
        ),
    )
    Column(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "iScroll",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = formatHeadingDate(date),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (accessibilityOn) {
                if (paused) {
                    Button(onClick = { onPause(false) }) { Text("Resume") }
                } else {
                    OutlinedButton(onClick = { onPause(true) }) { Text("Pause") }
                }
            }
        }
        SerifTime(
            ms = today.totalMs,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            text = if (today.totalMs == 0L) {
                "Nothing counted yet today."
            } else {
                "on Instagram, YouTube, and Reddit"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WeekStrip(
            reports = history,
            todayReport = today,
            today = date,
        )
        Text(
            text = "Today",
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 720.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    apps.forEach { app ->
                        TrackedAppCard(app, Modifier.weight(1f))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    apps.forEach { app -> TrackedAppCard(app) }
                }
            }
        }
        if (!accessibilityOn) {
            NoticeCard(
                title = "Tracking is off",
                body = "Turn on the iScroll accessibility service. Android uses that permission to let iScroll count scrolls inside Instagram, YouTube, and Reddit.",
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(
                onClick = onOpenAccessibility,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("Open accessibility settings")
            }
        } else if (paused) {
            NoticeCard(
                title = "Paused",
                body = "Time and scrolls are on hold. Resume here, from the notification, or from the iScroll Quick Settings tile.",
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private fun scrollDetail(count: Int, timeMs: Long, singular: String, pluralWord: String, empty: String): String {
    return when {
        count > 0 && singular == "post" -> "${plural(count, singular, pluralWord)} scrolled past"
        count > 0 -> "${plural(count, singular, pluralWord)} scrolled"
        timeMs > 0 && singular == "reel" -> "Open, with no reels counted"
        timeMs > 0 && singular == "short" -> "Open. Time includes regular videos."
        timeMs > 0 -> "Open, with no posts counted"
        else -> empty
    }
}
