package com.neetrust.core.network

import com.neetrust.ui.config.AppConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var tokenProvider: (() -> String?)? = null

    fun setTokenProvider(provider: () -> String?) {
        this.tokenProvider = provider
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenProvider?.invoke()
        val builder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        builder.header("Accept", "application/json")
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BACKEND_BASE_URL.let { if (it.endsWith("/")) it else "$it/" })
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    inline fun <reified T> createService(): T = retrofit.create(T::class.java)
}
