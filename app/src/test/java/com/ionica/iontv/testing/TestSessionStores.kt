package com.ionica.iontv.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ionica.iontv.core.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** DataStore на временном файле: настоящий формат хранения, без Android. */
fun preferencesIn(directory: File, name: String, scope: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(directory, "$name.preferences_pb") },
    )

fun sessionStoreIn(directory: File, scope: CoroutineScope): SessionStore =
    SessionStore(preferencesIn(directory, "session", scope))
