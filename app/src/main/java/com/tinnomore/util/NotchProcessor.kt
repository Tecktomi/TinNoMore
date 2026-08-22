package com.tinnomore.util

import android.content.Context
import kotlin.math.*

/**
 * NotchProcessor — DSP puro en Kotlin.
 *
 * Opera sobre archivos WAV reales (stereo, PCM-16, 44 100 Hz) almacenados en
 * res/raw. Aplica un filtro IIR biquad notch de 2º orden con ancho de banda
 * de 1 octava (fc/√2 … fc×√2) sobre cada canal por separado.
 *
 * Flujo:
 *   WAV (res/raw)  →  ShortArray stereo  →  Float izq/der  →
 *   notch biquad   →  applyGain          →  ShortArray stereo
 */
object NotchProcessor {

    const val SAMPLE_RATE    = 44100
    const val CHANNELS       = 2          // stereo

    // IDs de res/raw (nombres en minúsculas, sin extensión)
    const val RAW_PINK  = "pinknoise_amplitude1"
    const val RAW_WHITE = "whitenoise_amplitude1"
    const val RAW_BROWN = "brownnoise_amplitude1"

    enum class NoiseType(val rawName: String, val label: String) {
        PINK ("pinknoise_amplitude1",  "Rosa"),
        WHITE("whitenoise_amplitude1", "Blanco"),
        BROWN("brownnoise_amplitude1", "Marrón")
    }

    // ─── Entrada principal ────────────────────────────────────────────────────

    /**
     * Carga el WAV desde res/raw, aplica el notch y devuelve PCM-16 stereo listo
     * para AudioTrack (CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT, 44100 Hz).
     *
     * @param context   contexto de la app (para acceder a res/raw)
     * @param noiseType tipo de ruido a procesar
     * @param fcHz      frecuencia central del notch en Hz
     * @param gainDb    ganancia de salida en dB (0 = sin cambio)
     */
    fun generate(
        context:   Context,
        noiseType: NoiseType,
        fcHz:      Int,
        gainDb:    Float = 0f,
        widthOctaves: Float = 1f
    ): ShortArray {
        val rawId = context.resources.getIdentifier(noiseType.rawName, "raw", context.packageName)
        require(rawId != 0) { "Recurso raw no encontrado: ${noiseType.rawName}" }

        // Leer WAV completo → ShortArray interleaved [L0, R0, L1, R1, …]
        val pcm = readWavPcm(context, rawId)

        // Separar canales
        val totalFrames = pcm.size / CHANNELS
        val left  = FloatArray(totalFrames)
        val right = FloatArray(totalFrames)
        for (i in 0 until totalFrames) {
            left [i] = pcm[i * 2    ] / 32768f
            right[i] = pcm[i * 2 + 1] / 32768f
        }

        // Aplicar notch a cada canal
        applyNotch(left,  fcHz, SAMPLE_RATE, widthOctaves)
        applyNotch(right, fcHz, SAMPLE_RATE, widthOctaves)

        // Ganancia
        val linear = 10f.pow(gainDb / 20f)

        // Intercalar de nuevo → ShortArray
        val out = ShortArray(pcm.size)
        for (i in 0 until totalFrames) {
            out[i * 2    ] = (left [i] * linear).coerceIn(-1f, 1f).times(32767f).toInt().toShort()
            out[i * 2 + 1] = (right[i] * linear).coerceIn(-1f, 1f).times(32767f).toInt().toShort()
        }
        return out
    }

    // ─── Lector de WAV (cabecera estándar de 44 bytes) ────────────────────────

    private fun readWavPcm(context: Context, rawId: Int): ShortArray {
        context.resources.openRawResource(rawId).use { stream ->
            val header = ByteArray(44)
            stream.read(header)
            // Tamaño de datos en bytes (bytes 40-43, little-endian)
            val dataBytes =
                (header[40].toInt() and 0xFF) or
                ((header[41].toInt() and 0xFF) shl 8) or
                ((header[42].toInt() and 0xFF) shl 16) or
                ((header[43].toInt() and 0xFF) shl 24)
            val numSamples = dataBytes / 2          // PCM-16 → 2 bytes por sample
            val buf = ByteArray(dataBytes)
            var offset = 0
            while (offset < dataBytes) {
                val n = stream.read(buf, offset, dataBytes - offset)
                if (n < 0) break
                offset += n
            }
            val shorts = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                shorts[i] = ((buf[i * 2].toInt() and 0xFF) or
                             (buf[i * 2 + 1].toInt() shl 8)).toShort()
            }
            return shorts
        }
    }

    // ─── Biquad notch IIR (in-place sobre Float) ──────────────────────────────

    /**
     * H(z) = (1 - 2cos(w0)z⁻¹ + z⁻²) / (1 - 2r·cos(w0)z⁻¹ + r²z⁻²)
     * Ancho de banda configurable en octavas alrededor de fc.
     * widthOctaves = 1.0 → BW_Hz ≈ fc × 0.7071 (fc/√2 a fc×√2, el valor por defecto anterior)
     * widthOctaves menor → notch más angosto (afecta menos frecuencias vecinas)
     * widthOctaves mayor → notch más ancho (corta más espectro alrededor de fc)
     */
    private fun applyNotch(buf: FloatArray, fcHz: Int, sr: Int, widthOctaves: Float = 1f) {
        val w0   = 2.0 * PI * fcHz / sr
        val n    = widthOctaves.toDouble()
        val bwHz = fcHz * (2.0.pow(n / 2.0) - 2.0.pow(-n / 2.0))
        val r    = (1.0 - PI * bwHz / sr).coerceIn(0.0, 0.999) // evita inestabilidad del filtro con anchos grandes

        val cosW = cos(w0)
        val a1num = -2.0 * cosW
        val b1num = -2.0 * r * cosW
        val b2num =  r * r

        var x1 = 0.0; var x2 = 0.0
        var y1 = 0.0; var y2 = 0.0
        for (i in buf.indices) {
            val x0 = buf[i].toDouble()
            val y0 = x0 + a1num * x1 + x2 - b1num * y1 - b2num * y2
            buf[i] = y0.toFloat()
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
        }
    }
}
