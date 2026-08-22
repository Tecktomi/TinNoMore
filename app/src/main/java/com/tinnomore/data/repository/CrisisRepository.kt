package com.tinnomore.data.repository

import com.tinnomore.data.db.dao.CrisisRecordDao
import com.tinnomore.data.db.entity.CrisisRecord
import kotlinx.coroutines.flow.Flow

class CrisisRepository(private val dao: CrisisRecordDao) {

    fun getCrisisRecordsForPatient(patientId: Long): Flow<List<CrisisRecord>> =
        dao.getCrisisRecordsForPatient(patientId)

    suspend fun saveCrisisRecord(record: CrisisRecord): Long = dao.insert(record)
}
