package com.android.bilzy.di

import com.android.bilzy.BuildConfig
import com.android.bilzy.data.remote.AuthInterceptor
import com.android.bilzy.data.remote.AuthRefreshApi
import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.TokenAuthenticator
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private fun logging() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    // ── 토큰 재발급 전용(인증/Authenticator 없음) ───────────────
    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging())
            .build()

    @Provides
    @Singleton
    fun provideAuthRefreshApi(@Named("refresh") client: OkHttpClient, json: Json): AuthRefreshApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AuthRefreshApi::class.java)
    }

    // ── 메인 클라이언트(인증 헤더 + 401 자동 재발급) ────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging())
            .authenticator(tokenAuthenticator)
            // OCR(/ocr/scan)·AI 정산(/calculate)은 서버가 Gemini를 호출해 오래 걸린다.
            // 기본 10초로는 타임아웃 나므로 읽기/쓰기를 넉넉히 늘린다.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(150, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideBilzyApi(retrofit: Retrofit): BilzyApi = retrofit.create(BilzyApi::class.java)
}
