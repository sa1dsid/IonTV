package com.ionica.iontv.core.di

import com.ionica.iontv.core.common.SystemTimeSource
import com.ionica.iontv.core.common.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TimeModule {

    @Binds
    @Singleton
    fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
