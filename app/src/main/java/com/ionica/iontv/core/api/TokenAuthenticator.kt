package com.ionica.iontv.core.api

import com.ionica.iontv.core.api.dto.TokenInputDto
import com.ionica.iontv.core.api.dto.toTokens
import com.ionica.iontv.core.session.SessionStore
import com.ionica.iontv.core.session.TvSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

// Ответы, после которых refresh-токен уже не оживить: сессию отозвали, ТВ отвязали
// или токен истёк. 429, 5xx и обрыв сети сессию не закрывают — это временно.
private val REJECTED = setOf(BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, GONE, UNPROCESSABLE)

/**
 * Обновляет TV-сессию по 401 и повторяет запрос с новым токеном. Если бэкенд
 * отклонил refresh-токен, сессия закрывается — телевизор уходит на экран входа.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    @param:Unauthenticated private val api: IonTvApi,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val signed = response.request.tag(TvSessionAuth::class.java) ?: return null
        if (response.priorResponse != null) return null

        synchronized(lock) {
            val current = sessionStore.blockingCurrent() ?: return null

            // Пока запрос ждал блокировку, токен мог обновить соседний запрос.
            if (current.accessToken != signed.accessToken) {
                return response.request.signedWith(current.accessToken)
            }

            return when (val outcome = runBlocking { refresh(current) }) {
                is Refresh.Done -> response.request.signedWith(outcome.accessToken)
                Refresh.Rejected -> {
                    runBlocking { sessionStore.close() }
                    null
                }

                Refresh.Unavailable -> null
            }
        }
    }

    private suspend fun refresh(session: TvSession): Refresh = try {
        val tokens = api.refresh(TokenInputDto(session.refreshToken)).toTokens()
        sessionStore.replaceTokens(tokens)
        Refresh.Done(tokens.accessToken)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        if (error.apiError()?.status in REJECTED) Refresh.Rejected else Refresh.Unavailable
    }

    private sealed interface Refresh {
        data class Done(val accessToken: String) : Refresh
        data object Rejected : Refresh
        data object Unavailable : Refresh
    }
}
