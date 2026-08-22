package com.tinnomore.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

// ─── Request ──────────────────────────────────────────────────────────────────

data class AudiogramRequest(
    val db_250:  Float,
    val db_500:  Float,
    val db_1000: Float,
    val db_2000: Float,
    val db_3000: Float,
    val db_4000: Float,
    val db_6000: Float,
    val db_8000: Float
)

// ─── Response ─────────────────────────────────────────────────────────────────

data class TinnitusApiResult(
    val tinnitus:        Boolean,
    val tinnitus_score:  Float,
    val central_freq_hz: Int?,
    val freq_band:       String?,
    val freq_band_proba: Map<String, Float>?,
    val notch_depth_db:  Float,
    val confidence:      String,
    val pattern:         String,
    val recommendations: List<String>
)

// ─── Interface ────────────────────────────────────────────────────────────────

interface TinnitusApiService {

    @POST("analyze")
    suspend fun analyze(
        @Header("X-API-Key") apiKey: String,
        @Body input: AudiogramRequest
    ): Response<TinnitusApiResult>

    /** Envía una imagen JPEG/PNG del audiograma al endpoint /analyze-image */
    @Multipart
    @POST("analyze-image")
    suspend fun analyzeImage(
        @Header("X-API-Key") apiKey: String,
        @Part file: MultipartBody.Part
    ): Response<TinnitusApiResult>
}

// ─── Singleton ────────────────────────────────────────────────────────────────

object TinnitusApi {
    private const val BASE_URL = "https://api.tinnomore.xyz/"
    const val API_KEY = "pon-aqui-tu-clave-segura-8966ea19eefe968ede394f46"

    val service: TinnitusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TinnitusApiService::class.java)
    }
}
