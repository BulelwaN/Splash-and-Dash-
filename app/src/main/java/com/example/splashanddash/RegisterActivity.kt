package com.example.splashanddash

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.LinearLayout

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

        val name = input("Full Name")
        val email = input("Email Address")
        val phone = input("Phone Number")
        val address = input("Address (Optional)")

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

            when {
                fullName.isBlank() -> {
                    showMessage("Please enter your full name.")
                }

                !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches() -> {
                    showMessage("Please enter a valid email address.")
                }

                userPhone.isBlank() -> {
                    showMessage("Please enter your phone number.")
                }

                userPassword.length < 8 -> {
                    showMessage(
                        "Your password must contain at least 8 characters."
                    )
                }

                userPassword != confirm -> {
                    showMessage("Passwords do not match.")
                }

                else -> {
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

    private fun showMessage(
        message: String,
        title: String = "SplashDash",
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


