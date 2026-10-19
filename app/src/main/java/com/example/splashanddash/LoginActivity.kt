
package com.example.splashanddash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG = "SplashDashLogin"
    }

    private var isLoggingIn = false
    private var isLoadingProfile = false
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        // APP LOGO
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.splash_dash_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        main.addView(
            logo,
            LinearLayout.LayoutParams(-1, dp(140))
        )

        main.addView(
            sectionTitle(
                "Welcome Back",
                "Sign in to your SplashDash account"
            )
        )

        // LOGIN FIELDS
        val email = input("Email Address")

        email.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val password = input("Password")

        password.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        main.addView(email)
        main.addView(password)

        // EMAIL AND PASSWORD LOGIN
        main.addView(
            button("LOGIN") {

                if (isLoggingIn || hasNavigated) {
                    return@button
                }

                val userEmail = email.text.toString().trim()
                val userPassword = password.text.toString()

                if (!Patterns.EMAIL_ADDRESS
                        .matcher(userEmail).matches()) {

                    showMessage("Enter a valid email address.")
                    return@button
                }

                if (userPassword.isBlank()) {
                    showMessage("Enter your password.")
                    return@button
                }

                isLoggingIn = true

                Log.d(TAG, "Starting email login")

                FirebaseAccountManager.login(
                    userEmail,
                    userPassword
                ) { success, message ->

                    runOnUiThread {

                        if (isFinishing || isDestroyed) {
                            return@runOnUiThread
                        }

                        if (success) {

                            Log.d(
                                TAG,
                                "Firebase authentication successful"
                            )

                            openDashboard()

                        } else {

                            isLoggingIn = false

                            Log.e(
                                TAG,
                                "Authentication failed: $message"
                            )

                            showMessage(
                                "Login failed: $message"
                            )
                        }
                    }
                }
            }
        )

        main.addView(
            text("OR", 16f, true)
        )

        // GOOGLE SIGN-IN
        main.addView(
            outlineButton("CONTINUE WITH GOOGLE") {

                if (isLoggingIn || hasNavigated) {
                    return@outlineButton
                }

                isLoggingIn = true

                lifecycleScope.launch {

                    try {

                        GoogleSignInManager.signIn(
                            this@LoginActivity
                        ) { success, message ->

                            runOnUiThread {

                                if (isFinishing || isDestroyed) {
                                    return@runOnUiThread
                                }

                                if (success) {

                                    Log.d(
                                        TAG,
                                        "Google Sign-In successful"
                                    )

                                    openDashboard()

                                } else {

                                    isLoggingIn = false

                                    showMessage(message)
                                }
                            }
                        }

                    } catch (e: Exception) {

                        isLoggingIn = false

                        Log.e(
                            TAG,
                            "Google Sign-In exception",
                            e
                        )

                        showMessage(
                            e.localizedMessage
                                ?: "Google Sign-In failed."
                        )
                    }
                }
            }
        )

        // REGISTRATION
        main.addView(
            outlineButton("CREATE ACCOUNT") {

                if (!isLoggingIn) {
                    go(RegisterActivity::class.java)
                }
            }
        )

        // BACK TO HOME
        main.addView(
            outlineButton("BACK TO HOME") {

                if (!isLoggingIn) {
                    go(MainActivity::class.java)
                }
            }
        )

        main.addView(footer())

        screen.addView(main)
        setContentView(screen)
    }

    // LOAD PROFILE AND OPEN DASHBOARD
    private fun openDashboard() {

        if (hasNavigated || isLoadingProfile) {
            Log.d(TAG, "Duplicate navigation prevented")
            return
        }

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {

            isLoggingIn = false

            showMessage(
                "No authenticated user found."
            )

            return
        }

        isLoadingProfile = true

        Log.d(TAG, "Loading Firestore profile")

        FirebaseAccountManager.loadProfile(user) {
                profileFound, name, role ->

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                isLoadingProfile = false

                if (hasNavigated) {
                    return@runOnUiThread
                }

                Log.d(
                    TAG,
                    "Profile found: $profileFound, role: $role"
                )

                if (!profileFound) {

                    isLoggingIn = false

                    showMessage(
                        "Unable to load your account profile. " +
                                "Please check your connection and try again."
                    )

                    return@runOnUiThread
                }

                // Ensure the account has not changed
                if (FirebaseAuth.getInstance().currentUser?.uid
                    != user.uid) {

                    isLoggingIn = false

                    showMessage(
                        "Your login session changed. Please try again."
                    )

                    return@runOnUiThread
                }

                AppData.currentUser = name
                AppData.role = role

                val destination = when (role) {

                    "Admin" ->
                        AdminDashboardActivity::class.java

                    "Staff" ->
                        StaffDashboardActivity::class.java

                    "Customer" ->
                        CustomerDashboardActivity::class.java

                    else -> null
                }

                if (destination == null) {

                    isLoggingIn = false

                    showMessage(
                        "Invalid account role. Contact support."
                    )

                    return@runOnUiThread
                }

                hasNavigated = true

                Log.d(
                    TAG,
                    "Opening dashboard for role: $role"
                )

                val intent = Intent(
                    this@LoginActivity,
                    destination
                )

                try {

                    startActivity(intent)
                    finish()

                } catch (e: Exception) {

                    hasNavigated = false
                    isLoggingIn = false

                    Log.e(
                        TAG,
                        "Dashboard navigation failed",
                        e
                    )

                    showMessage(
                        "Unable to open dashboard: " +
                                (e.localizedMessage ?: "Unknown error")
                    )
                }
            }
        }
    }

    // DISPLAY ERROR MESSAGES
    private fun showMessage(message: String) {

        if (isFinishing || isDestroyed) {
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("SplashDash")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}



