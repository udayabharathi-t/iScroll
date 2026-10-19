package app.lapse.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lapse.tracking.InstalledApps

@Composable
fun SetupScreen(
    accessibilityOn: Boolean,
    notificationsOn: Boolean,
    batteryUnrestricted: Boolean,
    overlayEnabled: Boolean,
    installed: InstalledApps,
    onOpenAccessibility: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOverlay: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "iScroll keeps a daily report on this phone: how long Instagram, YouTube, and Reddit stayed open, how many reels and Shorts you scrolled, and how many Reddit posts you moved past.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel("Required")
        SetupBlock(
            title = "Accessibility",
            body = if (accessibilityOn) {
                "On. iScroll can see scrolls in the three apps."
            } else {
                "Off. Reports stay empty until this is on."
            },
            action = "Open accessibility settings",
            onAction = onOpenAccessibility,
        )
        SectionLabel("While you scroll")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = "Live count", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "A small counter at the top of the viewer. It shows how many reels, Shorts, or posts are in this visit, then starts over when you leave. It does not take taps.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = overlayEnabled, onCheckedChange = onOverlay)
        }
        SetupBlock(
            title = "Notifications",
            body = if (notificationsOn) {
                "On. The shade has Pause and Resume."
            } else {
                "Allow notifications so you can pause without opening iScroll."
            },
            action = if (notificationsOn) "Notification settings" else "Allow notifications",
            onAction = onRequestNotifications,
        )
        SetupBlock(
            title = "Battery",
            body = if (batteryUnrestricted) {
                "Unrestricted. Android is less likely to freeze the counter."
            } else {
                "Set iScroll to unrestricted so the count survives while you scroll."
            },
            action = if (batteryUnrestricted) "Battery settings" else "Allow background",
            onAction = onOpenBattery,
        )
        SectionLabel("On this phone")
        Text(
            text = installedSummary(installed),
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionLabel("How a count is made")
        Text(
            text = "Time is the stretch an app stays in front with the screen on. A reel or Short counts when that viewer is open and you move to the next one. A Reddit post counts when it leaves the top of the feed. Messages, comment threads, and the browser stay out of the scroll totals. The official apps are Instagram, Instagram Lite, YouTube, and Reddit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "App updates can change those screens, so a count can miss or, more rarely, add one. The daily time is the steadier number.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel("Privacy")
        Text(
            text = "Reports stay in private storage on this phone. iScroll has no account and no network access. Inside the three apps it reads view ids and short labels long enough to tell a viewer from a chat, then drops the words. It checks which app is open so the clock can stop, and it does not read the screen of anything else.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel("Stopping")
        Text(
            text = "Pause when you want a break and still want iScroll installed. Force stop from Android's app info screen also ends tracking until you open iScroll again. On some phones, force stop turns accessibility off and you enable it once more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpenAppDetails) { Text("Open app info") }
        Text(
            text = "Add the iScroll tile to Quick Settings if you want pause from the shade.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetupBlock(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAction) { Text(action) }
    }
}

private fun installedSummary(installed: InstalledApps): String {
    val present = buildList {
        if (installed.instagram) add("Instagram")
        if (installed.youtube) add("YouTube")
        if (installed.reddit) add("Reddit")
    }
    val missing = buildList {
        if (!installed.instagram) add("Instagram")
        if (!installed.youtube) add("YouTube")
        if (!installed.reddit) add("Reddit")
    }
    return when {
        present.isEmpty() -> "None of the three apps are installed yet. The report starts once they are."
        missing.isEmpty() -> "Instagram, YouTube, and Reddit are installed."
        else -> "${present.joinToString()} installed. ${missing.joinToString()} still missing."
    }
}
