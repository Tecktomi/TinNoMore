// =============================================================================
// TinnitusApi.kt
// Coloca este archivo en: app/src/main/java/com/tinnitus/api/
// =============================================================================

package com.tinnomore.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class AudiogramInput(
    val db_250:  Float,
    val db_500:  Float,
    val db_1000: Float,
    val db_2000: Float,
    val db_3000: Float,
    val db_4000: Float,
    val db_6000: Float,
    val db_8000: Float
)

data class TinnitusResult(
    val tinnitus:         Boolean,
    val tinnitus_score:   Float,
    val central_freq_hz:  Int?,
    val freq_band:        String?,
    val freq_band_proba:  Map<String, Float>?,
    val notch_depth_db:   Float,
    val confidence:       String,
    val pattern:          String,
    val recommendations:  List<String>
) {
    // Helpers para la UI
    fun confidenceLabel() = when(confidence) {
        "high"   -> "Alta confianza"
        "medium" -> "Confianza media"
        else     -> "Baja confianza"
    }

    fun freqBandLabel() = when(freq_band) {
        "low"    -> "Frecuencias bajas (≤1kHz)"
        "medium" -> "Frecuencias medias (2-3kHz)"
        "high"   -> "Frecuencias altas (4-8kHz)"
        else     -> "No determinada"
    }

    fun centralFreqDisplay() = central_freq_hz?.let {
        if (it >= 1000) "${it/1000}kHz" else "${it}Hz"
    } ?: "No detectada"
}

data class HealthResponse(
    val status:  String,
    val version: String
)

// ─── Retrofit Interface ───────────────────────────────────────────────────────

interface TinnitusApiService {

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @POST("analyze")
    suspend fun analyze(
        @Header("X-API-Key") apiKey: String,
        @Body input: AudiogramInput
    ): Response<TinnitusResult>
}

// ─── Singleton del cliente ────────────────────────────────────────────────────

object TinnitusApi {

    // ⚠️ Cambia esto por la IP o dominio de tu Oracle Cloud
    private const val BASE_URL = "https://api.tinnomore.xyz/"

    // ⚠️ Mueve esto a BuildConfig o EncryptedSharedPreferences en producción
    const val API_KEY = "pon-aqui-tu-clave-segura-8966ea19eefe968ede394f46"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // El modelo puede tardar en cargar la 1ra vez
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: TinnitusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TinnitusApiService::class.java)
    }
}
