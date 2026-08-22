package com.tinnomore.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Una sola instancia de DataStore por aplicación
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tinnomore_session")

/**
 * Gestiona la sesión persistente del usuario usando DataStore Preferences.
 * Guarda el userId al iniciar sesión y lo elimina al cerrarla.
 * Al arrancar la app, AuthViewModel consulta este valor para restaurar la sesión
 * automáticamente sin necesidad de volver a ingresar credenciales.
 */
class SessionManager(private val context: Context) {

    companion object {
        private val KEY_USER_ID = longPreferencesKey("session_user_id")
    }

    /** Flow con el userId guardado, o null si no hay sesión activa. */
    val savedUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    /** Persiste el userId del usuario que acaba de iniciar sesión. */
    suspend fun saveSession(userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
        }
    }

    /** Elimina la sesión guardada (al cerrar sesión). */
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
        }
    }
}
