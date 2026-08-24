package com.tinnomore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.User
import com.tinnomore.data.db.entity.UserRole
import com.tinnomore.data.repository.AssignmentRepository
import com.tinnomore.data.repository.UserRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * HU-06: gestión de cuentas de usuario para el administrador.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(AppDatabase.getDatabase(application).userDao())
    private val assignmentRepo = AssignmentRepository(
        AppDatabase.getDatabase(application).patientSpecialistAssignmentDao()
    )

    val allUsers = repository.getAllUsers()

    val specialists = repository.getAllUsers().map { list -> list.filter { it.role == UserRole.SPECIALIST } }

    /** Ids de especialistas asignados a cada paciente, para pintar la UI de asignación. */
    val patientSpecialistIds = combine(
        assignmentRepo.getAllAssignments(),
        repository.getAllUsers()
    ) { assignments, _ ->
        assignments.groupBy({ it.patientId }, { it.specialistId })
    }

    fun deleteUser(user: User) {
        viewModelScope.launch { repository.deleteUser(user) }
    }

    fun assignSpecialist(patientId: Long, specialistId: Long) {
        viewModelScope.launch { assignmentRepo.assign(patientId, specialistId) }
    }

    fun unassignSpecialist(patientId: Long, specialistId: Long) {
        viewModelScope.launch { assignmentRepo.unassign(patientId, specialistId) }
    }
}
