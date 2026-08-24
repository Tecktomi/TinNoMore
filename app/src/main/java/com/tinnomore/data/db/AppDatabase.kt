package com.tinnomore.data.db

import android.content.Context
import androidx.room.*
import com.tinnomore.data.db.dao.*
import com.tinnomore.data.db.entity.*

@Database(
    entities = [
        User::class,
        SymptomEntry::class,
        AudiometryProfile::class,
        CrisisRecord::class,
        TherapySession::class,
        PatientSpecialistAssignment::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun symptomDao(): SymptomDao
    abstract fun audiometryDao(): AudiometryDao
    abstract fun crisisRecordDao(): CrisisRecordDao
    abstract fun patientSpecialistAssignmentDao(): PatientSpecialistAssignmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tinnomore.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * Inserta datos de demostración si la base de datos está vacía.
         * Incluye un paciente, una especialista y un admin listos para probar.
         */
        suspend fun seedIfEmpty(db: AppDatabase) {
            val existing = db.userDao().getAllUsersSync()
            if (existing.isNotEmpty()) return

            // Usuarios demo
            val patientId1 = db.userDao().insert(
                User(name = "Juan Pérez", rut = "12.345.678-9",
                    email = "paciente@demo.com", password = "1234", role = UserRole.PATIENT)
            )
            val patientId2 = db.userDao().insert(
                User(name = "Ana González", rut = "9.876.543-2",
                    email = "paciente2@demo.com", password = "1234", role = UserRole.PATIENT)
            )
            db.userDao().insert(
                User(name = "Ivan Bustos", rut = "11.111.111-1",
                    email = "paciente3@demo.com", password = "1234", role = UserRole.PATIENT)

            )
            db.userDao().insert(
                User(name = "Juan Gaete", rut = "11.111.111-2",
                    email = "paciente4@demo.com", password = "1234", role = UserRole.PATIENT)

            )
            val specialistId1 = db.userDao().insert(
                User(name = "Carlos Muñoz", rut = "11.111.111-1",
                    email = "especialista@demo.com", password = "1234", role = UserRole.SPECIALIST)
            )
            db.userDao().insert(
                User(name = "Admin Sistema", rut = "00.000.000-0",
                    email = "admin@demo.com", password = "admin", role = UserRole.ADMIN)
            )

            // HU-05-1: pacientes asignados al especialista demo
            db.patientSpecialistAssignmentDao().insert(
                PatientSpecialistAssignment(patientId = patientId1, specialistId = specialistId1)
            )
            db.patientSpecialistAssignmentDao().insert(
                PatientSpecialistAssignment(patientId = patientId2, specialistId = specialistId1)
            )

            // Síntomas de demostración para paciente 1
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            listOf(
                Triple(8, 60, now - 6 * day),
                Triple(5, 30, now - 5 * day),
                Triple(7, 90, now - 4 * day),
                Triple(4, 20, now - 3 * day),
                Triple(6, 45, now - 2 * day),
                Triple(3, 15, now - 1 * day),
                Triple(5, 40, now - 3 * 3600_000L)
            ).forEach { (intensity, duration, ts) ->
                db.symptomDao().insert(
                    SymptomEntry(
                        patientId = patientId1,
                        timestamp = ts,
                        intensity = intensity,
                        durationMinutes = duration,
                        sleepImpact = intensity - 1,
                        concentrationImpact = intensity
                    )
                )
            }

            // Síntomas de demostración para paciente 2
            listOf(
                Triple(3, 20, now - 5 * day),
                Triple(6, 50, now - 3 * day),
                Triple(4, 35, now - 1 * day)
            ).forEach { (intensity, duration, ts) ->
                db.symptomDao().insert(
                    SymptomEntry(
                        patientId = patientId2,
                        timestamp = ts,
                        intensity = intensity,
                        durationMinutes = duration,
                        sleepImpact = intensity,
                        concentrationImpact = intensity - 1
                    )
                )
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)
}
