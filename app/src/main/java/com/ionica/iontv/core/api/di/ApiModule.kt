package com.ionica.iontv.core.api.di

import com.ionica.iontv.BuildConfig
import com.ionica.iontv.core.api.AUTHORIZATION
import com.ionica.iontv.core.api.AuthInterceptor
import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.ServerClockInterceptor
import com.ionica.iontv.core.api.TokenAuthenticator
import com.ionica.iontv.core.api.Unauthenticated
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_SECONDS = 15L

private const val READ_TIMEOUT_SECONDS = 20L

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideJson(): Json = ionTvJson()

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedClient(serverClockInterceptor: ServerClockInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(serverClockInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                    redactHeader(AUTHORIZATION)
                }
            )
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideClient(
        @Unauthenticated base: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedApi(@Unauthenticated client: OkHttpClient, json: Json): IonTvApi =
        createIonTvApi(client, json, BuildConfig.API_BASE_URL)

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient, json: Json): IonTvApi =
        createIonTvApi(client, json, BuildConfig.API_BASE_URL)
}

fun ionTvJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
}

fun createIonTvApi(client: OkHttpClient, json: Json, baseUrl: String): IonTvApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(IonTvApi::class.java)
