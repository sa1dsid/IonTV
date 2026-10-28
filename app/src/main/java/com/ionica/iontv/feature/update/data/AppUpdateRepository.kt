package com.ionica.iontv.feature.update.data

import com.ionica.iontv.core.api.IonTvApi
import com.ionica.iontv.core.api.Unauthenticated
import com.ionica.iontv.core.api.dto.AndroidAppDto
import com.ionica.iontv.core.common.runCatchingCancellable
import com.ionica.iontv.feature.update.InstalledVersion
import com.ionica.iontv.feature.update.domain.isNewerVersion
import com.ionica.iontv.feature.update.domain.model.AppUpdate
import javax.inject.Inject

class AppUpdateRepository @Inject constructor(
    @param:Unauthenticated private val api: IonTvApi,
    @param:InstalledVersion private val installedVersion: String,
) {

    suspend fun check(): Result<AppUpdate?> =
        runCatchingCancellable { api.config().androidApp?.toUpdate() }

    private fun AndroidAppDto.toUpdate(): AppUpdate? {
        val version = latestVersion?.trim()?.takeIf { isNewerVersion(it, installedVersion) } ?: return null
        val installation = installationUrl?.takeIf { it.isNotBlank() } ?: return null
        val download = downloadUrl?.takeIf { it.isNotBlank() } ?: return null
        return AppUpdate(versionName = version, installationUrl = installation, downloadUrl = download)
    }
}
