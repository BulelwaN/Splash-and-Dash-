package com.example.splashanddash

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
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
                "Book a House Call Wash",
                "Schedule your SplashDash service at your location in Johannesburg"
            )
        )

        // Vehicle Selection
        main.addView(text("Vehicle Type", 17f, true))
        val vehicles = listOf("Sedan", "SUV", "Hatchback", "Bakkie", "Other")
        val vehicleSpinner = Spinner(this)
        vehicleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicles)
        main.addView(vehicleSpinner)

        val registration = input("Vehicle Registration (e.g. AB 12 CD GP)")
        main.addView(registration)

        // Service Selection
        main.addView(text("Select Wash Service", 17f, true))
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

        // House Call Location Address (Strictly Johannesburg)
        main.addView(text("House Call Location (Johannesburg)", 17f, true))
        val locationInput = input("Street Address & Suburb in Johannesburg")
        main.addView(locationInput)

        // Date Picker (Strictly Future / Today & upcoming dates)
        main.addView(text("Booking Date", 17f, true))
        val dateField = input("Tap to Select Booking Date")
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
            // Restrict DatePicker UI to today and future dates only
            picker.datePicker.minDate = today.timeInMillis
            picker.show()
        }
        main.addView(dateField)

        // Time Picker (Enforce 08:00 to 17:00)
        main.addView(text("Booking Time (08:00 - 17:00)", 17f, true))
        val timeField = input("Tap to Select Time (08:00 - 17:00)")
        timeField.isFocusable = false
        timeField.isClickable = true

        timeField.setOnClickListener {
            val currentHour = if (calendar.get(Calendar.HOUR_OF_DAY) in 8..17) calendar.get(Calendar.HOUR_OF_DAY) else 9
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val formattedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                    timeField.setText(formattedTime)
                },
                currentHour,
                0,
                true
            )
            timePicker.show()
        }
        main.addView(timeField)

        // Submit Button
        val submitButton = button("CONFIRM BOOKING") {
            val vehicle = vehicleSpinner.selectedItem.toString()
            val reg = registration.text.toString().trim()
            val service = serviceSpinner.selectedItem.toString()
            val location = locationInput.text.toString().trim()
            val date = dateField.text.toString().trim()
            val time = timeField.text.toString().trim()

            // Calculate price
            val price = when (service) {
                "Exterior Wash" -> 400
                "Interior Cleaning" -> 500
                "Full Detail" -> 800
                else -> 0
            }

            // Validations
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

            // Date validation: Must be today or a future date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val selectedDateObj = try { dateFormat.parse(date) } catch (_: Exception) { null }
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            if (selectedDateObj == null || selectedDateObj.before(todayStart)) {
                showMessage("Please select a valid future date for your booking (today or an upcoming date).")
                return@button
            }

            if (time.isBlank()) {
                showMessage("Please select a booking time.")
                return@button
            }

            // Validate time window (8am to 5pm: 08:00 - 17:00)
            val timeParts = time.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: -1
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: -1

            if (hour < 8 || hour > 17 || (hour == 17 && minute > 0)) {
                showMessage(
                    "House call bookings are strictly available between 8:00 AM (08:00) and 5:00 PM (17:00). Please select a time within business operating hours."
                )
                return@button
            }

            // Same day validation: If booking for today, time must be in the future
            val now = Calendar.getInstance()
            val isToday = dateFormat.format(now.time) == date
            if (isToday) {
                val currentHourOfDay = now.get(Calendar.HOUR_OF_DAY)
                val currentMinuteOfHour = now.get(Calendar.MINUTE)
                if (hour < currentHourOfDay || (hour == currentHourOfDay && minute <= currentMinuteOfHour)) {
                    showMessage("The selected time slot has already passed for today. Please choose a future time slot or date.")
                    return@button
                }
            }

            // Save booking in Firestore
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
                        .setMessage("Your house call booking for $date at $time in Johannesburg has been saved. Would you like to proceed to payment?")
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

    private fun isJohannesburgLocation(address: String): Boolean {
        val clean = address.lowercase()
        val jhbKeywords = listOf(
            "johannesburg", "joburg", "jhb", "sandton", "randburg", "midrand",
            "rosebank", "soweto", "roodepoort", "fourways", "braamfontein",
            "kempton park", "alberton", "edenvale", "germiston", "woodmead",
            "sunninghill", "bryanston", "linden", "greenside", "parktown", "melville"
        )
        return jhbKeywords.any { clean.contains(it) }
    }

    private fun showMessage(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("SplashDash Booking")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
