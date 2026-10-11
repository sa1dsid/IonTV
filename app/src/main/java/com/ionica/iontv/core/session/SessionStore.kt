package com.ionica.iontv.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val DEVICE_ID = stringPreferencesKey("device_id")

private val ACCESS_TOKEN = stringPreferencesKey("access_token")

private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")

private val BINDING_ID = stringPreferencesKey("binding_id")

private val USERNAME = stringPreferencesKey("username")

private val SESSION_KEYS = listOf(ACCESS_TOKEN, REFRESH_TOKEN, BINDING_ID, USERNAME)

@Singleton
class SessionStore @Inject constructor(
    @param:SessionPreferences private val dataStore: DataStore<Preferences>,
) {

    /** Единственный источник правды о том, привязан ли телевизор. */
    val session: Flow<TvSession?> = dataStore.data
        .map { it.toSession() }
        .distinctUntilChanged()

    @Volatile
    private var cached: TvSession? = null

    @Volatile
    private var loaded = false

    suspend fun current(): TvSession? {
        if (!loaded) {
            cached = dataStore.data.first().toSession()
            loaded = true
        }
        return cached
    }

    /** Для перехватчиков OkHttp: они работают на потоке запроса и не умеют приостанавливаться. */
    fun blockingCurrent(): TvSession? = if (loaded) cached else runBlocking { current() }

    suspend fun open(tokens: SessionTokens, username: String?) {
        dataStore.edit { preferences ->
            preferences.putTokens(tokens)
            preferences.putOrRemove(USERNAME, username)
        }
        cached = TvSession(tokens.accessToken, tokens.refreshToken, tokens.bindingId, username)
        loaded = true
    }

    /** Бэкенд меняет refresh-токен при каждом обновлении: старый после этого недействителен. */
    suspend fun replaceTokens(tokens: SessionTokens) {
        val preferences = dataStore.edit { it.putTokens(tokens) }
        cached = preferences.toSession()
        loaded = true
    }

    /**
     * Сессия закончилась: телевизор отвязали, бэкенд отклонил refresh-токен или
     * пользователь сам отвязал ТВ. device_id выдаётся новый — старая привязка на
     * бэкенде может пережить сессию, и с прежним id повторная привязка упёрлась бы в 409.
     */
    suspend fun close() {
        dataStore.edit { preferences ->
            SESSION_KEYS.forEach { preferences.remove(it) }
            preferences[DEVICE_ID] = UUID.randomUUID().toString()
        }
        cached = null
        loaded = true
    }

    /** Постоянный UUID телевизора: создаётся при первом запуске и живёт до конца сессии. */
    suspend fun deviceId(): String {
        dataStore.data.first()[DEVICE_ID]?.let { return it }

        var deviceId = ""
        dataStore.edit { preferences ->
            deviceId = preferences[DEVICE_ID] ?: UUID.randomUUID().toString().also {
                preferences[DEVICE_ID] = it
            }
        }
        return deviceId
    }
}

private fun Preferences.toSession(): TvSession? {
    val access = this[ACCESS_TOKEN] ?: return null
    val refresh = this[REFRESH_TOKEN] ?: return null
    return TvSession(
        accessToken = access,
        refreshToken = refresh,
        bindingId = this[BINDING_ID],
        username = this[USERNAME],
    )
}

private fun MutablePreferences.putTokens(tokens: SessionTokens) {
    this[ACCESS_TOKEN] = tokens.accessToken
    this[REFRESH_TOKEN] = tokens.refreshToken
    putOrRemove(BINDING_ID, tokens.bindingId)
}

private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
    if (value == null) remove(key) else this[key] = value
}
