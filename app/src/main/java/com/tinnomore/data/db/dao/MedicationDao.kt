package com.tinnomore.data.db.dao

import androidx.room.*
import com.tinnomore.data.db.entity.MedicationEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    /** Historial de medicamentos del paciente, más recientes primero */
    @Query("SELECT * FROM medications WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getMedicationsForPatient(patientId: Long): Flow<List<MedicationEntry>>

    /** HU-05-3: filtro por rango de fechas para el especialista */
    @Query("""
        SELECT * FROM medications
        WHERE patientId = :patientId AND timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
    """)
    fun getMedicationsForPatientBetween(patientId: Long, from: Long, to: Long): Flow<List<MedicationEntry>>

    @Insert
    suspend fun insert(medication: MedicationEntry): Long

    @Update
    suspend fun update(medication: MedicationEntry): Int

    @Delete
    suspend fun delete(medication: MedicationEntry): Int
}
