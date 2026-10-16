package com.ionica.iontv.feature.dashboard.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.feature.consent.data.ConsentRepository
import com.ionica.iontv.feature.sharing.service.SharingController
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Отвязка с самого телевизора. DELETE /devices бэкенд принимает только от аккаунта,
 * поэтому ТВ отзывает согласие и свою TV-сессию, а карточка в кабинете остаётся,
 * пока её не уберут на сайте IonTV Pulse. Телевизор получает новый device_id и
 * привязывается заново как новое устройство.
 */
class UnlinkRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
    private val consent: ConsentRepository,
    private val sharingController: SharingController,
) {

    // По ТЗ раздача прекращается сразу, а без сети отвязка всё равно должна случиться:
    // поэтому запросы — без проверки ответа, а сессия закрывается в любом случае.
    suspend fun unlink() = withContext(NonCancellable) {
        sharingController.stop()
        consent.withdraw()
        runCatchingCancellable { api.logout() }
        sessionStore.close()
    }
}
