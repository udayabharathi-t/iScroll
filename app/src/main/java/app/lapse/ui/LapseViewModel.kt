package app.lapse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lapse.LapseApp
import app.lapse.data.DayReport
import app.lapse.tracking.DeviceStatus
import app.lapse.tracking.InstalledApps
import app.lapse.tracking.PauseTileService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LapseViewModel(app: Application) : AndroidViewModel(app) {
    private val lapse = app as LapseApp
    private val store = lapse.store
    private val prefs = lapse.preferences

    val today: StateFlow<DayReport> = store.revision
        .map { store.today() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), store.today())

    val history: StateFlow<List<DayReport>> = store.revision
        .map { store.recent(90) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), store.recent(90))

    val paused: StateFlow<Boolean> = prefs.pausedFlow
    val overlayEnabled: StateFlow<Boolean> = prefs.overlayFlow

    private val accessibilityState = MutableStateFlow(false)
    val accessibilityOn: StateFlow<Boolean> = accessibilityState.asStateFlow()

    private val notificationsState = MutableStateFlow(false)
    val notificationsOn: StateFlow<Boolean> = notificationsState.asStateFlow()

    private val batteryState = MutableStateFlow(false)
    val batteryUnrestricted: StateFlow<Boolean> = batteryState.asStateFlow()

    private val installedState = MutableStateFlow(InstalledApps())
    val installed: StateFlow<InstalledApps> = installedState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val context = getApplication<Application>()
        accessibilityState.value = DeviceStatus.accessibilityEnabled(context)
        notificationsState.value = DeviceStatus.notificationsAllowed(context)
        batteryState.value = DeviceStatus.batteryUnrestricted(context)
        installedState.value = DeviceStatus.installed(context)
    }

    fun setPaused(paused: Boolean) {
        prefs.paused = paused
        PauseTileService.requestRefresh(getApplication())
    }

    fun setOverlay(enabled: Boolean) {
        prefs.overlayEnabled = enabled
    }
}
