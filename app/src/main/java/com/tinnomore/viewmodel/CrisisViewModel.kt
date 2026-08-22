package com.tinnomore.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.CrisisRecord
import com.tinnomore.data.repository.CrisisRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10

// ─── Estado de la pantalla de crisis ────────────────────────────────────────
enum class CrisisState {
    IDLE,          // esperando que el usuario pulse el botón
    RECORDING,     // grabando audio (10 segundos)
    ANALYZING,     // procesando el archivo grabado
    SAFE,          // ambiente dentro del rango seguro (0-85 dB)
    DANGEROUS,     // ambiente peligroso (> 85 dB)
    NO_PERMISSION  // sin permiso de micrófono
}

data class CrisisResult(
    val decibels: Float,
    val isSafe: Boolean,
    val therapyIntensity: Float?,   // intensidad modificada si el ambiente es peligroso
    val message: String
)

/**
 * ViewModel que implementa HU-01 (botón de crisis + grabación 10 s)
 * y HU-02 (análisis de decibelios → modificación de terapia).
 *
 * Criterios cubiertos:
 *  HU-01-1: al pulsar Crisis → graba con micrófono durante 10 segundos
 *  HU-01-2: sin permiso → estado NO_PERMISSION con mensaje
 *  HU-01-3: guarda el archivo de audio localmente al finalizar
 *  HU-02-1: detecta nuevo archivo y analiza decibelios
 *  HU-02-2: decibelios fuera de rango seguro → modifica intensidad + advertencia
 *  HU-02-3: decibelios en rango seguro → muestra terapias sin modificar
 */
class CrisisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CrisisRepository(
        AppDatabase.getDatabase(application).crisisRecordDao()
    )

    private val _state = MutableStateFlow(CrisisState.IDLE)
    val state: StateFlow<CrisisState> = _state.asStateFlow()

    private val _result = MutableStateFlow<CrisisResult?>(null)
    val result: StateFlow<CrisisResult?> = _result.asStateFlow()

    private val _progress = MutableStateFlow(0f)   // 0.0 → 1.0
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var recordingJob: Job? = null

    companion object {
        const val RECORDING_DURATION_MS = 10_000L
        const val SAFE_DB_THRESHOLD = 85f   // rango seguro definido en el documento: 0-100 dB,
                                             // pero > 85 dB requiere protección auditiva
    }

    // ─── HU-01 ──────────────────────────────────────────────────────────────

    /** Inicia la grabación (llamar sólo cuando el permiso ya fue concedido). */
    fun startRecording(patientId: Long, filesDir: File) {
        _state.value = CrisisState.RECORDING
        _progress.value = 0f
        _elapsedSeconds.value = 0

        audioFile = File(filesDir, "crisis_${System.currentTimeMillis()}.3gp")

        try {
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }

            recordingJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                var maxAmplitude = 0

                while (System.currentTimeMillis() - startTime < RECORDING_DURATION_MS) {
                    val elapsed = System.currentTimeMillis() - startTime
                    _progress.value = elapsed.toFloat() / RECORDING_DURATION_MS
                    _elapsedSeconds.value = (elapsed / 1000).toInt()

                    // Leer amplitud máxima acumulada
                    val amp = mediaRecorder?.maxAmplitude ?: 0
                    if (amp > maxAmplitude) maxAmplitude = amp

                    delay(200)
                }

                _progress.value = 1f
                stopRecorder()

                // HU-01-3: archivo guardado; HU-02-1: analizamos
                analyzeAndSave(patientId, maxAmplitude)
            }
        } catch (e: Exception) {
            // HU-01-2: fallo al acceder al micrófono
            _state.value = CrisisState.NO_PERMISSION
            releaseRecorder()
        }
    }

    /** Llama cuando el sistema niega el permiso RECORD_AUDIO. */
    fun onPermissionDenied() {
        _state.value = CrisisState.NO_PERMISSION
    }

    // ─── HU-02 ──────────────────────────────────────────────────────────────

    private suspend fun analyzeAndSave(patientId: Long, maxAmplitude: Int) {
        _state.value = CrisisState.ANALYZING

        // Conversión de amplitud MediaRecorder (0-32767) a dB SPL aproximados.
        // Fórmula estándar: dB = 20·log10(amp / 32767) + 90  (offset empírico 90 dB)
        val decibels = if (maxAmplitude > 0) {
            (20.0 * log10(maxAmplitude.toDouble() / 32767.0) + 90.0)
                .toFloat().coerceIn(0f, 120f)
        } else {
            0f
        }

        val isSafe = decibels <= SAFE_DB_THRESHOLD

        val therapyIntensity: Float?
        val message: String

        if (isSafe) {
            // HU-02-3: ambiente seguro
            therapyIntensity = null
            message = "El ambiente es seguro para tu tinnitus " +
                    "(${decibels.toInt()} dB). "
            _state.value = CrisisState.SAFE
        } else {
            // HU-02-2: decibelios fuera del rango seguro → modificar intensidad
            // La intensidad extra escala linealmente entre 0.1 y 1.0
            // según cuánto supere el umbral (máximo razonable +35 dB)
            therapyIntensity = ((decibels - SAFE_DB_THRESHOLD) / 35f).coerceIn(0.1f, 1.0f)
            message = "El ruido ambiental detectado (${decibels.toInt()} dB) " +
                    "podría provocar daño permanente a tus oídos. " +
                    "Se recomienda abandonar el lugar cuanto antes o usar protectores de oído."
            _state.value = CrisisState.DANGEROUS
        }

        _result.value = CrisisResult(decibels, isSafe, therapyIntensity, message)

        // Persistir registro de crisis
        repository.saveCrisisRecord(
            CrisisRecord(
                patientId = patientId,
                audioFilePath = audioFile?.absolutePath,
                maxDecibels = decibels,
                therapyModified = !isSafe,
                modifiedIntensity = therapyIntensity
            )
        )
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    fun reset() {
        recordingJob?.cancel()
        stopRecorder()
        _state.value = CrisisState.IDLE
        _result.value = null
        _progress.value = 0f
        _elapsedSeconds.value = 0
    }

    private fun stopRecorder() {
        try { mediaRecorder?.stop() } catch (_: Exception) {}
        try { mediaRecorder?.reset() } catch (_: Exception) {}
        releaseRecorder()
    }

    private fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(getApplication())
        else
            MediaRecorder()

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        releaseRecorder()
    }
}
