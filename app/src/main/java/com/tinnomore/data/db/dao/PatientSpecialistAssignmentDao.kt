package com.tinnomore.data.db.dao

import androidx.room.*
import com.tinnomore.data.db.entity.PatientSpecialistAssignment
import com.tinnomore.data.db.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientSpecialistAssignmentDao {

    /** HU-05-1: pacientes asignados a un especialista, ordenados por nombre */
    @Query("""
        SELECT users.* FROM users
        INNER JOIN patient_specialist_assignments
            ON users.id = patient_specialist_assignments.patientId
        WHERE patient_specialist_assignments.specialistId = :specialistId
        ORDER BY users.name ASC
    """)
    fun getPatientsForSpecialist(specialistId: Long): Flow<List<User>>

    @Query("""
        SELECT users.* FROM users
        INNER JOIN patient_specialist_assignments
            ON users.id = patient_specialist_assignments.specialistId
        WHERE patient_specialist_assignments.patientId = :patientId
        ORDER BY users.name ASC
    """)
    fun getSpecialistsForPatient(patientId: Long): Flow<List<User>>

    @Query("SELECT * FROM patient_specialist_assignments")
    fun getAllAssignments(): Flow<List<PatientSpecialistAssignment>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(assignment: PatientSpecialistAssignment): Long

    @Query("DELETE FROM patient_specialist_assignments WHERE patientId = :patientId AND specialistId = :specialistId")
    suspend fun delete(patientId: Long, specialistId: Long)
}
