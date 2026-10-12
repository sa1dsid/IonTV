package com.ionica.iontv.testing

import com.ionica.iontv.core.api.RoutingApiServer
import com.ionica.iontv.core.api.TestApiStack
import com.ionica.iontv.core.common.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.rules.TemporaryFolder

private const val POLL_MILLIS = 2L

private const val AWAIT_MILLIS = 5_000L

/**
 * Основа тестов экранов: настоящий сетевой стек против MockWebServer, настоящий
 * DataStore во временной папке и виртуальное время для задержек ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestCase {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun closeStores() {
        storeScope.cancel()
    }

    internal fun screen(body: suspend TestScope.(TestApiStack) -> Unit) =
        runTest(mainDispatcher.dispatcher) {
            val server = RoutingApiServer()
            val stack = TestApiStack(
                server = server,
                sessionStore = sessionStoreIn(folder.root, storeScope),
                timeSource = TimeSource { testScheduler.currentTime },
            )
            try {
                // Прогреваем кэш хранилища: дальше ViewModel читает его без настоящего I/O,
                // и виртуальное время не обгоняет диск.
                stack.sessionStore.current()
                body(stack)
            } finally {
                server.shutdown()
            }
        }

    internal fun preferences(name: String) = preferencesIn(folder.root, name, storeScope)

    /**
     * Ждёт условия, не двигая виртуальное время. Ошибку запроса Retrofit возвращает
     * в корутину через Dispatchers.Default, поэтому её приходится ждать по-настоящему.
     */
    protected fun TestScope.settle(what: String, condition: () -> Boolean) =
        awaitReal(what, condition) { runCurrent() }

    /** То же, но с прокруткой виртуального времени: задержки и повторы проходят сразу. */
    protected fun TestScope.settleIdle(what: String, condition: () -> Boolean) =
        awaitReal(what, condition) { advanceUntilIdle() }

    private fun awaitReal(what: String, condition: () -> Boolean, step: () -> Unit) {
        val deadline = System.currentTimeMillis() + AWAIT_MILLIS
        while (true) {
            step()
            if (condition()) return
            if (System.currentTimeMillis() > deadline) fail("timed out waiting for $what")
            Thread.sleep(POLL_MILLIS)
        }
    }
}
