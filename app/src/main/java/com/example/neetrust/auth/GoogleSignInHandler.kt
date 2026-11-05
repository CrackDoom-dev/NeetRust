// [KATALOG] shared.auth.GoogleSignInHandler
package com.example.neetrust.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.neetrust.core.logger.AppLogger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInHandler(private val context: Context) {
    private val logger = AppLogger.of("GoogleSignInHandler")
    private val credentialManager = CredentialManager.create(context)

    suspend fun requestGoogleIdToken(serverClientId: String): Result<String> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleIdTokenCredential.idToken)
            } else {
                logger.w("Credential is not GoogleIdTokenCredential: ${credential.type}")
                Result.failure(Exception("Tipe kredensial tidak sesuai"))
            }
        } catch (e: GetCredentialException) {
            logger.e("Credential Manager error", e)
            Result.failure(e)
        } catch (e: Exception) {
            logger.e("Unexpected error during Google Sign In", e)
            Result.failure(e)
        }
    }
}
