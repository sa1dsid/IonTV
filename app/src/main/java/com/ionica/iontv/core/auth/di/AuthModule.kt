package com.ionica.iontv.core.auth.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.ionica.iontv.core.auth.CredentialManagerGoogleSignIn
import com.ionica.iontv.core.auth.GoogleSignIn
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {

    @Binds
    fun bindGoogleSignIn(impl: CredentialManagerGoogleSignIn): GoogleSignIn

    companion object {

        @Provides
        @Singleton
        fun provideCredentialManager(
            @ApplicationContext context: Context,
        ): CredentialManager = CredentialManager.create(context)
    }
}
