package com.ionica.iontv.core.system.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ionica.iontv.core.common.IoDispatcher
import com.ionica.iontv.core.system.AndroidBackgroundPermissions
import com.ionica.iontv.core.system.BackgroundPermissions
import com.ionica.iontv.core.system.DevicePreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SystemModule {

    @Binds
    fun bindBackgroundPermissions(impl: AndroidBackgroundPermissions): BackgroundPermissions

    companion object {

        @Provides
        @Singleton
        @DevicePreferences
        fun provideDevicePreferences(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + ioDispatcher),
            produceFile = { context.preferencesDataStoreFile("iontv_device") },
        )
    }
}
