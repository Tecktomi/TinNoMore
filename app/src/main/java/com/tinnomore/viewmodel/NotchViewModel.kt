package com.tinnomore.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.util.NotchProcessor
import com.tinnomore.util.NotchProcessor.NoiseType
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.repository.AudiometryRepository
import com.tinnomore.util.FrequencyPredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class NotchGenState {
    object Idle       : NotchGenState()
    object Generating : NotchGenState()
    data class Ready(val freqHz: Int, val noise: NoiseType) : NotchGenState()
    data class Error(val msg: String) : NotchGenState()
}

class NotchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    // ── Parámetros ────────────────────────────────────────────────────────────
    private val _selectedFreq  = MutableStateFlow(4000)
    val selectedFreq: StateFlow<Int> = _selectedFreq.asStateFlow()

    private val _noiseType = MutableStateFlow(NoiseType.PINK)
    val noiseType: StateFlow<NoiseType> = _noiseType.asStateFlow()

    private val _volumeDb = MutableStateFlow(-6f)
    val volumeDb: StateFlow<Float> = _volumeDb.asStateFlow()

    // Ancho del notch en octavas. 1.0 = valor histórico (fc/√2 … fc×√2).
    // Rango recomendado en UI: 0.25 (angosto, muy preciso) a 2.0 (ancho, más agresivo).
    private val _widthOctaves = MutableStateFlow(1f)
    val widthOctaves: StateFlow<Float> = _widthOctaves.asStateFlow()

    // ── Estado ────────────────────────────────────────────────────────────────
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _genState = MutableStateFlow<NotchGenState>(NotchGenState.Idle)
    val genState: StateFlow<NotchGenState> = _genState.asStateFlow()

    // ── Internals ─────────────────────────────────────────────────────────────
    private var pcmBuffer: ShortArray? = null
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null

    val availableFrequencies = listOf(250, 500, 750, 1000, 1500, 2000, 3000, 4000, 6000, 8000, 10000, 12000)

    // ─── API pública ──────────────────────────────────────────────────────────

    fun setFrequency(hz: Int) {
        if (hz == _selectedFreq.value) return
        invalidateBuffer()
        _selectedFreq.value = hz
    }

    fun setNoiseType(type: NoiseType) {
        if (type == _noiseType.value) return
        invalidateBuffer()
        _noiseType.value = type
    }

    fun setVolume(db: Float) {
        _volumeDb.value = db
        audioTrack?.setVolume(dbToLinear(db))   // tiempo real, sin regenerar
    }

    /** Cambia el ancho del notch (en octavas). Requiere regenerar el buffer. */
    fun setWidthOctaves(octaves: Float) {
        val clamped = octaves.coerceIn(0.1f, 3f)
        if (clamped == _widthOctaves.value) return
        invalidateBuffer()
        _widthOctaves.value = clamped
    }

    /** Genera buffer + inicia reproducción. Si ya hay buffer válido, sólo reproduce. */
    fun play() {
        if (_isPlaying.value) return
        val buf = pcmBuffer
        if (buf == null) {
            generateAndPlay()
        } else {
            startPlayback(buf)
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
        _isPlaying.value = false
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    private fun invalidateBuffer() {
        stop()
        pcmBuffer = null
        _genState.value = NotchGenState.Idle
    }

    private fun generateAndPlay() {
        val freq  = _selectedFreq.value
        val noise = _noiseType.value
        val db    = _volumeDb.value
        val width = _widthOctaves.value
        viewModelScope.launch(Dispatchers.Default) {
            _genState.value = NotchGenState.Generating
            try {
                val buf = NotchProcessor.generate(app, noise, freq, db, width)
                pcmBuffer = buf
                _genState.value = NotchGenState.Ready(freq, noise)
                withContext(Dispatchers.Main) { startPlayback(buf) }
            } catch (e: Exception) {
                _genState.value = NotchGenState.Error(e.message ?: "Error al procesar audio")
            }
        }
    }

    private fun startPlayback(buf: ShortArray) {
        val sr = NotchProcessor.SAMPLE_RATE
        val minBuf = AudioTrack.getMinBufferSize(
            sr,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sr)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(dbToLinear(_volumeDb.value))
        track.play()
        audioTrack = track
        _isPlaying.value = true

        playJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val written = track.write(buf, 0, buf.size)
                if (written < 0) break
            }
        }
    }

    private fun dbToLinear(db: Float): Float = Math.pow(10.0, (db / 20.0)).toFloat()

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
