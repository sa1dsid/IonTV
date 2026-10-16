package com.ionica.iontv.feature.dashboard.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.delay

// Сколько держать OK, чтобы открылась отвязка: дольше системного long press,
// чтобы случайное нажатие на пульте не роняло попап.
private const val LONG_PRESS_MILLIS = 800L

// ТЗ: «после ~1 минуты бездействия экран гаснет» — защита OLED от выгорания.
internal const val AMBIENT_AFTER_MILLIS = 60_000L

internal const val DASHBOARD_TAG = "dashboard"

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
    val remote = remember { RemoteKeys(LONG_PRESS_MILLIS) }
    var ambient by remember { mutableStateOf(false) }
    var lastInput by remember { mutableIntStateOf(0) }

    // Минута без пульта — экран гаснет до строки статуса. Попап отвязки ждёт ответа
    // и в ambient не уходит.
    LaunchedEffect(lastInput, uiState.confirmingUnlink) {
        if (uiState.confirmingUnlink) return@LaunchedEffect
        delay(AMBIENT_AFTER_MILLIS)
        ambient = true
    }

    // На ACTIVE и OFFLINE кнопок нет, а в ambient их нет нигде, но пульт должен
    // попадать в экран — иначе не поймать долгое OK и не разбудить дашборд. Когда кнопка
    // есть, сам экран фокус не берёт: «Назад» с кнопки ушёл бы на него, а не из приложения.
    val buttonOnScreen = !ambient && uiState.status.hasSettingsButton()
    LaunchedEffect(uiState.confirmingUnlink, buttonOnScreen) {
        if (!uiState.confirmingUnlink && !buttonOnScreen) rootFocus.requestFocus()
    }

    // С Android 16 «Назад» приходит не клавишей, а системным жестом — будим и им.
    BackHandler(enabled = ambient) { ambient = false }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .onPreviewKeyEvent { event ->
                lastInput++
                if (remote.swallowsTail(event.nativeKeyEvent)) return@onPreviewKeyEvent true
                // Любая кнопка будит дашборд, но сама никуда дальше не нажимается.
                if (ambient) {
                    if (event.type == KeyEventType.KeyDown) {
                        ambient = false
                        remote.swallowRestOf(event.nativeKeyEvent)
                    }
                    return@onPreviewKeyEvent true
                }
                if (uiState.confirmingUnlink) return@onPreviewKeyEvent false
                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!isOk || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                remote.onOkDown(event.nativeKeyEvent, onLongPressOk)
            }
            .focusRequester(rootFocus)
            .focusable(enabled = !buttonOnScreen)
            .testTag(DASHBOARD_TAG),
    ) {
        if (ambient) {
            AmbientScreen(status = uiState.status)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (uiState.confirmingUnlink) POPUP_BACKDROP_ALPHA else 1f),
            ) {
                DashboardHeader(uiState)
                StatusBlock(
                    status = uiState.status,
                    popupOpen = uiState.confirmingUnlink,
                    onOpenSettingsClick = onOpenSettingsClick,
                    settingsInteraction = settingsInteraction,
                    modifier = Modifier.weight(1f),
                )
                DashboardFooter(uiState.status)
            }
        }

        if (uiState.confirmingUnlink) {
            TvPopup(
                title = stringResource(R.string.dashboard_unlink_title),
                message = stringResource(R.string.dashboard_unlink_text),
                safeActionText = stringResource(R.string.dashboard_keep_linked),
                onSafeAction = onKeepLinkedClick,
                riskyActionText = stringResource(R.string.dashboard_unlink),
                onRiskyAction = onUnlinkClick,
                busy = uiState.unlinking,
                safeActionInteraction = popupInteraction,
            )
        }
    }
}

@Composable
private fun DashboardHeader(uiState: DashboardUiState) {
    val colors = IonTvTheme.colors
    // ТВ привязывают по QR на сайте, и имени аккаунта бэкенд телевизору не отдаёт.
    val account = stringResource(R.string.dashboard_your_account)
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
    popupOpen: Boolean,
    onOpenSettingsClick: () -> Unit,
    settingsInteraction: MutableInteractionSource?,
    modifier: Modifier = Modifier,
) {
    val colors = IonTvTheme.colors
    val typography = IonTvTheme.typography
    val active = status == SharingStatus.Active
    val accent = if (active) colors.success else colors.danger
    val settingsFocus = remember { FocusRequester() }

    // И после закрытия попапа: его кнопки уходят вместе с фокусом.
    LaunchedEffect(status, popupOpen) {
        if (status.hasSettingsButton() && !popupOpen) settingsFocus.requestFocus()
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
 * Пульт на дашборде. Долгое OK считаем по времени удержания: пульт присылает повторы
 * нажатия, и первый повтор после порога открывает отвязку.
 *
 * Нажатие, которое уже сработало (открыло отвязку или разбудило экран), доедаем
 * до отпускания: Compose нажимает кнопку в фокусе на отпускание OK, и без этого
 * отпускание сразу нажало бы «Keep linked» в попапе или «Open settings».
 */
private class RemoteKeys(private val longPressMillis: Long) {

    private var swallowedKey: Int? = null

    fun swallowRestOf(event: android.view.KeyEvent) {
        swallowedKey = event.keyCode
    }

    fun swallowsTail(event: android.view.KeyEvent): Boolean {
        if (event.keyCode != swallowedKey) return false
        // Новое нажатие той же кнопки: отпускание прошлого потерялось, не держим его.
        if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            swallowedKey = null
            return false
        }
        if (event.action == android.view.KeyEvent.ACTION_UP) swallowedKey = null
        return true
    }

    fun onOkDown(event: android.view.KeyEvent, onLongPress: () -> Unit): Boolean {
        if (event.repeatCount == 0) return false
        if (event.eventTime - event.downTime >= longPressMillis) {
            swallowRestOf(event)
            onLongPress()
        }
        return true
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DashboardActivePreview() {
    IonTvTheme {
        DashboardScreen(
            uiState = DashboardUiState(connection = Connection.ETHERNET),
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
            uiState = DashboardUiState(status = SharingStatus.Offline),
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
            uiState = DashboardUiState(connection = Connection.ETHERNET, confirmingUnlink = true),
            onOpenSettingsClick = {},
            onLongPressOk = {},
            onKeepLinkedClick = {},
            onUnlinkClick = {},
            popupInteraction = rememberFocusedInteraction(),
        )
    }
}
