package com.tinnomore.data.repository

import com.tinnomore.data.db.dao.AudiometryDao
import com.tinnomore.data.db.entity.AudiometryProfile
import kotlinx.coroutines.flow.Flow

class AudiometryRepository(private val dao: AudiometryDao) {

    suspend fun getLatestForPatient(patientId: Long): AudiometryProfile? =
        dao.getLatestForPatient(patientId)

    fun getAllForPatient(patientId: Long): Flow<List<AudiometryProfile>> =
        dao.getAllForPatient(patientId)

    suspend fun saveProfile(profile: AudiometryProfile): Long = dao.insert(profile)
}
