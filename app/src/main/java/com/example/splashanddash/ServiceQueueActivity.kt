package com.example.splashanddash

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner


class ServiceQueueActivity : BaseActivity() {

    private val statuses = arrayOf(
        "Pending",
        "In Progress",
        "Completed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Service Queue",
                "Manage today's vehicle cleaning services"
            )
        )

        if (AppData.bookings.isEmpty()) {

            main.addView(
                card(
                    "Queue Empty",
                    "There are currently no bookings waiting for service."
                )
            )

        } else {

            AppData.bookings.forEach { booking ->

                val details = """
                    Vehicle: ${booking.vehicle}
                    
                    Registration: ${booking.reg}
                    
                    Service: ${booking.service}
                    
                    Date: ${booking.date}
                    
                    Time: ${booking.time}
                    
                    Current Status: ${booking.status}
                """.trimIndent()

                val bookingCard = card(
                    "Booking #${booking.id}",
                    details
                )

                bookingCard.addView(
                    text(
                        "Update Service Status",
                        14f,
                        true
                    )
                )

                val statusSpinner = Spinner(this)

                statusSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    statuses
                )

                val position =
                    statuses.indexOf(booking.status)

                if (position >= 0) {
                    statusSpinner.setSelection(position)
                }

                bookingCard.addView(statusSpinner)

                bookingCard.addView(
                    button("UPDATE STATUS") {

                        booking.status =
                            statusSpinner.selectedItem.toString()

                        showAppMessage("Service status updated.")

                        recreate()
                    }
                )

                main.addView(bookingCard)
            }
        }

        main.addView(
            outlineButton("BACK TO STAFF DASHBOARD") {
                go(StaffDashboardActivity::class.java)
            }
        )

        main.addView(footer())

        screen.addView(main)

        setContentView(screen)
    }
}