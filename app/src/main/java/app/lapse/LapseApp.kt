package app.lapse

import android.app.Application
import app.lapse.data.DayReportStore
import app.lapse.data.TrackingPreferences

class LapseApp : Application() {
    lateinit var store: DayReportStore
        private set
    lateinit var preferences: TrackingPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        store = DayReportStore(this)
        preferences = TrackingPreferences(this)
    }
}
