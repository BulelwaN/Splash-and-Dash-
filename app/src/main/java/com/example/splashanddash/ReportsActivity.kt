package com.example.splashanddash

import android.os.Bundle

class ReportsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "System Reports",
                "Administrative reporting and business statistics"
            )
        )

        // ---------------------------------------------------------
        // OVERVIEW
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
                "Total Staff"
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
        // SERVICE POPULARITY
        // ---------------------------------------------------------

        main.addView(
            sectionTitle(
                "Service Popularity",
                "Number of bookings for each service"
            )
        )

        val serviceCounts =
            AppData.bookings
                .groupingBy { it.service }
                .eachCount()

        if (serviceCounts.isEmpty()) {

            main.addView(
                card(
                    "No Service Data",
                    "There are no service bookings available."
                )
            )

        } else {

            serviceCounts.forEach { (service, count) ->

                main.addView(
                    card(
                        service,
                        "$count booking(s)"
                    )
                )
            }
        }

        // ---------------------------------------------------------
        // BOOKING STATUS
        // ---------------------------------------------------------

        main.addView(
            sectionTitle(
                "Booking Status",
                "Current status of all bookings"
            )
        )

        val statusList = listOf(
            "Pending",
            "In Progress",
            "Completed"
        )

        statusList.forEach { status ->

            val count =
                AppData.bookings.count {
                    it.status == status
                }

            main.addView(
                card(
                    status,
                    "$count booking(s)"
                )
            )
        }

        main.addView(
            outlineButton("BACK TO ADMIN DASHBOARD") {
                go(AdminDashboardActivity::class.java)
            }
        )

        main.addView(footer())

        screen.addView(main)

        setContentView(screen)
    }
}