package app.lapse.tracking

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.lapse.LapseApp

class PauseTileService : TileService() {
    override fun onStartListening() {
        render()
    }

    override fun onClick() {
        val prefs = (application as LapseApp).preferences
        prefs.paused = !prefs.paused
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val paused = (application as LapseApp).preferences.paused
        tile.label = "Lapse"
        tile.state = if (paused) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (paused) "Paused" else "Tracking"
        }
        tile.updateTile()
    }

    companion object {
        fun requestRefresh(context: Context) {
            requestListeningState(context, ComponentName(context, PauseTileService::class.java))
        }
    }
}
