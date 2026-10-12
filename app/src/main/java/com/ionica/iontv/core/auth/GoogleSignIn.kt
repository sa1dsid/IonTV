package com.ionica.iontv.core.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ionica.iontv.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface GoogleSignIn {

    /** На Fire TV и безымянных боксах без Google Play services входа через Google нет. */
    fun isAvailable(): Boolean

    /** ID-токен выбранного аккаунта. Окно выбора аккаунта системное, поверх приложения. */
    suspend fun idToken(activityContext: Context): String
}

class GoogleSignInException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { CANCELLED, UNAVAILABLE }
}

@Singleton
class CredentialManagerGoogleSignIn @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager,
) : GoogleSignIn {

    override fun isAvailable(): Boolean {
        if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) return false
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return status == ConnectionResult.SUCCESS
    }

    // Сначала аккаунты, уже входившие в приложение, — тогда окно в один клик. Если таких
    // нет, показываем все аккаунты телевизора.
    override suspend fun idToken(activityContext: Context): String {
        val credential = credential(activityContext, filterByAuthorizedAccounts = true)
            ?: credential(activityContext, filterByAuthorizedAccounts = false)
            ?: throw GoogleSignInException(GoogleSignInException.Reason.UNAVAILABLE)

        val custom = credential as? CustomCredential
        if (custom?.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleSignInException(GoogleSignInException.Reason.UNAVAILABLE)
        }
        return GoogleIdTokenCredential.createFrom(custom.data).idToken
    }

    private suspend fun credential(
        activityContext: Context,
        filterByAuthorizedAccounts: Boolean,
    ): Credential? = try {
        credentialManager.getCredential(
            context = activityContext,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .build()
                )
                .build(),
        ).credential
    } catch (cancellation: GetCredentialCancellationException) {
        throw GoogleSignInException(GoogleSignInException.Reason.CANCELLED)
    } catch (failure: GetCredentialException) {
        null
    }
}
