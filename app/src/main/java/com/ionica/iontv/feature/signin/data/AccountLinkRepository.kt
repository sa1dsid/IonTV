package com.ionica.iontv.feature.signin.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.UNAUTHORIZED
import com.ionica.iontv.core.api.apiError
import com.ionica.iontv.core.api.bearer
import com.ionica.iontv.core.api.dto.AccountLoginDto
import com.ionica.iontv.core.api.dto.DeviceDto
import com.ionica.iontv.core.api.dto.GoogleLoginDto
import com.ionica.iontv.core.api.dto.toTokens
import com.ionica.iontv.core.auth.GoogleSignInException
import com.ionica.iontv.core.common.mapFailure
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.device.DeviceName
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.feature.signin.domain.model.LinkException
import com.ionica.iontv.feature.signin.domain.model.LinkFailure
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Вход аккаунтом прямо на телевизоре. Аккаунтная сессия тут временная: ею ТВ
 * регистрирует себя и получает ограниченную TV-сессию, а сам аккаунтный токен
 * сразу отзывается — на телевизоре он не хранится.
 */
class AccountLinkRepository @Inject constructor(
    private val api: IonTvApi,
    private val sessionStore: SessionStore,
    private val deviceName: DeviceName,
) {

    /** [idToken] открывает системное окно выбора аккаунта: его отмена — тоже исход входа. */
    suspend fun linkWithGoogle(idToken: suspend () -> String): Result<Unit> =
        link(googleLogin = true) { api.googleLogin(GoogleLoginDto(idToken = idToken())) }

    private suspend fun link(
        googleLogin: Boolean = false,
        login: suspend () -> AccountLoginDto,
    ): Result<Unit> = runCatchingCancellable {
        val account = login()
        val authorization = bearer(account.accessToken)
        try {
            val device = DeviceDto(sessionStore.deviceId(), deviceName.get())
            val tokens = api.registerDevice(authorization, device).toTokens()
            sessionStore.open(tokens, username = account.user.username)
        } finally {
            revoke(authorization)
        }
    }.mapFailure { LinkException(it.toLinkFailure(googleLogin)) }

    // Отзыв — уборка: не вышло, и аккаунтный токен сам истечёт.
    private suspend fun revoke(authorization: String) = withContext(NonCancellable) {
        runCatchingCancellable { api.logout(authorization) }
    }
}

private fun Throwable.toLinkFailure(googleLogin: Boolean): LinkFailure {
    if (this is LinkException) return failure
    if (this is GoogleSignInException) {
        return when (reason) {
            GoogleSignInException.Reason.CANCELLED -> LinkFailure.GOOGLE_CANCELLED
            GoogleSignInException.Reason.UNAVAILABLE -> LinkFailure.GOOGLE_UNAVAILABLE
        }
    }
    val error = apiError() ?: return LinkFailure.NO_CONNECTION

    // 401 на auth/google — бэкенд не принял ID-токен: для пользователя это тот же
    // недоступный Google, а не сбой сервера.
    return if (googleLogin && error.status == UNAUTHORIZED) {
        LinkFailure.GOOGLE_UNAVAILABLE
    } else {
        LinkFailure.SERVER
    }
}
