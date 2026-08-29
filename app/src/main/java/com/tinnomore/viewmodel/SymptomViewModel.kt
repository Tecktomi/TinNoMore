package com.tinnomore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.SymptomEntry
import com.tinnomore.data.repository.SymptomRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SymptomUiState {
    object Loading : SymptomUiState()
    data class Success(val symptoms: List<SymptomEntry>) : SymptomUiState()
    data class Error(val message: String) : SymptomUiState()
}

/**
 * ViewModel que implementa HU-03 (registro de síntomas).
 *
 * Criterios cubiertos:
 *  HU-03-1: guardar registro con intensidad 1-10 → mensaje "registro guardado exitosamente"
 *  HU-03-2: intentar guardar sin intensidad → mensaje de error, no se persiste
 *  HU-03-3: historial ordenado por fecha descendente con fecha e intensidad
 *  HU-03-4: sin registros → mensaje "No tienes registros." + botón "Crear mi primer registro"
 *  HU-03-5: editar registro creado hace menos de 24 horas
 */
class SymptomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SymptomRepository(
        AppDatabase.getDatabase(application).symptomDao()
    )

    private val _uiState = MutableStateFlow<SymptomUiState>(SymptomUiState.Loading)
    val uiState: StateFlow<SymptomUiState> = _uiState.asStateFlow()

    private val _toast = MutableStateFlow<Pair<Boolean, String>?>(null) // (isSuccess, message)
    val toast: StateFlow<Pair<Boolean, String>?> = _toast.asStateFlow()

    private var loadJob: Job? = null

    fun loadSymptoms(patientId: Long) {
        // El Flow de Room nunca termina: cancelar el anterior evita acumular un
        // colector por cada llamada (Inicio y Síntomas comparten este ViewModel y
        // ambos invocan loadSymptoms al entrar en composición).
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getSymptomsForPatient(patientId).collect { list ->
                _uiState.value = SymptomUiState.Success(list)
            }
        }
    }

    // ─── HU-03-1 / HU-03-2 ─────────────────────────────────────────────────

    fun saveSymptom(
        patientId: Long,
        intensity: Int?,
        durationMinutes: Int?,
        sleepImpact: Int?,
        concentrationImpact: Int?
    ) {
        // HU-03-2: intensidad obligatoria
        if (intensity == null) {
            _toast.value = false to "La intensidad es obligatoria"
            return
        }
        viewModelScope.launch {
            repository.addSymptom(
                SymptomEntry(
                    patientId = patientId,
                    intensity = intensity,
                    durationMinutes = durationMinutes,
                    sleepImpact = sleepImpact,
                    concentrationImpact = concentrationImpact
                )
            )
            _toast.value = true to "Registro guardado exitosamente"
        }
    }

    // ─── HU-03-5 ────────────────────────────────────────────────────────────

    fun updateSymptom(
        id: Long,
        patientId: Long,
        originalTimestamp: Long,
        intensity: Int?,
        durationMinutes: Int?,
        sleepImpact: Int?,
        concentrationImpact: Int?
    ) {
        if (intensity == null) {
            _toast.value = false to "La intensidad es obligatoria"
            return
        }
        val ageMs = System.currentTimeMillis() - originalTimestamp
        if (ageMs > 24 * 3_600_000L) {
            _toast.value = false to "Solo puedes editar registros de las últimas 24 horas"
            return
        }
        viewModelScope.launch {
            repository.updateSymptom(
                SymptomEntry(
                    id = id,
                    patientId = patientId,
                    timestamp = originalTimestamp,
                    intensity = intensity,
                    durationMinutes = durationMinutes,
                    sleepImpact = sleepImpact,
                    concentrationImpact = concentrationImpact
                )
            )
            _toast.value = true to "Registro guardado exitosamente"
        }
    }

    fun clearToast() {
        _toast.value = null
    }
}
