package com.tinnomore.util

/**
 * HU-04: Algoritmo de predicción de la frecuencia central (fc) del acúfeno.
 *
 * Lógica: el tinnitus neurosensorial se asocia a una "muesca" (notch) en el audiograma,
 * es decir, una caída abrupta de la audición en una frecuencia rodeada de umbrales mejores.
 * El algoritmo busca primero ese notch en el rango 2000-8000 Hz (donde el tinnitus típicamente
 * ocurre). Si no hay notch claro, retorna simplemente la frecuencia con mayor pérdida.
 */
object FrequencyPredictor {

    val FREQUENCIES = listOf(250, 500, 1000, 2000, 4000, 8000)

    /**
     * Predice la fc a partir de los umbrales auditivos de ambos oídos.
     * @param left  mapa frecuencia(Hz) → umbral(dB HL) del oído izquierdo
     * @param right mapa frecuencia(Hz) → umbral(dB HL) del oído derecho
     * @return frecuencia central estimada en Hz
     */
    fun predictFc(left: Map<Int, Int>, right: Map<Int, Int>): Int {
        // Promedia los dos canales para obtener un perfil general
        val avg = FREQUENCIES.associateWith { freq ->
            ((left[freq] ?: 20) + (right[freq] ?: 20)) / 2
        }

        // 1. Buscar "notch": frecuencia donde la pérdida es mayor que sus vecinas
        var bestNotchFreq = -1
        var bestNotchDepth = 0

        for (i in 1 until FREQUENCIES.size - 1) {
            val f = FREQUENCIES[i]
            if (f < 2000) continue                       // tinnitus rara vez < 2 kHz
            val prev = avg[FREQUENCIES[i - 1]] ?: 20
            val curr = avg[f] ?: 20
            val next = avg[FREQUENCIES[i + 1]] ?: 20
            val depth = curr - (prev + next) / 2         // profundidad del notch
            if (depth > bestNotchDepth) {
                bestNotchDepth = depth
                bestNotchFreq = f
            }
        }

        if (bestNotchDepth >= 10 && bestNotchFreq != -1) return bestNotchFreq

        // 2. Sin notch claro: retornar la frecuencia con mayor umbral en rango alto
        return avg
            .filter { it.key >= 2000 }
            .maxByOrNull { it.value }
            ?.key ?: 4000
    }

    /** Deserializa "250:20,500:25,1000:30" → mapOf(250 to 20, 500 to 25, ...) */
    fun parseChannelData(data: String): Map<Int, Int> {
        if (data.isBlank()) return emptyMap()
        return data.split(",").mapNotNull { token ->
            val parts = token.trim().split(":")
            if (parts.size == 2) parts[0].toIntOrNull()?.let { f ->
                parts[1].toIntOrNull()?.let { t -> f to t }
            } else null
        }.toMap()
    }

    /** Serializa mapOf(250 to 20, 500 to 25, ...) → "250:20,500:25,..." */
    fun serializeChannelData(data: Map<Int, Int>): String =
        data.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** Etiqueta legible para una frecuencia (p.ej. 1000 → "1k Hz") */
    fun freqLabel(hz: Int): String =
        if (hz >= 1000) "${hz / 1000}k Hz" else "$hz Hz"
}
