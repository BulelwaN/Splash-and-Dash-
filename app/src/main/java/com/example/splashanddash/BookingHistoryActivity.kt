package com.example.splashanddash

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

class BookingHistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAccountManager.auth.currentUser == null) {
            go(LoginActivity::class.java)
            finish()
            return
        }
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
                "View and manage your SplashDash bookings"
            )
        )

        main.addView(text("Loading your bookings...", 16f))

        screen.addView(main)
        setContentView(screen)

        FirestoreBookingRepository.getCustomerBookings { bookings, error ->

            if (isFinishing || isDestroyed) return@getCustomerBookings

            main.removeAllViews()

            main.addView(
                sectionTitle(
                    "My Bookings",
                    "Your online booking history"
                )
            )

            when {
                error != null -> {
                    main.addView(text("Error: $error", 16f))
                }

                bookings.isNullOrEmpty() -> {
                    main.addView(
                        text("You have no bookings yet.", 17f)
                    )
                }

                else -> {
                    main.addView(
                        text("Total Bookings: ${bookings.size}", 18f, true)
                    )

                    bookings.forEach { booking ->

                        val details = """
                            Vehicle: ${booking.vehicle}
                            Registration: ${booking.reg}
                            Service: ${booking.service}
                            Price: R${booking.price}
                            Date: ${booking.date}
                            Time: ${booking.time}
                            Location: ${booking.location}
                            Status: ${booking.status}
                        """.trimIndent()

                        main.addView(
                            card(
                                "Booking ${booking.id.toString().take(8)}",
                                details
                            )
                        )

                        val canManage =
                            booking.status.equals("Pending", true) ||
                                    booking.status.equals("Confirmed", true)

                        if (canManage && booking.firestoreId.isNotBlank()) {

                            main.addView(
                                button("EDIT BOOKING") {
                                    openEditBooking(booking)
                                }
                            )

                            main.addView(
                                outlineButton("CANCEL BOOKING") {
                                    confirmCancellation(booking.firestoreId)
                                }
                            )

                        } else {
                            main.addView(
                                text(
                                    "Editing and cancellation are unavailable for this booking.",
                                    14f
                                )
                            )
                        }
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

    private fun openEditBooking(booking: OnlineBooking) {

        val intent = Intent(this, BookingActivity::class.java).apply {
            putExtra("edit_booking_id", booking.firestoreId)
            putExtra("edit_vehicle", booking.vehicle)
            putExtra("edit_reg", booking.reg)
            putExtra("edit_service", booking.service)
            putExtra("edit_location", booking.location)
            putExtra("edit_date", booking.date)
            putExtra("edit_time", booking.time)
        }

        startActivity(intent)
    }

    private fun confirmCancellation(firestoreId: String) {

        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage(
                "Are you sure you want to cancel this booking? This action will update its status to Cancelled."
            )
            .setPositiveButton("YES, CANCEL") { _, _ ->

                FirestoreBookingRepository.cancelBooking(firestoreId) {
                        success, message ->

                    if (isFinishing || isDestroyed) return@cancelBooking

                    AlertDialog.Builder(this)
                        .setTitle(
                            if (success) "Booking Cancelled"
                            else "Cancellation Failed"
                        )
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ ->
                            if (success) {
                                loadBookings()
                            }
                        }
                        .show()
                }
            }
            .setNegativeButton("KEEP BOOKING", null)
            .show()
    }
}

