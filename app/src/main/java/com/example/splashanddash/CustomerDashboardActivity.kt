package com.example.splashanddash

import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class CustomerDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            go(LoginActivity::class.java)
            finish()
            return
        }

        showDashboard(
            total = null,
            pending = null,
            completed = null
        )
    }

    override fun onResume() {
        super.onResume()

        if (FirebaseAuth.getInstance().currentUser != null) {
            loadDashboardStatistics()
        }
    }

    private fun loadDashboardStatistics() {
        val expectedUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirestoreBookingRepository.getCustomerBookings { bookings, error ->
            if (isFinishing || isDestroyed) {
                return@getCustomerBookings
            }

            if (FirebaseAuth.getInstance().currentUser?.uid != expectedUid) {
                return@getCustomerBookings
            }

            if (error != null) {
                showDashboard(
                    total = null,
                    pending = null,
                    completed = null,
                    error = error
                )
                return@getCustomerBookings
            }

            val customerBookings = bookings.orEmpty()
            val totalBookings = customerBookings.size
            val pendingBookings = customerBookings.count { it.status.equals("Pending", ignoreCase = true) }
            val completedBookings = customerBookings.count { it.status.equals("Completed", ignoreCase = true) }

            showDashboard(
                total = totalBookings,
                pending = pendingBookings,
                completed = completedBookings
            )
        }
    }

    private fun showDashboard(
        total: Int?,
        pending: Int?,
        completed: Int?,
        error: String? = null
    ) {
        val screen = content()
        val main = column()

        // TOP NAVIGATION BAR
        main.addView(navBar())

        val customerName = AppData.currentUser
            .takeIf { it.isNotBlank() && it != "Guest" }
            ?: FirebaseAuth.getInstance().currentUser?.displayName
            ?: "Customer"

        // DASHBOARD HEADER
        main.addView(
            sectionTitle(
                "Customer Dashboard",
                "Welcome back, $customerName"
            )
        )

        // ACTIVITY METRICS ROW
        val totalText = total?.toString() ?: "—"
        val pendingText = pending?.toString() ?: "—"
        val completedText = completed?.toString() ?: "—"

        main.addView(
            metricRow(
                metricCard("Total Bookings", totalText),
                metricCard("Pending", pendingText),
                metricCard("Completed", completedText)
            )
        )

        if (error != null) {
            main.addView(
                text("Unable to load booking statistics: $error", 14f)
            )
            main.addView(
                outlineButton("RETRY") {
                    loadDashboardStatistics()
                }
            )
        } else if (total == null) {
            main.addView(
                text("Loading live activity...", 14f)
            )
        }

        addSpacing(main, 10)

        // QUICK ACTIONS SECTION
        main.addView(
            sectionTitle(
                "Quick Actions",
                "Manage your vehicle cleaning services"
            )
        )

        main.addView(
            card(
                "Book a Service",
                "Schedule a car wash or house call package at your location.",
                "Book Now"
            ) {
                go(BookingActivity::class.java)
            }
        )

        main.addView(
            card(
                "My Bookings",
                "View history and check live service status.",
                "View History"
            ) {
                go(BookingHistoryActivity::class.java)
            }
        )

        main.addView(
            card(
                "Payments",
                "Review pending payments and submit payment details.",
                "Pay Now"
            ) {
                go(PaymentActivity::class.java)
            }
        )

        main.addView(
            card(
                "Account Settings",
                "Manage profile and application settings.",
                "Settings"
            ) {
                go(SettingsActivity::class.java)
            }
        )

        addSpacing(main, 10)

        main.addView(
            outlineButton("LOG OUT") {
                FirebaseAccountManager.signOut()
                go(LoginActivity::class.java)
                finish()
            }
        )

        main.addView(footer())
        screen.addView(main)
        setContentView(screen)
    }
}
