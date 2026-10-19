package com.example.splashanddash

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure user is logged in
        if (FirebaseAccountManager.auth.currentUser == null) {
            go(LoginActivity::class.java)
            finish()
            return
        }

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Book a Car Wash",
                "Schedule your SplashDash service"
            )
        )

        // Vehicle Selection
        main.addView(text("Vehicle Type", 17f, true))
        val vehicles = listOf("Sedan", "SUV", "Hatchback", "Bakkie", "Other")
        val vehicleSpinner = Spinner(this)
        vehicleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicles)
        main.addView(vehicleSpinner)

        val registration = input("Vehicle Registration")
        main.addView(registration)

        // Service Selection
        main.addView(text("Select Service", 17f, true))
        val services = listOf("Exterior Wash", "Interior Cleaning", "Full Detail")
        val serviceSpinner = Spinner(this)
        serviceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, services)
        main.addView(serviceSpinner)

        main.addView(
            text(
                "Exterior Wash: R400 | Interior Cleaning: R500 | Full Detail: R800",
                14f
            )
        )

        // Date Picker
        val dateField = input("Select Booking Date")
        dateField.isFocusable = false
        dateField.isClickable = true
        val calendar = Calendar.getInstance()

        dateField.setOnClickListener {
            val today = Calendar.getInstance()
            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day, 0, 0, 0)
                    dateField.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.minDate = today.timeInMillis
            picker.show()
        }
        main.addView(dateField)

        // Time Input
        val timeField = input("Time (HH:mm)")
        main.addView(timeField)

        // Submit Button
        val submitButton = button("CONFIRM BOOKING") {
            val vehicle = vehicleSpinner.selectedItem.toString()
            val reg = registration.text.toString().trim()
            val service = serviceSpinner.selectedItem.toString()
            val date = dateField.text.toString().trim()
            val time = timeField.text.toString().trim()

            // 1. Calculate the price based on selected service
            val price = when (service) {
                "Exterior Wash" -> 400
                "Interior Cleaning" -> 500
                "Full Detail" -> 800
                else -> 0
            }

            val timePattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

            when {
                reg.isBlank() -> showMessage("Enter your vehicle registration.")
                date.isBlank() -> showMessage("Please select a booking date.")
                !timePattern.matches(time) -> showMessage("Enter a valid time, for example 10:30.")
                else -> {
                    // 2. Pass price and status ("Pending") to the repository
                    FirestoreBookingRepository.createBooking(
                        vehicle,
                        reg,
                        service,
                        date,
                        time,
                        price,    // NEW FIELD
                        "Pending" // NEW FIELD: Ensures PaymentActivity can find it
                    ) { success, message ->
                        if (success) {
                            android.app.AlertDialog.Builder(this@BookingActivity)
                                .setTitle("Booking Confirmed")
                                .setMessage("Your booking has been saved online. Would you like to pay now?")
                                .setPositiveButton("PAY NOW") { _, _ ->
                                    go(PaymentActivity::class.java)
                                    finish()
                                }
                                .setNegativeButton("LATER") { _, _ ->
                                    go(BookingHistoryActivity::class.java)
                                    finish()
                                }
                                .show()
                        } else {
                            showMessage(message)
                        }
                    }
                }
            }
        }

        main.addView(submitButton)

        main.addView(outlineButton("VIEW MY BOOKINGS") {
            go(BookingHistoryActivity::class.java)
        })

        main.addView(outlineButton("BACK TO DASHBOARD") {
            go(CustomerDashboardActivity::class.java)
            finish()
        })

        main.addView(footer())
        screen.addView(main)
        setContentView(screen)
    }

    private fun showMessage(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("SplashDash")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
