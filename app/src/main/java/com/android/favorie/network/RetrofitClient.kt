package com.android.favorie.network

import android.util.Log
import com.android.favorie.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://dev.offnal.site/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // JWT Bearer 토큰을 모든 요청에 자동으로 추가하는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val token = TokenManager.accessToken
        // TODO: 서버 확인용 임시 로그 — 토큰 전달 후 제거할 것
        Log.d("AuthToken", "Bearer $token")
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}