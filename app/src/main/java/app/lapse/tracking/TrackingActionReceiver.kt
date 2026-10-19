package app.lapse.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.lapse.LapseApp

class TrackingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = (context.applicationContext as LapseApp).preferences
        when (intent?.action) {
            TrackingNotifier.ACTION_PAUSE -> prefs.paused = true
            TrackingNotifier.ACTION_RESUME -> prefs.paused = false
        }
    }
}
