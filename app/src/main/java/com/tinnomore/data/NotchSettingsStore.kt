package com.tinnomore.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notchDataStore by preferencesDataStore(name = "notch_settings")

/**
 * Persiste la configuración del notch global (el que afecta a TODO el audio
 * del dispositivo, vía [com.tinnomore.util.GlobalNotchEffect]).
 *
 * Se guarda por separado del notch "local" de [NotchViewModel] porque el
 * usuario puede querer, por ejemplo, dejar el notch global activo todo el día
 * escuchando música/podcasts, sin necesidad de tener la app en primer plano
 * reproduciendo ruido rosa/blanco.
 */
class NotchSettingsStore(private val context: Context) {

    private object Keys {
        val ENABLED       = booleanPreferencesKey("global_notch_enabled")
        val FREQ_HZ       = intPreferencesKey("global_notch_freq_hz")
        val DEPTH_DB      = floatPreferencesKey("global_notch_depth_db")
        val WIDTH_OCTAVES = floatPreferencesKey("global_notch_width_octaves")
    }

    val enabled: Flow<Boolean> =
        context.notchDataStore.data.map { it[Keys.ENABLED] ?: false }

    val freqHz: Flow<Int> =
        context.notchDataStore.data.map { it[Keys.FREQ_HZ] ?: 4000 }

    val depthDb: Flow<Float> =
        context.notchDataStore.data.map { it[Keys.DEPTH_DB] ?: -24f }

    val widthOctaves: Flow<Float> =
        context.notchDataStore.data.map { it[Keys.WIDTH_OCTAVES] ?: 1f }

    suspend fun setEnabled(value: Boolean) {
        context.notchDataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setFrequency(hz: Int) {
        context.notchDataStore.edit { it[Keys.FREQ_HZ] = hz }
    }

    suspend fun setDepth(db: Float) {
        context.notchDataStore.edit { it[Keys.DEPTH_DB] = db }
    }

    suspend fun setWidthOctaves(octaves: Float) {
        context.notchDataStore.edit { it[Keys.WIDTH_OCTAVES] = octaves }
    }
}
