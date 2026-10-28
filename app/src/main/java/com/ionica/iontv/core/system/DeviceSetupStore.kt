package com.ionica.iontv.core.system

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

private val BATTERY_SETTINGS_VISITED = booleanPreferencesKey("battery_settings_visited")

private val AUTOSTART_VISITED = booleanPreferencesKey("autostart_visited")

private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevicePreferences

@Singleton
class DeviceSetupStore @Inject constructor(
    @param:DevicePreferences private val dataStore: DataStore<Preferences>,
) {

    @Volatile
    private var cached: Preferences? = null

    suspend fun batterySettingsVisited(): Boolean = read(BATTERY_SETTINGS_VISITED)

    suspend fun autostartVisited(): Boolean = read(AUTOSTART_VISITED)

    suspend fun isSetupCompleted(): Boolean = read(SETUP_COMPLETED)

    suspend fun markBatterySettingsVisited() = write(BATTERY_SETTINGS_VISITED)

    suspend fun markAutostartVisited() = write(AUTOSTART_VISITED)

    suspend fun markSetupCompleted() = write(SETUP_COMPLETED)

    private suspend fun read(key: Preferences.Key<Boolean>): Boolean {
        val preferences = cached ?: dataStore.data.first().also { cached = it }
        return preferences[key] == true
    }

    private suspend fun write(key: Preferences.Key<Boolean>) {
        cached = dataStore.edit { it[key] = true }
    }
}
