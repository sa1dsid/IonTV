package com.ionica.iontv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.feature.consent.presentation.ConsentRoute
import com.ionica.iontv.feature.dashboard.presentation.DashboardRoute
import com.ionica.iontv.feature.setup.presentation.SetupRoute
import com.ionica.iontv.feature.signin.presentation.SignInRoute
import com.ionica.iontv.feature.splash.domain.StartDestination
import com.ionica.iontv.feature.splash.presentation.SplashRoute

private object Routes {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val CONSENT = "consent"
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
}

// Экраны, где сессии ещё нет: конец сессии их не касается.
private val SIGNED_OUT_ROUTES = setOf(Routes.SPLASH, Routes.SIGN_IN)

/**
 * Поток экранов: сплэш → привязка по QR → согласие → чеклист → дашборд.
 * Шаги вперёд не оставляют истории: «Назад» с дашборда выходит из приложения, а не
 * возвращает к входу, — раздача при этом продолжается в сервисе.
 */
@Composable
fun IonTvRoot(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IonTvRootViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        viewModel.sessionEnded.collect {
            if (navController.currentDestination?.route !in SIGNED_OUT_ROUTES) {
                navController.startOver(Routes.SIGN_IN)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
            .fillMaxSize()
            .background(IonTvTheme.colors.background),
    ) {
        composable(Routes.SPLASH) {
            SplashRoute(onFinished = { destination -> navController.startOver(destination.route()) })
        }
        composable(Routes.SIGN_IN) {
            SignInRoute(onLinked = { navController.startOver(Routes.CONSENT) })
        }
        composable(Routes.CONSENT) {
            ConsentRoute(
                onAgreed = { setupPending ->
                    navController.startOver(if (setupPending) Routes.SETUP else Routes.DASHBOARD)
                },
                onExit = onExit,
            )
        }
        composable(Routes.SETUP) {
            SetupRoute(onFinished = { navController.startOver(Routes.DASHBOARD) })
        }
        composable(Routes.DASHBOARD) {
            DashboardRoute(
                onUnlinked = { navController.startOver(Routes.SIGN_IN) },
                onConsentRequired = { navController.startOver(Routes.CONSENT) },
            )
        }
    }
}

private fun StartDestination.route(): String = when (this) {
    StartDestination.SIGN_IN -> Routes.SIGN_IN
    StartDestination.CONSENT -> Routes.CONSENT
    StartDestination.SETUP -> Routes.SETUP
    StartDestination.DASHBOARD -> Routes.DASHBOARD
}

/** Переход без истории: предыдущие экраны и их ViewModel закрываются. */
private fun NavController.startOver(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
