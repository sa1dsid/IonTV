package com.ionica.iontv.feature.signin.domain.model

import kotlin.time.Duration

data class PairingCode(
    val pairId: String,
    val code: String,
    val qrContent: String,
    val pollToken: String,
    val lifetime: Duration,
    val pollInterval: Duration,
)

sealed interface PairingPoll {
    data object Pending : PairingPoll

    data object Linked : PairingPoll

    data object Expired : PairingPoll
}
