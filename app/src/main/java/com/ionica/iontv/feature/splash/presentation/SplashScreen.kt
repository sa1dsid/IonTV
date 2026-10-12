package com.ionica.iontv.feature.splash.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.IonTvLogo
import com.ionica.iontv.core.designsystem.component.TvSpinner
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.feature.splash.domain.StartDestination

@Composable
fun SplashRoute(
    onFinished: (StartDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let(onFinished)
    }

    SplashScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun SplashScreen(
    uiState: SplashUiState,
    modifier: Modifier = Modifier,
) {
    val colors = IonTvTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IonTvLogo()
        Spacer(Modifier.height(30.dp))
        TvSpinner()
        if (uiState.offline) {
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.no_connection_retrying),
                style = IonTvTheme.typography.notice,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SplashScreenPreview() {
    IonTvTheme {
        SplashScreen(uiState = SplashUiState())
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SplashScreenOfflinePreview() {
    IonTvTheme {
        SplashScreen(uiState = SplashUiState(offline = true))
    }
}
