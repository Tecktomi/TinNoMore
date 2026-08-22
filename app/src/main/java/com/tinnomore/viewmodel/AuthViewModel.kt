package com.tinnomore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.SessionManager
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.User
import com.tinnomore.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Gestiona la autenticación del usuario.
 *
 * v0.2 – Sesión persistente:
 *  • Al iniciar la app, intenta restaurar la sesión desde DataStore (sin login manual).
 *  • Al hacer login exitoso, guarda el userId en DataStore.
 *  • Al hacer logout, borra el userId de DataStore.
 *  • isSessionLoading = true mientras se comprueba la sesión guardada; la pantalla
 *    de login muestra un indicador de carga y no redirige hasta que sea false.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository    = UserRepository(AppDatabase.getDatabase(application).userDao())
    private val sessionManager = SessionManager(application)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    /** true mientras se consulta la sesión guardada al arrancar la app */
    private val _isSessionLoading = MutableStateFlow(true)
    val isSessionLoading: StateFlow<Boolean> = _isSessionLoading.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Restaurar sesión al arrancar ───────────────────────────────────────
    init {
        viewModelScope.launch {
            try {
                val savedId = sessionManager.savedUserId.first()
                if (savedId != null) {
                    val user = repository.getUserById(savedId)
                    if (user != null) {
                        _currentUser.value = user
                    } else {
                        // El usuario fue eliminado; limpiar sesión huérfana
                        sessionManager.clearSession()
                    }
                }
            } finally {
                _isSessionLoading.value = false
            }
        }
    }

    // ── Login manual ───────────────────────────────────────────────────────
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Por favor ingresa correo y contraseña"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.login(email, password)
            if (user != null) {
                sessionManager.saveSession(user.id)   // ← guardar sesión
                _currentUser.value = user
                _loginError.value = null
            } else {
                _loginError.value = "Correo o contraseña incorrectos"
            }
            _isLoading.value = false
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()              // ← borrar sesión
            _currentUser.value = null
            _loginError.value = null
        }
    }

    fun clearError() { _loginError.value = null }
}
