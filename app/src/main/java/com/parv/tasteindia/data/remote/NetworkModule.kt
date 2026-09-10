package com.parv.tasteindia.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * Builds the shared OkHttp / Retrofit stack. Plain functions (no DI framework) — the single
 * instances are owned by [com.parv.tasteindia.di.AppContainer].
 *
 * Coil reuses the same [OkHttpClient], so the whole app has one connection pool and one
 * timeout policy.
 */
object NetworkModule {

    private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

    /** Lenient enough for TheMealDB's inconsistent payloads, strict enough to catch shape drift. */
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun okHttpClient(enableLogging: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                if (enableLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()

    fun retrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun mealApi(retrofit: Retrofit): MealApi = retrofit.create(MealApi::class.java)
}
