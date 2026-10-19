package com.example.splashanddash

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

class ManageBookingsActivity : BaseActivity() {

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
                "Manage Bookings",
                "Review and update customer bookings"
            )
        )

        if (AppData.bookings.isEmpty()) {

            main.addView(
                card(
                    "No Bookings",
                    "There are currently no bookings in the system."
                )
            )

        } else {

            AppData.bookings.forEach { booking ->

                val details = """
                    Customer: ${AppData.currentUser}
                    
                    Vehicle: ${booking.vehicle}
                    
                    Registration: ${booking.reg}
                    
                    Service: ${booking.service}
                    
                    Date: ${booking.date}
                    
                    Time: ${booking.time}
                    
                    Price: R${booking.price}
                """.trimIndent()

                val bookingCard = card(
                    "Booking #${booking.id}",
                    details
                )

                val statusSpinner = Spinner(this)

                statusSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    statuses
                )

                val currentPosition =
                    statuses.indexOf(booking.status)

                if (currentPosition >= 0) {
                    statusSpinner.setSelection(currentPosition)
                }

                bookingCard.addView(
                    text(
                        "Booking Status",
                        14f,
                        true
                    )
                )

                bookingCard.addView(statusSpinner)

                bookingCard.addView(
                    button("UPDATE STATUS") {

                        booking.status =
                            statusSpinner.selectedItem.toString()

                        Toast.makeText(
                            this,
                            "Booking #${booking.id} updated.",
                            Toast.LENGTH_SHORT
                        ).show()

                        recreate()
                    }
                )

                main.addView(bookingCard)
            }
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