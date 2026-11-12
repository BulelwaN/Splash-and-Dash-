package com.example.splashanddash

import android.os.Bundle

class AdminDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        // TOP NAVIGATION BAR
        main.addView(navBar())

        main.addView(
            sectionTitle(
                "Admin Dashboard",
                "Manage your Splash & Dash operations"
            )
        )

        // BUSINESS OVERVIEW METRICS
        main.addView(
            text(
                "Business Overview",
                18f,
                true
            )
        )

        val totalBookings = AppData.bookings.size.toString()
        val totalRevenue = "R${AppData.bookings.sumOf { it.price }}"

        main.addView(
            metricRow(
                metricCard("Total Bookings", totalBookings),
                metricCard("Customers", "12"),
                metricCard("Staff Members", "5")
            )
        )

        main.addView(
            metricRow(
                metricCard("Total Revenue", totalRevenue)
            )
        )

        addSpacing(main, 10)

        // QUICK ACTIONS
        main.addView(
            sectionTitle(
                "Quick Actions",
                "Access the main management areas"
            )
        )

        main.addView(
            card(
                "Manage Customers",
                "View and manage registered customer accounts.",
                "Open"
            ) {
                go(ManageCustomersActivity::class.java)
            }
        )

        main.addView(
            card(
                "Manage Bookings",
                "Review bookings and update their status.",
                "Open"
            ) {
                go(ManageBookingsActivity::class.java)
            }
        )

        main.addView(
            card(
                "Manage Staff",
                "Add staff members and view your current team.",
                "Open"
            ) {
                go(ManageStaffActivity::class.java)
            }
        )

        main.addView(
            card(
                "System Reports",
                "View booking, revenue and service statistics.",
                "Open"
            ) {
                go(ReportsActivity::class.java)
            }
        )

        addSpacing(main, 10)

        main.addView(
            outlineButton("LOG OUT") {
                go(MainActivity::class.java)
                finish()
            }
        )

        main.addView(footer())
        screen.addView(main)
        setContentView(screen)
    }
}
