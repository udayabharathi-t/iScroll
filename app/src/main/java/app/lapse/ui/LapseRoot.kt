package app.lapse.ui

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lapse.data.DayReport
import app.lapse.tracking.InstalledApps
import app.lapse.tracking.SystemScreens
import app.lapse.ui.theme.LapseTheme
import java.time.LocalDate

private enum class Tab(val label: String) {
    Today("Today"),
    History("History"),
    Setup("Setup"),
}

@Composable
fun LapseRoot(viewModel: LapseViewModel) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val overlay by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val accessibilityOn by viewModel.accessibilityOn.collectAsStateWithLifecycle()
    val notificationsOn by viewModel.notificationsOn.collectAsStateWithLifecycle()
    val batteryOn by viewModel.batteryUnrestricted.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    LapseScaffold(
        today = today,
        history = history,
        paused = paused,
        accessibilityOn = accessibilityOn,
        notificationsOn = notificationsOn,
        batteryUnrestricted = batteryOn,
        overlayEnabled = overlay,
        installed = installed,
        onPause = viewModel::setPaused,
        onOverlay = viewModel::setOverlay,
        onOpenAccessibility = { context.startActivity(SystemScreens.accessibility()) },
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= 33 && !notificationsOn) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(SystemScreens.notifications(context))
            }
        },
        onOpenBattery = { context.startActivity(SystemScreens.battery(context)) },
        onOpenAppDetails = { context.startActivity(SystemScreens.appDetails(context)) },
    )
}

@Composable
private fun LapseScaffold(
    today: DayReport,
    history: List<DayReport>,
    paused: Boolean,
    accessibilityOn: Boolean,
    notificationsOn: Boolean,
    batteryUnrestricted: Boolean,
    overlayEnabled: Boolean,
    installed: InstalledApps,
    onPause: (Boolean) -> Unit,
    onOverlay: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenAppDetails: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var openDay by rememberSaveable { mutableStateOf<String?>(null) }
    val todayDate = LocalDate.now()
    BackHandler(enabled = openDay != null) { openDay = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (openDay == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Text(
                                    text = if (tab == item) "●" else "○",
                                    color = if (tab == item) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val selected = history.firstOrNull { it.day == openDay }
            when {
                selected != null -> DayDetailScreen(
                    report = selected,
                    today = todayDate,
                    onBack = { openDay = null },
                )
                openDay != null -> DayDetailScreen(
                    report = DayReport.empty(openDay!!),
                    today = todayDate,
                    onBack = { openDay = null },
                )
                tab == Tab.Today -> TodayScreen(
                    today = today,
                    history = history,
                    paused = paused,
                    accessibilityOn = accessibilityOn,
                    instagramInstalled = installed.instagram,
                    youtubeInstalled = installed.youtube,
                    redditInstalled = installed.reddit,
                    onPause = onPause,
                    onOpenAccessibility = onOpenAccessibility,
                )
                tab == Tab.History -> HistoryScreen(
                    history = history,
                    today = todayDate,
                    onOpen = { openDay = it },
                )
                tab == Tab.Setup -> SetupScreen(
                    accessibilityOn = accessibilityOn,
                    notificationsOn = notificationsOn,
                    batteryUnrestricted = batteryUnrestricted,
                    overlayEnabled = overlayEnabled,
                    installed = installed,
                    onOpenAccessibility = onOpenAccessibility,
                    onRequestNotifications = onRequestNotifications,
                    onOpenBattery = onOpenBattery,
                    onOpenAppDetails = onOpenAppDetails,
                    onOverlay = onOverlay,
                )
            }
        }
    }
}

@Preview(name = "Today", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TodayDarkPreview() {
    LapseTheme {
        LapseScaffold(
            today = DayReport(
                day = "2026-10-19",
                instagramMs = 3_720_000,
                youtubeMs = 1_080_000,
                redditMs = 360_000,
                reels = 48,
                shorts = 12,
                posts = 23,
            ),
            history = listOf(
                DayReport("2026-10-19", 3_720_000, 1_080_000, 360_000, 48, 12, 23),
                DayReport("2026-10-18", 5_400_000, 600_000, 900_000, 70, 8, 40),
            ),
            paused = false,
            accessibilityOn = true,
            notificationsOn = true,
            batteryUnrestricted = false,
            overlayEnabled = true,
            installed = InstalledApps(instagram = true, youtube = true, reddit = true),
            onPause = {},
            onOverlay = {},
            onOpenAccessibility = {},
            onRequestNotifications = {},
            onOpenBattery = {},
            onOpenAppDetails = {},
        )
    }
}
