package com.tinnomore.data.db.dao

import androidx.room.*
import com.tinnomore.data.db.entity.SymptomEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {

    /** HU-03: Todos los síntomas de un paciente, ordenados por fecha descendente */
    @Query("SELECT * FROM symptoms WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getSymptomsForPatient(patientId: Long): Flow<List<SymptomEntry>>

    /** HU-05: Filtro por rango de fechas para el especialista */
    @Query("""
        SELECT * FROM symptoms
        WHERE patientId = :patientId AND timestamp BETWEEN :from AND :to
        ORDER BY timestamp DESC
    """)
    fun getSymptomsForPatientBetween(patientId: Long, from: Long, to: Long): Flow<List<SymptomEntry>>

    @Query("SELECT * FROM symptoms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SymptomEntry?

    @Insert
    suspend fun insert(symptom: SymptomEntry): Long

    @Update
    suspend fun update(symptom: SymptomEntry): Int

    @Delete
    suspend fun delete(symptom: SymptomEntry): Int
}
