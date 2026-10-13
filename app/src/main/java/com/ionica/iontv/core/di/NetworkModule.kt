package com.ionica.iontv.core.di

import com.ionica.iontv.core.network.AndroidNetworkMonitor
import com.ionica.iontv.core.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkModule {

    @Binds
    @Singleton
    fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}
