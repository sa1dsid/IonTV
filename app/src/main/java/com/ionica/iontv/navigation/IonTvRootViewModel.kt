package com.ionica.iontv.navigation

import androidx.lifecycle.ViewModel
import com.ionica.iontv.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class IonTvRootViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {

    val sessionEnded: Flow<Unit> = sessionStore.session
        .map { it != null }
        .distinctUntilChanged()
        .drop(1)
        .filter { linked -> !linked }
        .map { }
}
