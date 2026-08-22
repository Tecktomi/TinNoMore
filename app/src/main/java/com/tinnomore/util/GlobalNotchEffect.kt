package com.tinnomore.util

import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * GlobalNotchEffect — aplica el notch de terapia a TODO el audio que sale del
 * dispositivo (no solo al de esta app), enganchando un [DynamicsProcessing]
 * al "audio session 0".
 *
 * Contexto importante (léase antes de tocar esto):
 *
 * El framework de Android mezcla la salida de todas las apps en un bus común
 * antes de llegar al DAC/Bluetooth. Un [AudioEffect] (o subclase, como
 * DynamicsProcessing) creado con audioSession = 0 se engancha a ESE bus
 * mezclado, no a la sesión de una app en particular. Esto es exactamente lo
 * que usan apps de EQ del sistema (Wavelet, Precise Volume, etc.) para
 * ecualizar todo el audio sin root. Por eso funciona para "cualquier otra
 * app" tal como pide el requerimiento.
 *
 * Limitaciones reales que hay que comunicar al usuario/cliente:
 *  - Requiere API 28+ (DynamicsProcessing). minSdk del proyecto se subió a 28.
 *  - El fabricante del dispositivo debe honrar efectos en la sesión global
 *    (AOSP estándar sí; algunos OEMs —cierto Samsung con su propio audio HAL—
 *    pueden ignorarlo o aplicar su propio DSP encima). No hay forma de
 *    garantizar esto al 100% sin certificación por fabricante.
 *  - No es "captura y reinyección" de audio de otras apps (eso Android no lo
 *    permite a apps normales desde Android 10 salvo con
 *    AudioPlaybackCaptureConfiguration, y ni así se puede reemplazar la
 *    salida). Es un filtro insertado en el mismo pipeline de mezcla, así que
 *    NO hay captura de contenido de terceros ni problema de privacidad.
 *  - Debe vivir en un foreground service; si el proceso muere, el efecto se
 *    libera y el notch global desaparece hasta que se reinicie el servicio.
 *  - Con auriculares Bluetooth con A2DP "offload" en algunos chips el DSP de
 *    postprocesado puede no aplicarse (el audio se manda comprimido
 *    directamente al códec). En la mayoría de los teléfonos Android estándar
 *    sí aplica.
 */
@RequiresApi(Build.VERSION_CODES.P)
object GlobalNotchEffect {

    private const val TAG = "GlobalNotchEffect"

    // Prioridad alta para intentar quedar antes de otros efectos de sistema (EQ, etc.)
    private const val PRIORITY = 100

    // Nº de bandas del EQ multibanda que se usa para "esculpir" el notch.
    // Log-espaciadas 20 Hz–20 kHz. Más bandas = notch más preciso pero
    // DynamicsProcessing soporta hasta 32; con 20 alcanza sobra de resolución
    // para el ancho de banda de 1 octava que usamos en toda la app.
    private const val BAND_COUNT = 20
    private const val MIN_FREQ = 20f
    private const val MAX_FREQ = 20000f

