package com.ionica.iontv.feature.sharing.di

import com.ionica.iontv.feature.sharing.SharingEngine
import com.ionica.iontv.feature.sharing.SharingMonitor
import com.ionica.iontv.feature.sharing.proxy.ProxyClient
import com.ionica.iontv.feature.sharing.proxy.SdkProxyClient
import com.ionica.iontv.feature.sharing.service.ServiceSharingController
import com.ionica.iontv.feature.sharing.service.SharingController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SharingModule {

    @Binds
    fun bindSharingMonitor(impl: SharingEngine): SharingMonitor

    @Binds
    fun bindSharingController(impl: ServiceSharingController): SharingController

    @Binds
    fun bindProxyClient(impl: SdkProxyClient): ProxyClient
}
