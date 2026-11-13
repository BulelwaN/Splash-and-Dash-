package com.example.splashanddash

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

object FirebaseAccountManager {

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val database: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private const val USERS_COLLECTION = "users"

    private val allowedRoles = setOf(
        "Customer",
        "Staff",
        "Admin"
    )

    // REGISTER A CUSTOMER USING FIREBASE AUTHENTICATION
    fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        callback: (Boolean, String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanAddress = address.trim()

        if (
            cleanName.isBlank() ||
            cleanEmail.isBlank() ||
            password.isBlank() ||
            cleanPhone.isBlank() ||
            cleanAddress.isBlank()
        ) {
            callback(false, "Please complete all registration fields.")
            return
        }

        auth.createUserWithEmailAndPassword(
            cleanEmail,
            password
        ).addOnSuccessListener { result ->

            val user = result.user

            if (user == null) {
                callback(false, "Firebase could not create your account.")
                return@addOnSuccessListener
            }

            val profile = hashMapOf(
                "uid" to user.uid,
                "name" to cleanName,
                "email" to (user.email ?: cleanEmail),
                "phone" to cleanPhone,
                "address" to cleanAddress,
                "role" to "Customer"
            )

            database.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(profile)
                .addOnSuccessListener {
                    callback(
                        true,
                        "Your SplashDash account was created successfully."
                    )
                }
                .addOnFailureListener { exception ->

                    // The Firebase account exists, but the profile
                    // could not be saved. Sign out to avoid entering
                    // the application with an incomplete profile.
                    auth.signOut()

                    callback(
                        false,
                        "Your authentication account was created, " +
                                "but your profile could not be saved. " +
                                "Please try signing in again or contact support. " +
                                firestoreErrorMessage(exception)
                    )
                }
        }.addOnFailureListener { exception ->
            callback(
                false,
                authenticationErrorMessage(exception)
            )
        }
    }

    // SIGN IN USING EMAIL AND PASSWORD
    fun login(
        email: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || password.isBlank()) {
            callback(false, "Enter your email address and password.")
            return
        }

        auth.signInWithEmailAndPassword(
            cleanEmail,
            password
        ).addOnSuccessListener {
            callback(true, "Login successful.")
        }.addOnFailureListener { exception ->
            callback(
                false,
                authenticationErrorMessage(exception)
            )
        }
    }

    // LOAD THE FIRESTORE PROFILE FOR THE AUTHENTICATED USER
    fun loadProfile(
        user: FirebaseUser,
        callback: (Boolean, String, String) -> Unit
    ) {
        database.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    callback(false, "", "")
                    return@addOnSuccessListener
                }

                val name = document.getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: user.displayName
                    ?: user.email
                    ?: "Customer"

                val storedRole = document.getString("role")
                    ?.trim()
                    ?: ""

                // Unknown or missing roles must not receive
                // access to a privileged dashboard.
                val role = if (storedRole in allowedRoles) {
                    storedRole
                } else {
                    "Invalid"
                }

                callback(true, name, role)
            }
            .addOnFailureListener {
                callback(false, "", "")
            }
    }

    // SIGN OUT AND RESET LOCAL USER INFORMATION
    fun signOut() {
        auth.signOut()

        AppData.currentUser = "Guest"
        AppData.role = "Customer"
    }

    // FRIENDLIER FIREBASE AUTHENTICATION ERRORS
    private fun authenticationErrorMessage(
        exception: Exception
    ): String {

        val errorCode =
            (exception as? FirebaseAuthException)?.errorCode

        return when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "This email address is already registered."

            "ERROR_INVALID_EMAIL" ->
                "Please enter a valid email address."

            "ERROR_WEAK_PASSWORD" ->
                "Your password is too weak. Please choose a stronger password."

            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" ->
                "Incorrect email address or password."

            "ERROR_USER_DISABLED" ->
                "This account has been disabled. Please contact support."

            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Network error. Please check your internet connection."

            "ERROR_TOO_MANY_REQUESTS" ->
                "Too many attempts. Please wait and try again."

            else ->
                exception.localizedMessage
                    ?: "Authentication failed. Please try again."
        }
    }

    // FRIENDLIER FIRESTORE ERRORS
    private fun firestoreErrorMessage(
        exception: Exception
    ): String {

        val code =
            (exception as? FirebaseFirestoreException)?.code

        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firestore permission denied. Check your Firebase security rules."

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "The database is unavailable. Check your internet connection."

            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Firebase could not verify your account session."

            else ->
                exception.localizedMessage
                    ?: "Unknown database error."
        }
    }
}

