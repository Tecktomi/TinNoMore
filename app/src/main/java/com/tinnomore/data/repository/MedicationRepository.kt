package com.tinnomore.data.repository

import com.tinnomore.data.db.dao.MedicationDao
import com.tinnomore.data.db.entity.MedicationEntry
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val dao: MedicationDao) {

    fun getMedicationsForPatient(patientId: Long): Flow<List<MedicationEntry>> =
        dao.getMedicationsForPatient(patientId)

    fun getMedicationsForPatientBetween(patientId: Long, from: Long, to: Long): Flow<List<MedicationEntry>> =
        dao.getMedicationsForPatientBetween(patientId, from, to)

    suspend fun addMedication(medication: MedicationEntry): Long = dao.insert(medication)

    suspend fun updateMedication(medication: MedicationEntry) = dao.update(medication)

    suspend fun deleteMedication(medication: MedicationEntry) = dao.delete(medication)
}
