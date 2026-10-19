package com.example.splashanddash

import android.os.Bundle

class StaffDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Staff Dashboard",
                "Welcome, ${AppData.currentUser}"
            )
        )

        val pending = AppData.bookings.count {
            it.status == "Pending"
        }

        val inProgress = AppData.bookings.count {
            it.status == "In Progress"
        }

        val completed = AppData.bookings.count {
            it.status == "Completed"
        }

        // ---------------------------------------------------------
        // QUEUE SUMMARY
        // ---------------------------------------------------------

        main.addView(
            text(
                "Service Overview",
                20f,
                true
            )
        )

        main.addView(
            metricCard(
                pending.toString(),
                "Pending"
            )
        )

        main.addView(
            metricCard(
                inProgress.toString(),
                "In Progress"
            )
        )

        main.addView(
            metricCard(
                completed.toString(),
                "Completed"
            )
        )

        addVerticalSpace(main, 12)

        // ---------------------------------------------------------
        // ACTIONS
        // ---------------------------------------------------------

        main.addView(
            card(
                "Service Queue",
                "View today's bookings and update service progress.",
                "Open"
            ) {
                go(ServiceQueueActivity::class.java)
            }
        )

        addVerticalSpace(main, 8)

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