package com.ionica.iontv.feature.dashboard.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionica.iontv.BuildConfig
import com.ionica.iontv.R
import com.ionica.iontv.core.designsystem.component.IonTvLogo
import com.ionica.iontv.core.designsystem.component.POPUP_BACKDROP_ALPHA
import com.ionica.iontv.core.designsystem.component.TvButton
import com.ionica.iontv.core.designsystem.component.TvPopup
import com.ionica.iontv.core.designsystem.component.rememberFocusedInteraction
import com.ionica.iontv.core.designsystem.theme.IonTvTheme
import com.ionica.iontv.core.network.Connection
import com.ionica.iontv.feature.sharing.domain.model.PauseReason
import com.ionica.iontv.feature.sharing.domain.model.SharingStatus

// Сколько держать OK, чтобы открылась отвязка: дольше системного long press,
// чтобы случайное нажатие на пульте не роняло попап.
private const val LONG_PRESS_MILLIS = 800L

private val StatusDotSize = 16.dp

private val StatusDotGlow = 17.dp

@Composable
fun DashboardRoute(
    onUnlinked: () -> Unit,
    onConsentRequired: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DashboardEvent.Unlinked -> onUnlinked()
                DashboardEvent.ConsentRequired -> onConsentRequired()
            }
        }
    }

    DashboardScreen(
        uiState = uiState,
        onOpenSettingsClick = viewModel::onOpenSettingsClick,
        onLongPressOk = viewModel::onLongPressOk,
        onKeepLinkedClick = viewModel::onKeepLinkedClick,
        onUnlinkClick = viewModel::onUnlinkClick,
        modifier = modifier,
    )
}

/** Кадры 5a–5c и попап 3c. Статистика и настройки — в IO Aura на телефоне, здесь только статус. */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onOpenSettingsClick: () -> Unit,
    onLongPressOk: () -> Unit,
    onKeepLinkedClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsInteraction: MutableInteractionSource? = null,
    popupInteraction: MutableInteractionSource? = null,
) {
    val colors = IonTvTheme.colors
    val rootFocus = remember { FocusRequester() }
    val longPress = remember { LongPressTracker(LONG_PRESS_MILLIS) }

    // На ACTIVE и OFFLINE кнопок нет, но пульт должен попадать в экран — иначе не
    // поймать долгое OK. Фокус держит сам экран.
    LaunchedEffect(uiState.confirmingUnlink, uiState.status) {
        if (!uiState.confirmingUnlink && !uiState.status.hasSettingsButton()) rootFocus.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .onPreviewKeyEvent { event ->
                if (uiState.confirmingUnlink) return@onPreviewKeyEvent false
                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!isOk) return@onPreviewKeyEvent false

                when (event.type) {
                    KeyEventType.KeyDown -> longPress.onDown(event.nativeKeyEvent, onLongPressOk)
                    KeyEventType.KeyUp -> longPress.onUp()
                    else -> false
                }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (uiState.confirmingUnlink) POPUP_BACKDROP_ALPHA else 1f),
        ) {
            DashboardHeader(uiState)
            StatusBlock(
                status = uiState.status,
                onOpenSettingsClick = onOpenSettingsClick,
                settingsInteraction = settingsInteraction,
                modifier = Modifier.weight(1f),
            )
            DashboardFooter(uiState.status)
        }

        if (uiState.confirmingUnlink) {
            TvPopup(
                title = stringResource(R.string.dashboard_unlink_title),
                message = stringResource(R.string.dashboard_unlink_text),
                safeActionText = stringResource(R.string.dashboard_keep_linked),
                onSafeAction = onKeepLinkedClick,
                riskyActionText = stringResource(R.string.dashboard_unlink),
                onRiskyAction = onUnlinkClick,
                safeActionInteraction = popupInteraction,
            )
        }
    }
}

@Composable
private fun DashboardHeader(uiState: DashboardUiState) {
    val colors = IonTvTheme.colors
    val account = uiState.username?.let { "@$it" } ?: stringResource(R.string.dashboard_your_account)
    val connection = uiState.connection.label()?.takeIf { uiState.status != SharingStatus.Offline }
    val linkedTo = stringResource(R.string.dashboard_linked_to)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 49.dp, top = 30.dp, end = 49.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IonTvLogo(fontSize = 22.sp)
        Text(
            text = buildAnnotatedString {
                append(linkedTo)
                append(" ")
                withStyle(SpanStyle(color = colors.textEmphasis, fontWeight = FontWeight.Medium)) {
                    append(account)
                }
                connection?.let { append(" · $it") }
            },
            style = IonTvTheme.typography.statusMeta,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun StatusBlock(
    status: SharingStatus,
    onOpenSettingsClick: () -> Unit,
    settingsInteraction: MutableInteractionSource?,
    modifier: Modifier = Modifier,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val active = status == SharingStatus.Active
    val accent = if (active) colors.success else colors.danger
    val settingsFocus = remember { FocusRequester() }

    LaunchedEffect(status) {
        if (status.hasSettingsButton()) settingsFocus.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 49.dp, vertical = 37.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = accent)
            Spacer(Modifier.width(17.dp))
            Text(
                text = stringResource(status.headline()),
                style = typography.statusHeadline,
                color = if (active) colors.textPrimary else colors.danger,
            )
        }
        status.reason()?.let { reason ->
            Spacer(Modifier.height(17.dp))
            Text(text = stringResource(reason), style = typography.statusReason, color = colors.textSecondary)
        }
        if (status.hasSettingsButton()) {
            Spacer(Modifier.height(27.dp))
            TvButton(
                text = stringResource(R.string.dashboard_open_settings),
                onClick = onOpenSettingsClick,
                modifier = Modifier.focusRequester(settingsFocus),
                interactionSource = settingsInteraction,
            )
        }
    }
}

