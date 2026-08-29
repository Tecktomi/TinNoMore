package com.tinnomore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.MedicationEntry
import com.tinnomore.data.repository.MedicationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MedicationUiState {
    object Loading : MedicationUiState()
    data class Success(val medications: List<MedicationEntry>) : MedicationUiState()
    data class Error(val message: String) : MedicationUiState()
}

/**
 * ViewModel del registro de medicamentos del paciente.
 * Sigue el mismo contrato que [SymptomViewModel]: estado de lista + toast de resultado.
 */
class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MedicationRepository(
        AppDatabase.getDatabase(application).medicationDao()
    )

    private val _uiState = MutableStateFlow<MedicationUiState>(MedicationUiState.Loading)
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    private val _toast = MutableStateFlow<Pair<Boolean, String>?>(null) // (isSuccess, message)
    val toast: StateFlow<Pair<Boolean, String>?> = _toast.asStateFlow()

    private var loadJob: Job? = null

    fun loadMedications(patientId: Long) {
        // El Flow de Room nunca termina: cancelar el anterior evita acumular
        // un colector por cada vez que se entra a la pestaña.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getMedicationsForPatient(patientId).collect { list ->
                _uiState.value = MedicationUiState.Success(list)
            }
        }
    }

    fun saveMedication(patientId: Long, name: String, dose: String?, notes: String?) {
        // El nombre del medicamento es obligatorio
        if (name.isBlank()) {
            _toast.value = false to "El nombre del medicamento es obligatorio"
            return
        }
        viewModelScope.launch {
            repository.addMedication(
                MedicationEntry(
                    patientId = patientId,
                    name      = name.trim(),
                    dose      = dose?.trim()?.ifBlank { null },
                    notes     = notes?.trim()?.ifBlank { null }
                )
            )
            _toast.value = true to "Medicamento registrado exitosamente"
        }
    }

    /** Igual que en síntomas: solo se puede editar dentro de las 24 horas siguientes. */
    fun updateMedication(
        id: Long,
        patientId: Long,
        originalTimestamp: Long,
        name: String,
        dose: String?,
        notes: String?
    ) {
        if (name.isBlank()) {
            _toast.value = false to "El nombre del medicamento es obligatorio"
            return
        }
        if (System.currentTimeMillis() - originalTimestamp > 24 * 3_600_000L) {
            _toast.value = false to "Solo puedes editar registros de las últimas 24 horas"
            return
        }
        viewModelScope.launch {
            repository.updateMedication(
                MedicationEntry(
                    id        = id,
                    patientId = patientId,
                    timestamp = originalTimestamp,
                    name      = name.trim(),
                    dose      = dose?.trim()?.ifBlank { null },
                    notes     = notes?.trim()?.ifBlank { null }
                )
            )
            _toast.value = true to "Medicamento registrado exitosamente"
        }
    }

    fun deleteMedication(medication: MedicationEntry) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            _toast.value = true to "Registro eliminado"
        }
    }

    fun clearToast() {
        _toast.value = null
    }
}
