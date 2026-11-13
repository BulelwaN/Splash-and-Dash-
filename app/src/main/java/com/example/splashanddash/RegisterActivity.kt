package com.example.splashanddash

import android.os.Bundle
import android.text.InputType
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
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        phone.inputType = InputType.TYPE_CLASS_PHONE

        main.addView(name)
        main.addView(email)
        main.addView(phone)
        main.addView(address)

        main.addView(text("Security", 20f, true))

        val password = input("Password")
        password.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        val confirmPassword = input("Confirm Password")
        confirmPassword.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        main.addView(password)
        main.addView(confirmPassword)

        lateinit var registerButton: android.widget.Button

        registerButton = button("CREATE ACCOUNT")  {

            val fullName = name.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val userPhone = phone.text.toString().trim()
            val userAddress = address.text.toString().trim()
            val userPassword = password.text.toString()
            val confirm = confirmPassword.text.toString()

            // 1. Full name validation
            val validName = fullName.length >= 3 &&
                    fullName.any { it.isLetter() } &&
                    fullName.all {
                        it.isLetter() || it.isWhitespace() ||
                                it == '-' || it == '\''
                    }

            if (!validName) {
                showMessage(
                    "Enter a valid full name with at least 3 characters. " +
                            "Letters, spaces, hyphens and apostrophes are allowed."
                )
                return@button
            }

            // 2. Email validation
            if (
                userEmail.isBlank() ||
                !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()
            ) {
                showMessage("Please enter a valid email address.")
                return@button
            }

            // 3. Phone validation
            val digitsOnly = userPhone.filter { it.isDigit() }

            if (digitsOnly.length !in 10..15) {
                showMessage(
                    "Please enter a valid phone number containing " +
                            "between 10 and 15 digits."
                )
                return@button
            }

            // 4. Address validation
            if (userAddress.isBlank()) {
                showMessage(
                    "Please enter your address for house call services."
                )
                return@button
            }

            if (!isJohannesburgLocation(userAddress)) {
                showMessage(
                    "SplashDash currently operates house call services " +
                            "within Johannesburg. Please enter a valid " +
                            "Johannesburg service-area address."
                )
                return@button
            }

            // 5. Password strength validation
            val (isStrong, passwordError) =
                validatePasswordStrength(userPassword)

            if (!isStrong) {
                showMessage(passwordError)
                return@button
            }

            // 6. Confirm password validation
            if (userPassword != confirm) {
                showMessage("Passwords do not match.")
                return@button
            }

            // Prevent multiple registration submissions
            registerButton.isEnabled = false
            registerButton.text = "CREATING ACCOUNT..."

            FirebaseAccountManager.register(
                fullName,
                userEmail,
                userPassword,
                userPhone,
                userAddress
            ) { success, message ->

                registerButton.isEnabled = true
                registerButton.text = "CREATE ACCOUNT"

                if (success) {

                    AppData.currentUser = fullName
                    AppData.currentUserId =
                        FirebaseAccountManager.auth.currentUser?.uid

                    AppData.role = "Customer"

                    showMessage(
                        "Your SplashDash account has been created successfully.",
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

    // PASSWORD COMPLEXITY VALIDATION
    private fun validatePasswordStrength(
        password: String
    ): Pair<Boolean, String> {

        if (password.length < 8) {
            return Pair(
                false,
                "Password must contain at least 8 characters."
            )
        }

        if (!password.any { it.isUpperCase() }) {
            return Pair(
                false,
                "Password must contain at least one uppercase letter."
            )
        }

        if (!password.any { it.isLowerCase() }) {
            return Pair(
                false,
                "Password must contain at least one lowercase letter."
            )
        }

        if (!password.any { it.isDigit() }) {
            return Pair(
                false,
                "Password must contain at least one number."
            )
        }

        val specialChars = "!@#$%^&*()_+-=[]{};':\"|,.<>/?`~"

        if (!password.any { specialChars.contains(it) }) {
            return Pair(
                false,
                "Password must contain at least one special character."
            )
        }

        return Pair(true, "")
    }

    // JOHANNESBURG SERVICE AREA VALIDATION
    private fun isJohannesburgLocation(address: String): Boolean {

        val clean = address.lowercase()

        val jhbKeywords = listOf(
            "johannesburg",
            "joburg",
            "jhb",
            "sandton",
            "randburg",
            "midrand",
            "rosebank",
            "soweto",
            "roodepoort",
            "fourways",
            "braamfontein",
            "kempton park",
            "alberton",
            "edenvale",
            "germiston",
            "lenasia",
            "woodmead",
            "sunninghill",
            "bryanston",
            "linden",
            "greenside",
            "parktown",
            "melville"
        )

        return jhbKeywords.any { clean.contains(it) }
    }

    // REGISTRATION FEEDBACK
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


