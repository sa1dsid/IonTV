package com.ionica.iontv.core.api

import com.ionica.iontv.core.common.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendHealth @Inject constructor(
    private val api: IonTvApi,
) {

    /** Бэкенд отвечает и IONTV на нём включён: health отвечает даже при выключенном IONTV. */
    suspend fun isServing(): Boolean = runCatchingCancellable { api.health().enabled }.getOrDefault(false)
}
