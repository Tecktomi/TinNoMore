package com.tinnomore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.SymptomEntry
import com.tinnomore.data.db.entity.User
import com.tinnomore.data.repository.AssignmentRepository
import com.tinnomore.data.repository.SymptomRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PatientWithSymptoms(
    val patient: User,
    val symptoms: List<SymptomEntry>
)

/**
 * ViewModel que implementa HU-05 (acceso y monitoreo de datos de pacientes).
 *
 * Criterios cubiertos:
 *  HU-05-1: el especialista ve el listado de sus pacientes asignados al acceder al panel
 *  HU-05-2: al seleccionar un paciente se cargan los gráficos de evolución y síntomas
 *  HU-05-3: filtro de fechas actualiza la vista con los registros del rango indicado
 *  HU-05-4: filtro de orden alfabético ordena la lista
 *  HU-05-5: búsqueda por RUT (o nombre) muestra el paciente si existe
 */
class SpecialistViewModel(application: Application) : AndroidViewModel(application) {

    private val symptomRepo = SymptomRepository(AppDatabase.getDatabase(application).symptomDao())
    private val assignmentRepo = AssignmentRepository(
        AppDatabase.getDatabase(application).patientSpecialistAssignmentDao()
    )

    // ─── Lista de pacientes ──────────────────────────────────────────────────

    private val _allPatients = MutableStateFlow<List<User>>(emptyList())

    private val _filtered = MutableStateFlow<List<User>>(emptyList())
    val filtered: StateFlow<List<User>> = _filtered.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ─── Paciente seleccionado ───────────────────────────────────────────────

    private val _selected = MutableStateFlow<PatientWithSymptoms?>(null)
    val selected: StateFlow<PatientWithSymptoms?> = _selected.asStateFlow()

    private var symptomJob: Job? = null
    private var patientsJob: Job? = null
    private var loadedSpecialistId: Long? = null

    /**
     * HU-05-1: carga únicamente los pacientes asignados al especialista con sesión iniciada.
     * Se debe llamar una vez que se conoce el id del especialista logueado.
     */
    fun loadPatients(specialistId: Long) {
        if (loadedSpecialistId == specialistId) return
        loadedSpecialistId = specialistId
        patientsJob?.cancel()
        patientsJob = viewModelScope.launch {
            assignmentRepo.getPatientsForSpecialist(specialistId).collect { list ->
                _allPatients.value = list
                applyFilter()
            }
        }
    }

    // ─── HU-05-5: buscar por nombre o RUT ───────────────────────────────────

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        applyFilter()
    }

    private fun applyFilter() {
        val q = _searchQuery.value.trim()
        _filtered.value = if (q.isBlank()) {
            _allPatients.value
        } else {
            _allPatients.value.filter { p ->
                p.name.contains(q, ignoreCase = true) ||
                        p.rut.replace(".", "").replace("-", "")
                            .contains(q.replace(".", "").replace("-", ""))
            }
        }
    }

    // ─── HU-05-4: ordenar alfabéticamente ───────────────────────────────────

    fun sortAlphabetically() {
        _filtered.value = _filtered.value.sortedBy { it.name }
    }

    // ─── HU-05-2: seleccionar paciente → cargar evolución y síntomas ─────────

    fun selectPatient(patient: User) {
        // Acceso seguro: solo se puede abrir el perfil de un paciente asignado
        if (_allPatients.value.none { it.id == patient.id }) return
        symptomJob?.cancel()
        symptomJob = viewModelScope.launch {
            symptomRepo.getSymptomsForPatient(patient.id).collect { symptoms ->
                _selected.value = PatientWithSymptoms(patient, symptoms)
            }
        }
    }

    // ─── HU-05-3: filtro por fechas ──────────────────────────────────────────

    fun filterByDateRange(from: Long, to: Long) {
        val patient = _selected.value?.patient ?: return
        symptomJob?.cancel()
        symptomJob = viewModelScope.launch {
            symptomRepo.getSymptomsForPatientBetween(patient.id, from, to).collect { symptoms ->
                _selected.value = PatientWithSymptoms(patient, symptoms)
            }
        }
    }

    fun clearFilter() {
        val patient = _selected.value?.patient ?: return
        selectPatient(patient)
    }

    fun clearSelected() {
        symptomJob?.cancel()
        _selected.value = null
    }
}
