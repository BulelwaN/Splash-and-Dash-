package com.example.splashanddash

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.EditText
import android.widget.Toast

class ContactActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Get In Touch",
                "We're always ready to help with your vehicle cleaning needs."
            )
        )

        // ---------------------------------------------------------
        // CONTACT INFORMATION
        // ---------------------------------------------------------

        main.addView(
            card(
                "Splash & Dash",
                """
                Professional vehicle cleaning made simple.
                
                Phone
                012 345 6789
                
                Email
                info@splashanddash.co.za
                
                Location
                Rosebank, Johannesburg
                """.trimIndent()
            )
        )

        addVerticalSpace(main, 10)

        // ---------------------------------------------------------
        // CONTACT FORM
        // ---------------------------------------------------------

        main.addView(
            sectionTitle(
                "Send Us a Message",
                "We'll get back to you as soon as possible."
            )
        )

        val name = input(
            "Your Name"
        )

        val email = input(
            "Email Address"
        )

        val message = input(
            "Your Message"
        )

        message.setSingleLine(false)
        message.minLines = 5
        message.gravity = Gravity.TOP
        message.setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14)
        )

        main.addView(name)
        main.addView(email)
        main.addView(message)

        main.addView(
            button("SEND MESSAGE") {

                if (
                    name.text.isBlank() ||
                    email.text.isBlank() ||
                    message.text.isBlank()
                ) {

                    Toast.makeText(
                        this,
                        "Please complete all fields.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@button
                }

                Toast.makeText(
                    this,
                    "Message sent successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                name.text.clear()
                email.text.clear()
                message.text.clear()
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
}