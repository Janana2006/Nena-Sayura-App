package lk.nenasayura.app.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import lk.nenasayura.app.R

object GoogleSignInHelper {

    private const val TAG = "GoogleSignIn"

    suspend fun getIdToken(context: Context): Result<String> {
        val webClientId = context.getString(R.string.web_client_id)
        val manager = CredentialManager.create(context)

        // STEP 1: Phone eke thiyena Google accounts list eka penawa (account chooser)
        val chooserRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)   // okkoma accounts pennanna
                    .setAutoSelectEnabled(false)            // thaniyama account ekak thiyenawath auto select karanna epa
                    .build()
            )
            .build()

        val first = requestToken(manager, context, chooserRequest)
        val firstError = first.exceptionOrNull() ?: return first

        // STEP 2: Phone eke Google account ekak naththam full "Sign in with Google" flow eka
        // (eke "Add account" karanna puluwan)
        if (firstError is NoCredentialException) {
            Log.d(TAG, "No accounts on device, opening full sign-in flow")
            val fullRequest = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
                .build()

            val second = requestToken(manager, context, fullRequest)
            val secondError = second.exceptionOrNull() ?: return second
            return Result.failure(friendly(secondError))
        }

        return Result.failure(friendly(firstError))
    }

    private suspend fun requestToken(
        manager: CredentialManager,
        context: Context,
        request: GetCredentialRequest
    ): Result<String> {
        return try {
            val credential = manager.getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "getCredential failed: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Token parsing failed", e)
            Result.failure(e)
        }
    }

    // User ta pennanna message eka
    private fun friendly(e: Throwable): Exception = when (e) {
        is GetCredentialCancellationException -> Exception("Google sign-in was cancelled")
        is NoCredentialException -> Exception("No Google account found. Please add one and try again.")
        else -> Exception(e.localizedMessage ?: "Google sign-in failed")
    }
}