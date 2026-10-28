package com.ionica.iontv.feature.dashboard.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.feature.consent.data.ConsentRepository
import com.ionica.iontv.feature.sharing.proxy.ProxyClient
import com.ionica.iontv.feature.sharing.service.SharingController
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UnlinkRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
    private val consent: ConsentRepository,
    private val sharingController: SharingController,
    private val proxy: ProxyClient,
) {

    suspend fun unlink() = withContext(NonCancellable) {
        proxy.disconnect()
        sharingController.stop()
        consent.withdraw()
        runCatchingCancellable { api.logout() }
        sessionStore.close()
    }
}
