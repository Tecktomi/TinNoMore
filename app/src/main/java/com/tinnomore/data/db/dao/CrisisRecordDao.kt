package com.tinnomore.data.db.dao

import androidx.room.*
import com.tinnomore.data.db.entity.CrisisRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CrisisRecordDao {

    /** HU-01 / HU-02: Historial de crisis de un paciente */
    @Query("SELECT * FROM crisis_records WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getCrisisRecordsForPatient(patientId: Long): Flow<List<CrisisRecord>>

    /** HU-05-3: filtro por rango de fechas para el especialista (exposición a ruido) */
    @Query("""
        SELECT * FROM crisis_records
        WHERE patientId = :patientId AND timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
    """)
    fun getCrisisRecordsForPatientBetween(patientId: Long, from: Long, to: Long): Flow<List<CrisisRecord>>

    @Insert
    suspend fun insert(record: CrisisRecord): Long
}
