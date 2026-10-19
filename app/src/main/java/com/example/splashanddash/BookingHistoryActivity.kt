package com.example.splashanddash

import android.os.Bundle
import android.widget.LinearLayout

class BookingHistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAccountManager.auth.currentUser == null) {
            go(LoginActivity::class.java)
            finish()
            return
        }

        loadBookings()
    }

    override fun onResume() {
        super.onResume()

        if (FirebaseAccountManager.auth.currentUser != null) {
            loadBookings()
        }
    }

    private fun loadBookings() {

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "My Bookings",
                "View your SplashDash booking history"
            )
        )

        main.addView(
            text("Loading your bookings...", 16f)
        )

        screen.addView(main)
        setContentView(screen)

        FirestoreBookingRepository.getCustomerBookings {
                bookings, error ->

            if (isFinishing || isDestroyed) {
                return@getCustomerBookings
            }

            main.removeAllViews()

            main.addView(
                sectionTitle(
                    "My Bookings",
                    "Your online booking history"
                )
            )

            when {
                error != null -> {
                    main.addView(
                        text("Error: $error", 16f)
                    )
                }

                bookings.isNullOrEmpty() -> {
                    main.addView(
                        text(
                            "You have no bookings yet.",
                            17f
                        )
                    )
                }

                else -> {
                    main.addView(
                        text(
                            "Total Bookings: ${bookings.size}",
                            18f,
                            true
                        )
                    )

                    bookings.forEach { booking ->

                        val details = """
                            Vehicle: ${booking.vehicle}
                            Registration: ${booking.reg}
                            Service: ${booking.service}
                            Price: R${booking.price}
                            Date: ${booking.date}
                            Time: ${booking.time}
                            Status: ${booking.status}
                        """.trimIndent()

                        main.addView(
                            card(
                                "Booking ${booking.id.toString().take(8)}",
                                details
                            )
                        )
                    }
                }
            }

            main.addView(
                button("BOOK ANOTHER SERVICE") {
                    go(BookingActivity::class.java)
                }
            )

            main.addView(
                outlineButton("REFRESH BOOKINGS") {
                    loadBookings()
                }
            )

            main.addView(
                outlineButton("BACK TO DASHBOARD") {
                    go(CustomerDashboardActivity::class.java)
                    finish()
                }
            )

            main.addView(footer())
        }
    }
}
