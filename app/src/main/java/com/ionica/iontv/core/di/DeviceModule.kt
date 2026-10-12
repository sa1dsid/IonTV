package com.ionica.iontv.core.di

import com.ionica.iontv.core.device.DeviceName
import com.ionica.iontv.core.device.SystemDeviceName
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DeviceModule {

    @Binds
    fun bindDeviceName(impl: SystemDeviceName): DeviceName
}
