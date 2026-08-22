package com.tinnomore.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinnomore.data.api.AudiogramRequest
import com.tinnomore.data.api.TinnitusApi
import com.tinnomore.data.api.TinnitusApiResult
import com.tinnomore.data.db.AppDatabase
import com.tinnomore.data.db.entity.AudiometryProfile
import com.tinnomore.data.repository.AudiometryRepository
import com.tinnomore.util.FrequencyPredictor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// ── Estado del análisis ML ────────────────────────────────────────────────────
sealed class ServerAnalysisState {
    object Idle    : ServerAnalysisState()
    object Loading : ServerAnalysisState()
    data class Success(val result: TinnitusApiResult) : ServerAnalysisState()
    data class Error(val message: String)             : ServerAnalysisState()
}

class AudiometryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudiometryRepository(
        AppDatabase.getDatabase(application).audiometryDao()
    )

    val frequencies = FrequencyPredictor.FREQUENCIES

    private val defaultThresholds get() = frequencies.associateWith { 20 }

    // ── Oído afectado ─────────────────────────────────────────────────────────
    private val _affectedEar = MutableStateFlow<String?>(null)
    val affectedEar: StateFlow<String?> = _affectedEar.asStateFlow()

    // ── Umbrales ──────────────────────────────────────────────────────────────
    private val _thresholds = MutableStateFlow<Map<Int, Int>>(defaultThresholds)
    val thresholds: StateFlow<Map<Int, Int>> = _thresholds.asStateFlow()

    // ── Frecuencia central ML ─────────────────────────────────────────────────
    private val _mlPredictedFc = MutableStateFlow<Int?>(null)
    val predictedFc: StateFlow<Int?> = _mlPredictedFc.asStateFlow()

    // ── Estados auxiliares ────────────────────────────────────────────────────
    private val _savedProfile = MutableStateFlow<AudiometryProfile?>(null)
    val savedProfile: StateFlow<AudiometryProfile?> = _savedProfile.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _serverState = MutableStateFlow<ServerAnalysisState>(ServerAnalysisState.Idle)
    val serverState: StateFlow<ServerAnalysisState> = _serverState.asStateFlow()

    // ── Estado de análisis por imagen ─────────────────────────────────────────
    private val _imageAnalysisState = MutableStateFlow<ServerAnalysisState>(ServerAnalysisState.Idle)
    val imageAnalysisState: StateFlow<ServerAnalysisState> = _imageAnalysisState.asStateFlow()

    /** URI de la imagen seleccionada/tomada (para mostrar preview) */
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // ── Compatibilidad ────────────────────────────────────────────────────────
    val left : StateFlow<Map<Int, Int>> get() = _thresholds
    val right: StateFlow<Map<Int, Int>> get() = _thresholds

    // ─── Selección de oído ────────────────────────────────────────────────────

    fun selectEar(ear: String?) {
        _affectedEar.value = ear
    }

    // ─── Edición de umbrales ──────────────────────────────────────────────────

    fun setThreshold(freq: Int, db: Int) {
        _thresholds.value = _thresholds.value.toMutableMap().also { it[freq] = db }
    }

    fun setLeftThreshold(freq: Int, db: Int)  = setThreshold(freq, db)
    fun setRightThreshold(freq: Int, db: Int) = setThreshold(freq, db)

    // ─── URI de imagen ────────────────────────────────────────────────────────

    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
        if (uri == null) _imageAnalysisState.value = ServerAnalysisState.Idle
    }

    // ─── Guardar y analizar con valores numéricos ─────────────────────────────

    fun saveAndPredict(patientId: Long) {
        val ear  = _affectedEar.value ?: "LEFT"
        val data = _thresholds.value

        viewModelScope.launch {
            val profile = AudiometryProfile(
                patientId        = patientId,
                affectedEar      = ear,
                channelData      = FrequencyPredictor.serializeChannelData(data),
                leftChannelData  = if (ear == "LEFT")  FrequencyPredictor.serializeChannelData(data) else "",
                rightChannelData = if (ear == "RIGHT") FrequencyPredictor.serializeChannelData(data) else "",
                mlPredictedFc    = null,
                predictedFc      = 4000
            )
            val newId = repository.saveProfile(profile)
            _savedProfile.value = profile.copy(id = newId)
            _message.value = "Audiometría guardada. Analizando con IA…"
        }

        analyzeWithServer(data, patientId)
    }

    private fun analyzeWithServer(data: Map<Int, Int>, patientId: Long) {
        fun db(freq: Int) = (data[freq] ?: 20).toFloat()
        val db3000 = ((data[2000] ?: 20) + (data[4000] ?: 20)) / 2f
        val db6000 = if (data.containsKey(6000)) db(6000)
                     else ((data[4000] ?: 20) + (data[8000] ?: 20)) / 2f

        val request = AudiogramRequest(
            db_250  = db(250),
            db_500  = db(500),
            db_1000 = db(1000),
            db_2000 = db(2000),
            db_3000 = db3000,
            db_4000 = db(4000),
            db_6000 = db6000,
            db_8000 = db(8000)
        )

        viewModelScope.launch {
            _serverState.value = ServerAnalysisState.Loading
            try {
                val response = TinnitusApi.service.analyze(TinnitusApi.API_KEY, request)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    _serverState.value = ServerAnalysisState.Success(result)

                    val mlFc = if (result.tinnitus) result.central_freq_hz else null
                    _mlPredictedFc.value = mlFc

                    val ear  = _affectedEar.value ?: "LEFT"
                    val data2 = _thresholds.value
                    val updated = AudiometryProfile(
                        patientId        = patientId,
                        affectedEar      = ear,
                        channelData      = FrequencyPredictor.serializeChannelData(data2),
                        leftChannelData  = if (ear == "LEFT")  FrequencyPredictor.serializeChannelData(data2) else "",
                        rightChannelData = if (ear == "RIGHT") FrequencyPredictor.serializeChannelData(data2) else "",
                        mlPredictedFc    = mlFc,
                        predictedFc      = mlFc ?: 4000
                    )
                    repository.saveProfile(updated)
                    _savedProfile.value = updated

                    _message.value = if (mlFc != null)
                        "Análisis ML completo. Frecuencia central: ${FrequencyPredictor.freqLabel(mlFc)}"
                    else
                        "Análisis ML: no se detectó patrón de tinnitus."
                } else {
                    _serverState.value = ServerAnalysisState.Error("Error del servidor: ${response.code()}")
                }
            } catch (e: java.net.ConnectException)      { _serverState.value = ServerAnalysisState.Error("Sin conexión al servidor") }
              catch (e: java.net.SocketTimeoutException) { _serverState.value = ServerAnalysisState.Error("Timeout — reintenta más tarde") }
              catch (e: Exception)                       { _serverState.value = ServerAnalysisState.Error("Error: ${e.message}") }
        }
    }

    // ─── Analizar imagen de audiograma ────────────────────────────────────────

    /**
     * Lee el URI de la imagen, construye un MultipartBody y llama a /analyze-image.
     * Actualiza [imageAnalysisState] y, si hay tinnitus, también [predictedFc].
     */
    fun analyzeImageAudiogram(context: Context, imageUri: Uri, patientId: Long) {
        viewModelScope.launch {
            _imageAnalysisState.value = ServerAnalysisState.Loading
            try {
                val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                    ?: run {
                        _imageAnalysisState.value = ServerAnalysisState.Error("No se pudo leer la imagen")
                        return@launch
                    }

                val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "audiogram.jpg", requestBody)

                val response = TinnitusApi.service.analyzeImage(TinnitusApi.API_KEY, part)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    _imageAnalysisState.value = ServerAnalysisState.Success(result)

                    // Si la imagen detecta tinnitus y tiene frecuencia, actualiza predictedFc
                    val mlFc = if (result.tinnitus) result.central_freq_hz else null
                    if (mlFc != null) {
                        _mlPredictedFc.value = mlFc
                        _message.value = "Imagen analizada. Frecuencia central: ${FrequencyPredictor.freqLabel(mlFc)}"
                    } else {
                        _message.value = if (result.tinnitus)
                            "Imagen: tinnitus detectado (sin frecuencia central)"
                        else
                            "Imagen: sin signos de tinnitus"
                    }
                } else {
                    _imageAnalysisState.value = ServerAnalysisState.Error(
                        "Error del servidor: ${response.code()}"
                    )
                }
            } catch (e: java.net.ConnectException)      { _imageAnalysisState.value = ServerAnalysisState.Error("Sin conexión al servidor") }
              catch (e: java.net.SocketTimeoutException) { _imageAnalysisState.value = ServerAnalysisState.Error("Timeout — reintenta más tarde") }
              catch (e: Exception)                       { _imageAnalysisState.value = ServerAnalysisState.Error("Error: ${e.message}") }
        }
    }

    // ─── Cargar perfil previo ─────────────────────────────────────────────────

    fun loadLatestProfile(patientId: Long) {
        viewModelScope.launch {
            val profile = repository.getLatestForPatient(patientId) ?: return@launch
            _savedProfile.value = profile
            _affectedEar.value  = profile.affectedEar.ifBlank { "LEFT" }
            _mlPredictedFc.value = profile.mlPredictedFc

            val channelStr = profile.channelData.ifBlank {
                if (profile.affectedEar == "RIGHT") profile.rightChannelData
                else profile.leftChannelData
            }
            _thresholds.value = FrequencyPredictor.parseChannelData(channelStr)
                .ifEmpty { defaultThresholds }
        }
    }

    fun clearMessage()          { _message.value = null }
    fun clearServerState()      { _serverState.value = ServerAnalysisState.Idle }
    fun clearImageAnalysisState() { _imageAnalysisState.value = ServerAnalysisState.Idle }
}
