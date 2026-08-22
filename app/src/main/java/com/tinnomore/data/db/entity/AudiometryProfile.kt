package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil audiométrico del paciente.
 *
 * affectedEar    — "LEFT" o "RIGHT": el oído donde el paciente percibe el tinnitus.
 *                  Determina qué canal se usa en la pantalla de audiometría.
 * channelData    — umbrales del oído afectado: "250:20,500:25,1000:30,2000:60,4000:80,8000:70"
 * mlPredictedFc  — frecuencia central devuelta por el servidor ML (puede ser null si
 *                  el servidor no detectó tinnitus o hubo error de red).
 *
 * leftChannelData / rightChannelData se mantienen por compatibilidad con datos existentes
 * (Room fallbackToDestructiveMigration los borra en desarrollo, pero la API de Room los
 * necesita declarados si existen filas antiguas). En nuevos registros ambos se llenan
 * con el canal afectado; el canal sano queda vacío ("").
 */
@Entity(tableName = "audiometry_profiles")
data class AudiometryProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val timestamp: Long = System.currentTimeMillis(),

    // Canal afectado
    val affectedEar: String = "LEFT",          // "LEFT" | "RIGHT"
    val channelData: String = "",              // umbrales del oído afectado

    // Compatibilidad con esquema anterior (no se usan en lógica nueva)
    val leftChannelData: String  = "",
    val rightChannelData: String = "",

    // Frecuencia central predicha
    val mlPredictedFc: Int? = null,            // resultado del servidor ML  ← fuente de verdad
    val predictedFc: Int    = mlPredictedFc ?: 4000  // fallback/legacy
)
