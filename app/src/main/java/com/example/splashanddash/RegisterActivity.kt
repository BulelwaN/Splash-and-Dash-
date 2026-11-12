package com.example.splashanddash

import android.os.Bundle
import android.util.Patterns

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Create Account",
                "Register securely with SplashDash"
            )
        )

        main.addView(text("Personal Information", 20f, true))

        val name = input("Full Name (e.g. John Smith)")
        val email = input("Email Address")
        val phone = input("Phone Number")
        val address = input("Address in Johannesburg (Required)")

        email.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        phone.inputType =
            android.text.InputType.TYPE_CLASS_PHONE

        main.addView(name)
        main.addView(email)
        main.addView(phone)
        main.addView(address)

        main.addView(text("Security", 20f, true))

        val password = input("Password")
        password.inputType = 129

        val confirmPassword = input("Confirm Password")
        confirmPassword.inputType = 129

        main.addView(password)
        main.addView(confirmPassword)

        val registerButton = button("CREATE ACCOUNT") {

            val fullName = name.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val userPhone = phone.text.toString().trim()
            val userAddress = address.text.toString().trim()
            val userPassword = password.text.toString()
            val confirm = confirmPassword.text.toString()

            // 1. Name Validation: Minimum 3 characters, no special characters like '-'
            val nameRegex = Regex("^[a-zA-Z\\s]{3,}$")
            if (fullName.length < 3 || !nameRegex.matches(fullName)) {
                showMessage(
                    "Full Name must be at least 3 characters long and contain only letters and spaces (no special characters like '-')."
                )
                return@button
            }

            // 2. Email Validation: Mandatory & valid email
            if (userEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                showMessage("Please enter a valid email address.")
                return@button
            }

            // 3. Phone Validation: Mandatory & valid phone number (at least 10 digits)
            val digitsOnly = userPhone.filter { it.isDigit() }
            if (userPhone.isBlank() || digitsOnly.length < 10) {
                showMessage("Please enter a valid phone number with at least 10 digits.")
                return@button
            }

            // 4. Address Validation: Mandatory & strictly Johannesburg location
            if (userAddress.isBlank()) {
                showMessage("Please enter your address for house call service delivery.")
                return@button
            }
            if (!isJohannesburgLocation(userAddress)) {
                showMessage(
                    "SplashDash operates house call services strictly within Johannesburg. Please enter a valid Johannesburg address."
                )
                return@button
            }

            // 5. Password Complexity Validation
            val (isStrong, pwdError) = validatePasswordStrength(userPassword)
            if (!isStrong) {
                showMessage(pwdError)
                return@button
            }

            if (userPassword != confirm) {
                showMessage("Passwords do not match.")
                return@button
            }

            FirebaseAccountManager.register(
                fullName,
                userEmail,
                userPassword,
                userPhone,
                userAddress
            ) { success, message ->

                if (success) {
                    AppData.currentUser = fullName
                    AppData.role = "Customer"

                    showMessage(
                        "Account created successfully!",
                        "Registration Successful"
                    ) {
                        go(CustomerDashboardActivity::class.java)
                        finish()
                    }
                } else {
                    showMessage(message)
                }
            }
        }

        main.addView(registerButton)

        main.addView(
            outlineButton("ALREADY HAVE AN ACCOUNT? LOGIN") {
                go(LoginActivity::class.java)
            }
        )

        main.addView(
            outlineButton("BACK TO HOME") {
                go(MainActivity::class.java)
            }
        )

        main.addView(footer())
        screen.addView(main)
        setContentView(screen)
    }

    private fun validatePasswordStrength(password: String): Pair<Boolean, String> {
        if (password.length < 8) {
            return Pair(false, "Password must be at least 8 characters long.")
        }
        if (!password.any { it.isUpperCase() }) {
            return Pair(false, "Password must contain at least one uppercase letter (A-Z).")
        }
        if (!password.any { it.isLowerCase() }) {
            return Pair(false, "Password must contain at least one lowercase letter (a-z).")
        }
        if (!password.any { it.isDigit() }) {
            return Pair(false, "Password must contain at least one number (0-9).")
        }
        val specialChars = "!@#$%^&*()_+-=[]{};':\"|,.<>/?`~"
        if (!password.any { specialChars.contains(it) }) {
            return Pair(false, "Password must contain at least one special character (e.g. !@#$%^&*).")
        }
        return Pair(true, "")
    }

    private fun isJohannesburgLocation(address: String): Boolean {
        val clean = address.lowercase()
        val jhbKeywords = listOf(
            "johannesburg", "joburg", "jhb", "sandton", "randburg", "midrand",
            "rosebank", "soweto", "roodepoort", "fourways", "braamfontein",
            "kempton park", "alberton", "edenvale", "germiston", "woodmead",
            "sunninghill", "bryanston", "linden", "greenside", "parktown", "melville"
        )
        return jhbKeywords.any { clean.contains(it) }
    }

    private fun showMessage(
        message: String,
        title: String = "SplashDash Validation",
        onOk: (() -> Unit)? = null
    ) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                onOk?.invoke()
            }
            .show()
    }
}
