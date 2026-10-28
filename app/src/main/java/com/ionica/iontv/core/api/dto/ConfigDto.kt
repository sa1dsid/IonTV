package com.ionica.iontv.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigDto(
    @SerialName("androidApp") val androidApp: AndroidAppDto? = null,
)

@Serializable
data class AndroidAppDto(
    @SerialName("latestVersion") val latestVersion: String? = null,
    @SerialName("installationUrl") val installationUrl: String? = null,
    @SerialName("downloadUrl") val downloadUrl: String? = null,
)
