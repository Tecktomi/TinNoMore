package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sesión de terapia de muesca (Notch Therapy) aplicada al paciente.
 * Se configura automáticamente a partir de la fc predicha por HU-04.
 */
@Entity(tableName = "therapy_sessions")
data class TherapySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notchFrequency: Int,   // fc en Hz
    val intensityDb: Float,    // intensidad de la terapia en dB
    val durationSeconds: Int   // duración de la sesión en segundos
)
