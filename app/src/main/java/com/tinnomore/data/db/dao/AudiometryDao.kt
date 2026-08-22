package com.tinnomore.data.db.dao

import androidx.room.*
import com.tinnomore.data.db.entity.AudiometryProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiometryDao {

    /** HU-04: Último perfil audiométrico guardado para un paciente */
    @Query("SELECT * FROM audiometry_profiles WHERE patientId = :patientId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForPatient(patientId: Long): AudiometryProfile?

    @Query("SELECT * FROM audiometry_profiles WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getAllForPatient(patientId: Long): Flow<List<AudiometryProfile>>

    @Insert
    suspend fun insert(profile: AudiometryProfile): Long
}
