package com.example.splashanddash

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAccountManager {

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val database: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        callback: (Boolean, String) -> Unit
    ) {
        // Trim inputs to avoid mismatch with strict Firestore rules
        val cleanEmail = email.trim()

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { result ->

                val user = result.user

                if (user == null) {
                    callback(false, "Registration failed.")
                    return@addOnSuccessListener
                }

                // Use user.email directly from the Auth object to ensure it 
                // matches request.auth.token.email in the Firestore rules
                val profile = hashMapOf(
                    "uid" to user.uid,
                    "name" to name.trim(),
                    "email" to (user.email ?: cleanEmail),
                    "phone" to phone.trim(),
                    "address" to address.trim(),
                    "role" to "Customer"
                )

                database.collection("users")
                    .document(user.uid)
                    .set(profile)
                    .addOnSuccessListener {
                        callback(true, "Account created successfully.")
                    }
                    .addOnFailureListener { exception ->
                        callback(
                            false,
                            "Account created, but profile could not be saved: ${exception.localizedMessage}"
                        )
                    }
            }
            .addOnFailureListener { exception ->
                callback(
                    false,
                    exception.localizedMessage ?: "Registration failed."
                )
            }
    }

    fun login(
        email: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                callback(true, "Login successful.")
            }
            .addOnFailureListener { exception ->
                callback(
                    false,
                    exception.localizedMessage ?: "Login failed."
                )
            }
    }

    fun loadProfile(
        user: FirebaseUser,
        callback: (Boolean, String, String) -> Unit
    ) {
        database.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    callback(false, "", "")
                    return@addOnSuccessListener
                }

                val name = document.getString("name")
                    ?: user.displayName
                    ?: user.email
                    ?: "Customer"

                val role = document.getString("role") ?: "Customer"

                callback(true, name, role)
            }
            .addOnFailureListener {
                callback(false, "", "")
            }
    }

    fun signOut() {
        auth.signOut()
        AppData.currentUser = "Guest"
        AppData.role = "Customer"
    }
}
