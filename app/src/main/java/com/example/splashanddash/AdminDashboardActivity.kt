package com.example.splashanddash

import android.os.Bundle
import android.widget.LinearLayout

class AdminDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Admin Dashboard",
                "Manage your Splash & Dash operations"
            )
        )

        // ---------------------------------------------------------
        // SUMMARY
        // ---------------------------------------------------------

        main.addView(
            text(
                "Business Overview",
                20f,
                true
            )
        )

        main.addView(
            metricCard(
                AppData.bookings.size.toString(),
                "Total Bookings"
            )
        )

        main.addView(
            metricCard(
                "12",
                "Total Customers"
            )
        )

        main.addView(
            metricCard(
                "5",
                "Staff Members"
            )
        )

        main.addView(
            metricCard(
                "R${AppData.bookings.sumOf { it.price }}",
                "Total Revenue"
            )
        )

        addVerticalSpace(main, 12)

        // ---------------------------------------------------------
        // QUICK ACTIONS
        // ---------------------------------------------------------

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

        addVerticalSpace(main, 10)

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