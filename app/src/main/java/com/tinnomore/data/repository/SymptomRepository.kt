package com.tinnomore.data.repository

import com.tinnomore.data.db.dao.SymptomDao
import com.tinnomore.data.db.entity.SymptomEntry
import kotlinx.coroutines.flow.Flow

class SymptomRepository(private val dao: SymptomDao) {

    fun getSymptomsForPatient(patientId: Long): Flow<List<SymptomEntry>> =
        dao.getSymptomsForPatient(patientId)

    fun getSymptomsForPatientBetween(patientId: Long, from: Long, to: Long): Flow<List<SymptomEntry>> =
        dao.getSymptomsForPatientBetween(patientId, from, to)

    suspend fun addSymptom(symptom: SymptomEntry): Long = dao.insert(symptom)

    suspend fun updateSymptom(symptom: SymptomEntry) = dao.update(symptom)

    suspend fun getById(id: Long): SymptomEntry? = dao.getById(id)
}
