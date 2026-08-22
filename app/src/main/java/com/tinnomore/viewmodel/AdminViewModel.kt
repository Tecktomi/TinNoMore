package com.tinnomore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.User
import com.tinnomore.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * HU-06: gestión de cuentas de usuario para el administrador.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(AppDatabase.getDatabase(application).userDao())

    val allUsers = repository.getAllUsers()

    fun deleteUser(user: User) {
        viewModelScope.launch { repository.deleteUser(user) }
    }
}
