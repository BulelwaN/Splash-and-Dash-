package com.example.splashanddash

import android.os.Bundle
import android.widget.LinearLayout
import com.google.firebase.auth.FirebaseAuth

class CustomerDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d(
            "SplashDashDashboard",
            "Customer Dashboard onCreate called"
        )

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

        val expectedUid =
            FirebaseAuth.getInstance().currentUser?.uid ?: return
        android.util.Log.d(
            "SplashDashDashboard",
            "Loading Firestore booking statistics"
        )

        FirestoreBookingRepository.getCustomerBookings {
                bookings, error ->

            if (isFinishing || isDestroyed) {
                return@getCustomerBookings
            }

            // Ignore results if the signed-in account changed.
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

            val pendingBookings = customerBookings.count {
                it.status.equals(
                    "Pending",
                    ignoreCase = true
                )
            }

            val completedBookings = customerBookings.count {
                it.status.equals(
                    "Completed",
                    ignoreCase = true
                )
            }

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

        // Retrieve the signed-in customer's name.
        val customerName = AppData.currentUser
            .takeIf { it.isNotBlank() && it != "Guest" }
            ?: FirebaseAuth.getInstance()
                .currentUser
                ?.displayName
            ?: "Customer"

        // DASHBOARD HEADER
        main.addView(
            sectionTitle(
                "Customer Dashboard",
                "Welcome back, $customerName"
            )
        )

        // ACTIVITY SECTION
        main.addView(
            text(
                "Your Activity",
                20f,
                true
            )
        )

        val totalText = total?.toString() ?: "—"
        val pendingText = pending?.toString() ?: "—"
        val completedText = completed?.toString() ?: "—"

        main.addView(
            card(
                "Total Bookings",
                totalText
            )
        )

        main.addView(
            card(
                "Pending Bookings",
                pendingText
            )
        )

        main.addView(
            card(
                "Completed Services",
                completedText
            )
        )

        if (error != null) {
            main.addView(
                text(
                    "Unable to load booking statistics: $error",
                    14f
                )
            )

            main.addView(
                outlineButton("RETRY") {
                    loadDashboardStatistics()
                }
            )
        } else if (total == null) {
            main.addView(
                text(
                    "Loading your booking statistics...",
                    14f
                )
            )
        }

        // CUSTOMER ACTIONS
        main.addView(
            sectionTitle(
                "What would you like to do?",
                "Manage your vehicle cleaning services"
            )
        )

        main.addView(
            card(
                "Book a Service",
                "Choose a car wash package and select a convenient date and time."
            )
        )

        main.addView(
            button("BOOK A SERVICE") {
                go(BookingActivity::class.java)
            }
        )

        main.addView(
            card(
                "My Bookings",
                "View your booking history and track the status of your services."
            )
        )

        main.addView(
            button("VIEW MY BOOKINGS") {
                go(BookingHistoryActivity::class.java)
            }
        )

        main.addView(
            card(
                "Payments",
                "View booking payment information and submit payment requests."
            )
        )

        main.addView(
            button("PAYMENTS") {
                go(PaymentActivity::class.java)
            }
        )

        main.addView(
            card(
                "Account Settings",
                "Manage your account and application preferences."
            )
        )

        main.addView(
            outlineButton("SETTINGS") {
                go(SettingsActivity::class.java)
            }
        )

        main.addView(
            outlineButton("REFRESH DASHBOARD") {
                loadDashboardStatistics()
            }
        )

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
