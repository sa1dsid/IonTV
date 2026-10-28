package com.ionica.iontv.feature.update.di

import com.ionica.iontv.BuildConfig
import com.ionica.iontv.feature.update.InstalledVersion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    @Provides
    @InstalledVersion
    fun provideInstalledVersion(): String = BuildConfig.VERSION_NAME
}
