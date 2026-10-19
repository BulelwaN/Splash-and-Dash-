
package com.example.splashanddash

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

object GoogleSignInManager {

    private const val TAG = "SplashDashGoogle"

    suspend fun signIn(
        activity: Activity,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val googleOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(
                    activity.getString(R.string.default_web_client_id)
                )
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val credentialManager =
                CredentialManager.create(activity)

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential

            if (
                credential !is CustomCredential ||
                credential.type !=
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                onResult(false, "Unsupported Google credential.")
                return
            }

            val googleCredential =
                GoogleIdTokenCredential.createFrom(
                    credential.data
                )

            val firebaseCredential =
                GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )

            FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .addOnSuccessListener { authResult ->

                    val user = authResult.user

                    if (user == null) {
                        onResult(
                            false,
                            "Google authentication failed."
                        )
                        return@addOnSuccessListener
                    }

                    Log.d(
                        TAG,
                        "Google authentication successful"
                    )

                    loadOrCreateProfile(user, onResult)
                }
                .addOnFailureListener { exception ->

                    Log.e(
                        TAG,
                        "Firebase Google authentication failed",
                        exception
                    )

                    onResult(
                        false,
                        exception.localizedMessage
                            ?: "Google login failed."
                    )
                }

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Google credential selection failed",
                exception
            )

            onResult(
                false,
                exception.localizedMessage
                    ?: "Google Sign-In was cancelled or failed."
            )
        }
    }

    private fun loadOrCreateProfile(
        user: FirebaseUser,
        onResult: (Boolean, String) -> Unit
    ) {
        val userReference = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)

        userReference.get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    Log.d(
                        TAG,
                        "Existing Firestore profile found"
                    )

                    onResult(
                        true,
                        "Google login successful."
                    )

                } else {

                    Log.d(
                        TAG,
                        "Creating new Google customer profile"
                    )

                    val profile = hashMapOf(
                        "uid" to user.uid,
                        "name" to (
                                user.displayName ?: "Customer"
                                ),
                        "email" to (
                                user.email ?: ""
                                ),
                        "phone" to "",
                        "address" to "",
                        "role" to "Customer"
                    )

                    userReference.set(profile)
                        .addOnSuccessListener {

                            Log.d(
                                TAG,
                                "Google customer profile created"
                            )

                            onResult(
                                true,
                                "Google account connected."
                            )
                        }
                        .addOnFailureListener { exception ->

                            reportFirestoreError(
                                "Saving customer profile",
                                exception,
                                onResult
                            )
                        }
                }
            }
            .addOnFailureListener { exception ->

                reportFirestoreError(
                    "Loading customer profile",
                    exception,
                    onResult
                )
            }
    }

    private fun reportFirestoreError(
        operation: String,
        exception: Exception,
        onResult: (Boolean, String) -> Unit
    ) {
        Log.e(
            TAG,
            "$operation failed",
            exception
        )

        val message = when (
            (exception as? FirebaseFirestoreException)?.code
        ) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firestore permission denied. " +
                        "Check your database security rules."

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore is unavailable. " +
                        "Check your internet connection."

            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Firestore could not verify your session."

            else ->
                exception.localizedMessage
                    ?: "Unable to access your customer profile."
        }

        onResult(false, message)
    }
}

