package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro de toma de medicamento del paciente.
 * El paciente anota qué medicamento tomó y cuándo; el especialista
 * lo consulta desde el detalle del paciente para complementar el tratamiento.
 */
@Entity(tableName = "medications")
data class MedicationEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val name: String,        // nombre del medicamento, obligatorio
    val dose: String?,       // ej. "50 mg", "1 comprimido"
    val notes: String?       // motivo u observaciones
)
