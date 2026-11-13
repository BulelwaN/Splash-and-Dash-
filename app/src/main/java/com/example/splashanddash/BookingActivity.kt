package com.example.splashanddash

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingActivity : BaseActivity() {

    private val editBookingId: String
        get() = intent.getStringExtra("edit_booking_id") ?: ""

    private val isEditMode: Boolean
        get() = editBookingId.isNotBlank()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAccountManager.auth.currentUser == null) {
            go(LoginActivity::class.java)
            finish()
            return
        }

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                if (isEditMode) "Edit Booking" else "Book a House Call Wash",
                if (isEditMode) "Update your SplashDash booking details"
                else "Schedule your SplashDash service in Johannesburg"
            )
        )

        // Vehicle selection
        main.addView(text("Vehicle Type", 17f, true))

        val vehicles = listOf("Sedan", "SUV", "Hatchback", "Bakkie", "Other")
        val vehicleSpinner = Spinner(this)

        vehicleSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            vehicles
        )

        main.addView(vehicleSpinner)

        val registration = input("Vehicle Registration (e.g. AB 12 CD GP)")
        registration.setText(intent.getStringExtra("edit_reg") ?: "")
        main.addView(registration)

        // Service selection
        main.addView(text("Select Wash Service", 17f, true))

        val services = listOf(
            "Exterior Wash",
            "Interior Cleaning",
            "Full Detail"
        )

        val serviceSpinner = Spinner(this)

        serviceSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            services
        )

        main.addView(serviceSpinner)

        main.addView(
            text(
                "Exterior Wash: R400 | Interior Cleaning: R500 | Full Detail: R800",
                14f
            )
        )

        // Location
        main.addView(text("House Call Location (Johannesburg)", 17f, true))

        val locationInput = input("Street Address & Suburb in Johannesburg")
        locationInput.setText(intent.getStringExtra("edit_location") ?: "")
        main.addView(locationInput)

        // Date
        main.addView(text("Booking Date", 17f, true))

        val dateField = input("Tap to Select Booking Date")
        dateField.isFocusable = false
        dateField.isClickable = true

        val calendar = Calendar.getInstance()

        val existingDate = intent.getStringExtra("edit_date")
        if (!existingDate.isNullOrBlank()) {
            dateField.setText(existingDate)

            try {
                val parsedDate = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).parse(existingDate)

                if (parsedDate != null) {
                    calendar.time = parsedDate
                }
            } catch (_: Exception) {
                // Keep the current date if parsing fails.
            }
        }

        dateField.setOnClickListener {
            val today = Calendar.getInstance()

            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day, 0, 0, 0)

                    dateField.setText(
                        SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                        ).format(calendar.time)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            picker.datePicker.minDate = today.timeInMillis
            picker.show()
        }

        main.addView(dateField)

        // Time
        main.addView(text("Booking Time (08:00 - 17:00)", 17f, true))

        val timeField = input("Tap to Select Time (08:00 - 17:00)")
        timeField.isFocusable = false
        timeField.isClickable = true

        timeField.setText(intent.getStringExtra("edit_time") ?: "")

        timeField.setOnClickListener {
            val currentHour =
                if (calendar.get(Calendar.HOUR_OF_DAY) in 8..17) {
                    calendar.get(Calendar.HOUR_OF_DAY)
                } else {
                    9
                }

            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    timeField.setText(
                        String.format(
                            Locale.US,
                            "%02d:%02d",
                            hourOfDay,
                            minute
                        )
                    )
                },
                currentHour,
                0,
                true
            )

            timePicker.show()
        }

        main.addView(timeField)

        // Preselect existing vehicle and service
        val existingVehicle = intent.getStringExtra("edit_vehicle")
        val existingService = intent.getStringExtra("edit_service")

        vehicles.indexOf(existingVehicle).takeIf { it >= 0 }?.let {
            vehicleSpinner.setSelection(it)
        }

        services.indexOf(existingService).takeIf { it >= 0 }?.let {
            serviceSpinner.setSelection(it)
        }

        // Save or update booking
        main.addView(
            button(
                if (isEditMode) "SAVE CHANGES" else "CONFIRM BOOKING"
            ) {
                val vehicle = vehicleSpinner.selectedItem.toString()
                val reg = registration.text.toString().trim()
                val service = serviceSpinner.selectedItem.toString()
                val location = locationInput.text.toString().trim()
                val date = dateField.text.toString().trim()
                val time = timeField.text.toString().trim()

                val price = when (service) {
                    "Exterior Wash" -> 400
                    "Interior Cleaning" -> 500
                    "Full Detail" -> 800
                    else -> 0
                }

                if (reg.isBlank()) {
                    showMessage("Please enter your vehicle registration.")
                    return@button
                }

                if (location.isBlank()) {
                    showMessage("Please enter your house call address.")
                    return@button
                }

                if (!isJohannesburgLocation(location)) {
                    showMessage(
                        "SplashDash operates house call services strictly within Johannesburg. Please enter a valid Johannesburg location."
                    )
                    return@button
                }

                if (date.isBlank()) {
                    showMessage("Please select a booking date.")
                    return@button
                }

                val dateFormat = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

                val selectedDate = try {
                    dateFormat.parse(date)
                } catch (_: Exception) {
                    null
                }

                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                if (selectedDate == null || selectedDate.before(todayStart)) {
                    showMessage(
                        "Please select a valid future date (today or an upcoming date)."
                    )
                    return@button
                }

                if (time.isBlank()) {
                    showMessage("Please select a booking time.")
                    return@button
                }

                val timeParts = time.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: -1
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: -1

                if (
                    hour !in 8..17 ||
                    minute !in 0..59 ||
                    (hour == 17 && minute > 0)
                ) {
                    showMessage(
                        "Bookings are available between 08:00 and 17:00."
                    )
                    return@button
                }

                val now = Calendar.getInstance()
                val isToday = dateFormat.format(now.time) == date

                if (isToday) {
                    val currentHour = now.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = now.get(Calendar.MINUTE)

                    if (
                        hour < currentHour ||
                        (hour == currentHour && minute <= currentMinute)
                    ) {
                        showMessage(
                            "The selected time has already passed. Please choose a future time."
                        )
                        return@button
                    }
                }

                if (isEditMode) {
                    FirestoreBookingRepository.updateBooking(
                        firestoreId = editBookingId,
                        vehicle = vehicle,
                        reg = reg,
                        service = service,
                        date = date,
                        time = time,
                        price = price,
                        location = location
                    ) { success, message ->
                        if (success) {
                            showMessage("Your booking has been updated successfully.")
                            finish()
                        } else {
                            showMessage(message)
                        }
                    }
                } else {
                    FirestoreBookingRepository.createBooking(
                        vehicle = vehicle,
                        reg = reg,
                        service = service,
                        date = date,
                        time = time,
                        price = price,
                        status = "Pending",
                        location = location
                    ) { success, message ->
                        if (success) {
                            android.app.AlertDialog.Builder(this@BookingActivity)
                                .setTitle("Booking Confirmed")
                                .setMessage(
                                    "Your house call booking for $date at $time in Johannesburg has been saved. Would you like to proceed to payment?"
                                )
                                .setPositiveButton("PAY NOW") { _, _ ->
                                    go(PaymentActivity::class.java)
                                    finish()
                                }
                                .setNegativeButton("PAY LATER") { _, _ ->
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
        )

        if (!isEditMode) {
            main.addView(
                outlineButton("VIEW MY BOOKINGS") {
                    go(BookingHistoryActivity::class.java)
                }
            )
        }

        main.addView(
            outlineButton("BACK") {
                finish()
            }
        )

        main.addView(footer())

        screen.addView(main)
        setContentView(screen)
    }

    private fun isJohannesburgLocation(address: String): Boolean {
        val clean = address.lowercase()

        val keywords = listOf(
            "johannesburg", "joburg", "jhb", "sandton", "randburg",
            "midrand", "lenasia", "rosebank", "soweto", "roodepoort", "fourways",
            "braamfontein", "kempton park", "alberton", "edenvale",
            "germiston", "woodmead", "sunninghill", "bryanston",
            "linden", "greenside", "parktown", "melville"
        )

        return keywords.any { clean.contains(it) }
    }

    private fun showMessage(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("SplashDash Booking")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}

