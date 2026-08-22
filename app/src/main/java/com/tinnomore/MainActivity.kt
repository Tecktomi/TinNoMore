package com.tinnomore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinnomore.ui.navigation.AppNavigation
import com.tinnomore.ui.theme.TinNoMoreTheme
import com.tinnomore.viewmodel.GlobalNotchViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Necesario en API 33+ para poder mostrar la notificación persistente
        // del foreground service del notch global.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            TinNoMoreTheme {
                val globalNotchVm: GlobalNotchViewModel = viewModel()
                // Si el usuario dejó el notch global activado en una sesión
                // anterior, relanza el servicio (Android puede haber matado
                // el proceso en background).
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    globalNotchVm.restoreIfNeeded()
                }
                AppNavigation()
            }
        }
    }
}
