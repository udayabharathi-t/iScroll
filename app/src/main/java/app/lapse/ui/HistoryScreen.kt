package app.lapse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lapse.data.DayReport
import app.lapse.engine.formatDuration
import app.lapse.engine.formatHeadingDate
import app.lapse.engine.formatHistoryDate
import app.lapse.engine.plural
import app.lapse.ui.theme.InstagramMark
import app.lapse.ui.theme.RedditMark
import app.lapse.ui.theme.YoutubeMark
import java.time.LocalDate

@Composable
fun HistoryScreen(
    history: List<DayReport>,
    today: LocalDate,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        Column(
            modifier = modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Text(text = "History", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "No days on file yet. Lapse writes a line for each day it watches Instagram, YouTube, or Reddit.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val weekStart = today.minusDays(6).toString()
    val weekMs = history.filter { it.day >= weekStart }.sumOf { it.totalMs }
    LazyColumn(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.widthIn(max = 720.dp).padding(bottom = 8.dp)) {
                Text(text = "History", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Last 7 days · ${formatDuration(weekMs)}",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(history, key = { it.day }) { report ->
            val parsed = runCatching { LocalDate.parse(report.day) }.getOrNull()
            val label = if (parsed == null) report.day else formatHistoryDate(parsed, today)
            Card(
                onClick = { onOpen(report.day) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatDuration(report.totalMs),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = historyDetail(report),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun DayDetailScreen(
    report: DayReport,
    today: LocalDate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = runCatching { LocalDate.parse(report.day) }.getOrNull()
    Column(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(
            text = if (parsed == null) report.day else formatHeadingDate(parsed),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (parsed == today) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        SerifTime(ms = report.totalMs, modifier = Modifier.padding(top = 20.dp))
        Text(
            text = "lost that day",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.padding(top = 24.dp)) {
            MarkRow(InstagramMark, "Instagram", "${formatDuration(report.instagramMs)} · ${plural(report.reels, "reel", "reels")}")
            MarkRow(YoutubeMark, "YouTube", "${formatDuration(report.youtubeMs)} · ${plural(report.shorts, "short", "shorts")}")
            MarkRow(RedditMark, "Reddit", "${formatDuration(report.redditMs)} · ${plural(report.posts, "post", "posts")} scrolled past")
        }
        NoticeCard(
            title = "What this line includes",
            body = "Time is counted while the app was on screen and Lapse was running. Reels, Shorts, and posts are counted from scrolls. A pause, a dark screen, or a force stop leaves a gap.",
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

private fun historyDetail(report: DayReport): String {
    return listOf(
        plural(report.reels, "reel", "reels"),
        plural(report.shorts, "short", "shorts"),
        "${plural(report.posts, "post", "posts")} passed",
    ).joinToString(" · ")
}
