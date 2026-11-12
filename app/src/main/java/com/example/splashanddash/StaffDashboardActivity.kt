package com.example.splashanddash

import android.os.Bundle

class StaffDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        // TOP NAVIGATION BAR
        main.addView(navBar())

        main.addView(
            sectionTitle(
                "Staff Dashboard",
                "Welcome back, ${AppData.currentUser}"
            )
        )

        val pending = AppData.bookings.count { it.status == "Pending" }.toString()
        val inProgress = AppData.bookings.count { it.status == "In Progress" }.toString()
        val completed = AppData.bookings.count { it.status == "Completed" }.toString()

        // QUEUE OVERVIEW METRICS
        main.addView(
            text(
                "Service Queue Overview",
                18f,
                true
            )
        )

        main.addView(
            metricRow(
                metricCard("Pending", pending),
                metricCard("In Progress", inProgress),
                metricCard("Completed", completed)
            )
        )

        addSpacing(main, 10)

        // QUICK ACTIONS
        main.addView(
            sectionTitle(
                "Service Operations",
                "Update customer bookings and progress"
            )
        )

        main.addView(
            card(
                "Service Queue",
                "View today's bookings and update service progress.",
                "Open Queue"
            ) {
                go(ServiceQueueActivity::class.java)
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
