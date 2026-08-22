package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HU-03: Registro de síntomas diarios del paciente.
 * Almacena intensidad (obligatoria, 1-10), duración, impacto en sueño y concentración.
 */
@Entity(tableName = "symptoms")
data class SymptomEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val intensity: Int,             // 1-10, obligatorio
    val durationMinutes: Int?,      // minutos de duración del episodio
    val sleepImpact: Int?,          // impacto en sueño 1-10
    val concentrationImpact: Int?   // impacto en concentración 1-10
)