@Composable
private fun DashboardFooter(status: SharingStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 49.dp, end = 49.dp, bottom = 27.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val style = IonTvTheme.typography.footer
        val color = IonTvTheme.colors.textTertiary
        Text(text = stringResource(status.footer()), style = style, color = color)
        Text(text = stringResource(R.string.dashboard_version, BuildConfig.VERSION_NAME), style = style, color = color)
    }
}

/** Точка статуса с ореолом того же цвета, как box-shadow в макете. */
@Composable
private fun StatusDot(color: Color) {
    Canvas(modifier = Modifier.size(StatusDotSize)) {
        val glow = StatusDotGlow.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = size.minDimension / 2f + glow,
            ),
            radius = size.minDimension / 2f + glow,
        )
        drawCircle(color = color)
    }
}

private fun SharingStatus.hasSettingsButton(): Boolean =
    this == SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION)

@StringRes
private fun SharingStatus.headline(): Int = when (this) {
    SharingStatus.Active -> R.string.dashboard_active
    is SharingStatus.Paused -> R.string.dashboard_paused
    SharingStatus.Offline -> R.string.dashboard_offline
}

@StringRes
private fun SharingStatus.reason(): Int? = when (this) {
    SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION) -> R.string.dashboard_paused_battery
    SharingStatus.Offline -> R.string.error_no_connection
    else -> null
}

@StringRes
private fun SharingStatus.footer(): Int = when (this) {
    SharingStatus.Active -> R.string.dashboard_footer_active
    is SharingStatus.Paused -> R.string.dashboard_footer_paused
    SharingStatus.Offline -> R.string.dashboard_footer_offline
}

@Composable
private fun Connection.label(): String? = when (this) {
    Connection.ETHERNET -> stringResource(R.string.dashboard_ethernet)
    Connection.WIFI -> stringResource(R.string.dashboard_wifi)
    Connection.OTHER, Connection.NONE -> null
}

/**
 * Долгое OK по времени удержания: пульт присылает повторы нажатия, и первый повтор
 * после порога открывает отвязку. Отпускание после этого съедаем — иначе кнопка
 * в фокусе сработала бы следом.
 */
private class LongPressTracker(private val thresholdMillis: Long) {

    private var fired = false

    fun onDown(event: android.view.KeyEvent, onLongPress: () -> Unit): Boolean {
        if (event.repeatCount == 0) {
            fired = false
            return false
        }
        if (!fired && event.eventTime - event.downTime >= thresholdMillis) {
            fired = true
            onLongPress()
        }
        return true
    }

    fun onUp(): Boolean = fired.also { fired = false }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DashboardActivePreview() {
    IonTvTheme {
        DashboardScreen(
            uiState = DashboardUiState(username = "syrex", connection = Connection.ETHERNET),
            onOpenSettingsClick = {},
            onLongPressOk = {},
            onKeepLinkedClick = {},
            onUnlinkClick = {},
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DashboardPausedPreview() {
    IonTvTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                status = SharingStatus.Paused(PauseReason.BATTERY_OPTIMIZATION),
                username = "syrex",
                connection = Connection.WIFI,
            ),
            onOpenSettingsClick = {},
            onLongPressOk = {},
            onKeepLinkedClick = {},
            onUnlinkClick = {},
            settingsInteraction = rememberFocusedInteraction(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DashboardOfflinePreview() {
    IonTvTheme {
        DashboardScreen(
            uiState = DashboardUiState(status = SharingStatus.Offline, username = "syrex"),
            onOpenSettingsClick = {},
            onLongPressOk = {},
            onKeepLinkedClick = {},
            onUnlinkClick = {},
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DashboardLinkedByQrPreview() {
    IonTvTheme {
        DashboardScreen(
            uiState = DashboardUiState(connection = Connection.WIFI),
            onOpenSettingsClick = {},
            onLongPressOk = {},
            onKeepLinkedClick = {},
            onUnlinkClick = {},
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DashboardUnlinkPreview() {
    IonTvTheme {
        DashboardScreen(
            uiState = DashboardUiState(username = "syrex", connection = Connection.ETHERNET, confirmingUnlink = true),
            onOpenSettingsClick = {},
            onLongPressOk = {},
            onKeepLinkedClick = {},
            onUnlinkClick = {},
            popupInteraction = rememberFocusedInteraction(),
        )
    }
}
