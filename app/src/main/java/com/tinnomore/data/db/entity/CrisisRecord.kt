package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HU-01 / HU-02: Registro de crisis de tinnitus.
 * Almacena el audio grabado, los decibelios medidos y si la terapia fue modificada.
 */
@Entity(tableName = "crisis_records")
data class CrisisRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String?,        // ruta local del archivo .3gp grabado
    val maxDecibels: Float,            // decibelios máximos detectados
    val therapyModified: Boolean,      // true si el ambiente era peligroso
    val modifiedIntensity: Float?      // nueva intensidad calculada si fue modificada
)
