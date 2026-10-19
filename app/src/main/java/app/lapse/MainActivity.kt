package app.lapse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lapse.tracking.DeviceStatus
import app.lapse.tracking.LapseAccessibilityService
import app.lapse.ui.LapseRoot
import app.lapse.ui.theme.LapseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LapseTheme {
                LapseRoot(viewModel = viewModel())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (DeviceStatus.accessibilityEnabled(this) && DeviceStatus.canPostForegroundNotification(this)) {
            ContextCompat.startForegroundService(this, Intent(this, LapseAccessibilityService::class.java))
        }
    }
}
