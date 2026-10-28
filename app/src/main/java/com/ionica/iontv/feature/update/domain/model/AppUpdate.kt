package com.ionica.iontv.feature.update.domain.model

data class AppUpdate(
    val versionName: String,
    val installationUrl: String,
    val downloadUrl: String,
)
