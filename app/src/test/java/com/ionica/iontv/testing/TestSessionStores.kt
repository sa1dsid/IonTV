package com.ionica.iontv.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ionica.iontv.core.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File

private val CREATED = booleanPreferencesKey("test_file_created")

/**
 * DataStore на временном файле: настоящий формат хранения, без Android. Файл создаётся
 * сразу: первое чтение ещё не созданного файла DataStore может поймать посреди первой
 * записи из ViewModel, и тест падает на FileNotFoundException.
 */
fun preferencesIn(directory: File, name: String, scope: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(directory, "$name.preferences_pb") },
    ).also { store -> runBlocking { store.edit { it[CREATED] = true } } }

fun sessionStoreIn(directory: File, scope: CoroutineScope): SessionStore =
    SessionStore(preferencesIn(directory, "session", scope))
