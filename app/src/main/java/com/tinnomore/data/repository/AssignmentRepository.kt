package com.tinnomore.data.repository

import com.tinnomore.data.db.dao.PatientSpecialistAssignmentDao
import com.tinnomore.data.db.entity.PatientSpecialistAssignment
import com.tinnomore.data.db.entity.User
import kotlinx.coroutines.flow.Flow

class AssignmentRepository(private val dao: PatientSpecialistAssignmentDao) {

    fun getPatientsForSpecialist(specialistId: Long): Flow<List<User>> =
        dao.getPatientsForSpecialist(specialistId)

    fun getSpecialistsForPatient(patientId: Long): Flow<List<User>> =
        dao.getSpecialistsForPatient(patientId)

    fun getAllAssignments(): Flow<List<PatientSpecialistAssignment>> =
        dao.getAllAssignments()

    suspend fun assign(patientId: Long, specialistId: Long) =
        dao.insert(PatientSpecialistAssignment(patientId = patientId, specialistId = specialistId))

    suspend fun unassign(patientId: Long, specialistId: Long) =
        dao.delete(patientId, specialistId)
}
