package app.lapse.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class TrackingPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    val pausedFlow = MutableStateFlow(prefs.getBoolean(KEY_PAUSED, false))
    val overlayFlow = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY, true))

    var paused: Boolean
        get() = prefs.getBoolean(KEY_PAUSED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PAUSED, value).apply()
            pausedFlow.value = value
        }

    var overlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_OVERLAY, value).apply()
            overlayFlow.value = value
        }

    fun listen(listener: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val wrapper = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> listener() }
        prefs.registerOnSharedPreferenceChangeListener(wrapper)
        return wrapper
    }

    fun stopListening(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val NAME = "lapse"
        const val KEY_PAUSED = "paused"
        const val KEY_OVERLAY = "overlay"
    }
}
