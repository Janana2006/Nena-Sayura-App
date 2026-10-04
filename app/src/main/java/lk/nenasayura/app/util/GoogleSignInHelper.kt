package lk.nenasayura.app.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import lk.nenasayura.app.R

object GoogleSignInHelper {

    suspend fun getIdToken(context: Context): Result<String> {
        return try {
            val option = GetSignInWithGoogleOption.Builder(
                context.getString(R.string.web_client_id)
            ).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val result = CredentialManager.create(context).getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            // User chooser eka close kala, ho setup eke prashnayak
            Log.e("GoogleSignIn", "Cancelled", e)
            Result.failure(Exception("Google sign-in was cancelled"))
        } catch (e: NoCredentialException) {
            // Phone eke Google account ekak naha
            Log.e("GoogleSignIn", "No credential", e)
            Result.failure(Exception("No Google account found on this phone. Add one in Settings and try again."))
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Failed", e)
            Result.failure(e)
        }
    }
}