    private var dp: DynamicsProcessing? = null
    private var currentSessionId: Int = 0

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Crea (o reconfigura) el efecto global y lo deja activo.
     * Seguro de llamar repetidamente: si ya existe uno activo lo libera antes.
     *
     * @param fcHz     frecuencia central del notch (misma que en NotchProcessor)
     * @param depthDb  profundidad del notch en dB (negativo, ej. -24f = fuerte atenuación)
     */
    /**
     * @param fcHz         frecuencia central del notch (misma que en NotchProcessor)
     * @param depthDb      profundidad del notch en dB (negativo, ej. -24f = fuerte atenuación)
     * @param widthOctaves ancho total del notch en octavas (1f = fc/√2…fc×√2, igual que el histórico)
     */
    fun start(fcHz: Int, depthDb: Float = -24f, widthOctaves: Float = 1f) {
        release()
        try {
            val bandFreqs = buildLogBands()
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                /* channelCount   */ 2,
                /* preEqInUse     */ false,
                /* preEqBandCount */ 0,
                /* mbcInUse       */ false,
                /* mbcBandCount   */ 0,
                /* postEqInUse    */ true,
                /* postEqBandCount*/ BAND_COUNT,
                /* limiterInUse   */ true
            )
                .setPreferredFrameDuration(10f)
                .build()

            // AudioEffect con audioSession = 0 → se engancha al bus de mezcla
            // global del dispositivo, no a una sesión de app puntual.
            val globalSessionId = 0 // sesión 0 = bus de mezcla global del dispositivo
            val effect = DynamicsProcessing(PRIORITY, globalSessionId, config)
            currentSessionId = globalSessionId

            for (channel in 0 until 2) {
                val eq = DynamicsProcessing.Eq(true, true, BAND_COUNT)
                for (b in bandFreqs.indices) {
                    val freq = bandFreqs[b]
                    val gain = gainForBand(freq, fcHz.toFloat(), depthDb, widthOctaves)
                    val band = DynamicsProcessing.EqBand(true, freq, gain)
                    eq.setBand(b, band)
                }
                effect.setPostEqAllChannelsTo(eq)
            }

            effect.enabled = true
            dp = effect
            Log.i(TAG, "Notch global activo: fc=${fcHz}Hz depth=${depthDb}dB width=${widthOctaves}oct (session=0)")
        } catch (e: Exception) {
            // Si el fabricante bloquea efectos en sesión global, esto puede
            // lanzar IllegalStateException/UnsupportedOperationException.
            Log.e(TAG, "No se pudo iniciar el notch global en este dispositivo", e)
            dp = null
        }
    }

    /** Actualiza frecuencia/profundidad/ancho sin recrear el efecto si ya existe. */
    fun update(fcHz: Int, depthDb: Float = -24f, widthOctaves: Float = 1f) {
        val effect = dp
        if (effect == null) {
            start(fcHz, depthDb, widthOctaves)
            return
        }
        try {
            val bandFreqs = buildLogBands()
            for (channel in 0 until 2) {
                val eq = DynamicsProcessing.Eq(true, true, BAND_COUNT)
                for (b in bandFreqs.indices) {
                    val freq = bandFreqs[b]
                    val gain = gainForBand(freq, fcHz.toFloat(), depthDb, widthOctaves)
                    eq.setBand(b, DynamicsProcessing.EqBand(true, freq, gain))
                }
                effect.setPostEqAllChannelsTo(eq)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo actualizar el notch global, reintentando desde cero", e)
            start(fcHz, depthDb, widthOctaves)
        }
    }

    fun release() {
        try {
            dp?.enabled = false
            dp?.release()
        } catch (_: Exception) {
        } finally {
            dp = null
        }
    }

    val isActive: Boolean get() = dp != null

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun buildLogBands(): FloatArray {
        val minLog = log2(MIN_FREQ)
        val maxLog = log2(MAX_FREQ)
        val step = (maxLog - minLog) / (BAND_COUNT - 1)
        return FloatArray(BAND_COUNT) { i -> 2f.pow(minLog + step * i) }
    }

    /**
     * Misma filosofía que NotchProcessor.applyNotch: notch de ancho
     * configurable en octavas (widthOctaves, total, centrado en fc) con
     * transición suave hacia los bordes para no generar artefactos
     * audibles banda a banda.
     */
    private fun gainForBand(bandFreq: Float, fcHz: Float, depthDb: Float, widthOctaves: Float): Float {
        val octavesFromCenter = kotlin.math.abs(log2(bandFreq / fcHz))
        val halfWidthOctaves = widthOctaves.coerceIn(0.1f, 3f) / 2f
        return when {
            octavesFromCenter <= halfWidthOctaves * 0.6f -> depthDb
            octavesFromCenter >= halfWidthOctaves * 1.6f -> 0f
            else -> {
                // Interpolación lineal en la zona de transición
                val t = (octavesFromCenter - halfWidthOctaves * 0.6f) /
                        (halfWidthOctaves * 1.6f - halfWidthOctaves * 0.6f)
                depthDb * (1f - t.coerceIn(0f, 1f))
            }
        }.coerceIn(max(depthDb, -60f), 0f)
    }
}
