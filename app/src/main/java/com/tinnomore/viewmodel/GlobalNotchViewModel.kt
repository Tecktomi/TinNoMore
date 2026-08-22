package com.tinnomore.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.NotchSettingsStore
import com.tinnomore.service.GlobalNotchService
import com.tinnomore.util.GlobalNotchEffect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Controla el notch "global" (afecta a todo el audio del dispositivo,
 * no solo al que reproduce esta app). Ver [GlobalNotchEffect] para el
 * detalle de cómo y por qué funciona, y sus limitaciones.
 */
class GlobalNotchViewModel(application: Application) : AndroidViewModel(application) {

    private val store = NotchSettingsStore(application)

    val isSupported: Boolean = GlobalNotchEffect.isSupported
    val minSdkMessage: String? =
        if (!isSupported) "El notch global requiere Android 9 (API 28) o superior. Este dispositivo usa API ${Build.VERSION.SDK_INT}." else null

    val enabled: StateFlow<Boolean> = store.enabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val freqHz: StateFlow<Int> = store.freqHz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4000)

    val depthDb: StateFlow<Float> = store.depthDb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -24f)

    /** Activa/desactiva el notch global y arranca/detiene el foreground service. */
    fun setEnabled(value: Boolean) {
        if (!isSupported) return
        viewModelScope.launch {
            store.setEnabled(value)
            val app = getApplication<Application>()
            if (value) {
                GlobalNotchService.start(app, freqHz.value, depthDb.value)
            } else {
                GlobalNotchService.stop(app)
            }
        }
    }

    /** Cambia la frecuencia; si el notch global está activo, se reconfigura en caliente. */
    fun setFrequency(hz: Int) {
        viewModelScope.launch {
            store.setFrequency(hz)
            // El servicio observa el DataStore y se reconfigura solo; no hace
            // falta relanzarlo aquí.
        }
    }

    fun setDepth(db: Float) {
        viewModelScope.launch { store.setDepth(db) }
    }

    /** Llamar al arrancar la app: si quedó activado de una sesión previa, relanza el servicio. */
    fun restoreIfNeeded() {
        if (!isSupported) return
        viewModelScope.launch {
            if (store.enabled.first()) {
                GlobalNotchService.start(getApplication(), store.freqHz.first(), store.depthDb.first())
            }
        }
    }
}
