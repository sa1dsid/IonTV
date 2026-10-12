package com.ionica.iontv.feature.signin.data

import com.ionica.iontv.core.api.BAD_REQUEST
import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.TOO_MANY_REQUESTS
import com.ionica.iontv.core.api.UNAUTHORIZED
import com.ionica.iontv.core.api.UNPROCESSABLE
import com.ionica.iontv.core.api.apiError
import com.ionica.iontv.core.api.bearer
import com.ionica.iontv.core.api.dto.AccountLoginDto
import com.ionica.iontv.core.api.dto.DeviceDto
import com.ionica.iontv.core.api.dto.EmailInputDto
import com.ionica.iontv.core.api.dto.EmailLoginDto
import com.ionica.iontv.core.api.dto.GoogleLoginDto
import com.ionica.iontv.core.api.dto.toTokens
import com.ionica.iontv.core.auth.GoogleSignInException
import com.ionica.iontv.core.common.mapFailure
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.core.device.DeviceName
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.feature.signin.domain.model.LinkException
import com.ionica.iontv.feature.signin.domain.model.LinkFailure
import kotlinx.coroutines.CancellationException
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
        link(LoginKind.GOOGLE) { api.googleLogin(GoogleLoginDto(idToken = idToken())) }

    /** Шлёт код на почту. В ответе — через сколько секунд можно попросить новый. */
    suspend fun requestEmailCode(email: String): Result<Int> = runCatchingCancellable {
        api.emailCode(EmailInputDto(email)).resendAfterSeconds
    }.mapFailure { LinkException(it.toEmailCodeFailure()) }

    /** Новому адресу бэкенд сам заводит аккаунт: отдельной регистрации на ТВ нет. */
    suspend fun linkWithEmailCode(email: String, code: String): Result<Unit> =
        link(LoginKind.EMAIL) { api.emailLogin(EmailLoginDto(email = email, code = code)) }

    private suspend fun link(
        kind: LoginKind,
        login: suspend () -> AccountLoginDto,
    ): Result<Unit> = runCatchingCancellable {
        val account = try {
            login()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            throw LinkException(error.toLoginFailure(kind))
        }

        val authorization = bearer(account.accessToken)
        try {
            val device = DeviceDto(sessionStore.deviceId(), deviceName.get())
            val tokens = api.registerDevice(authorization, device).toTokens()
            sessionStore.open(tokens, username = account.user.username)
        } finally {
            revoke(authorization)
        }
    }.mapFailure { error ->
        error as? LinkException ?: LinkException(error.toConnectionFailure())
    }

    // Отзыв — уборка: не вышло, и аккаунтный токен сам истечёт.
    private suspend fun revoke(authorization: String) = withContext(NonCancellable) {
        runCatchingCancellable { api.logout(authorization) }
    }

    private enum class LoginKind { GOOGLE, EMAIL }

    private fun Throwable.toLoginFailure(kind: LoginKind): LinkFailure {
        if (this is GoogleSignInException) {
            return when (reason) {
                GoogleSignInException.Reason.CANCELLED -> LinkFailure.GOOGLE_CANCELLED
                GoogleSignInException.Reason.UNAVAILABLE -> LinkFailure.GOOGLE_UNAVAILABLE
            }
        }
        val status = apiError()?.status ?: return LinkFailure.NO_CONNECTION

        return when (kind) {
            // Бэкенд не принял ID-токен: для пользователя это тот же недоступный Google.
            LoginKind.GOOGLE -> if (status == UNAUTHORIZED) LinkFailure.GOOGLE_UNAVAILABLE else LinkFailure.SERVER
            // 400 — неверный код, 429 — код сгорел после пяти попыток: в обоих случаях нужен новый.
            LoginKind.EMAIL -> when (status) {
                BAD_REQUEST, UNPROCESSABLE, TOO_MANY_REQUESTS -> LinkFailure.CODE_INVALID
                else -> LinkFailure.SERVER
            }
        }
    }
}

private fun Throwable.toEmailCodeFailure(): LinkFailure = when (apiError()?.status) {
    null -> LinkFailure.NO_CONNECTION
    UNPROCESSABLE -> LinkFailure.EMAIL_INVALID
    TOO_MANY_REQUESTS -> LinkFailure.RESEND_TOO_SOON
    else -> LinkFailure.SERVER
}

private fun Throwable.toConnectionFailure(): LinkFailure =
    if (apiError() == null) LinkFailure.NO_CONNECTION else LinkFailure.SERVER
