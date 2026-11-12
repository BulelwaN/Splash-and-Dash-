package com.example.splashanddash

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookings = mutableListOf<AppData.Booking>()
    private lateinit var mainLayout: LinearLayout

    private lateinit var cardDetailsLayout: LinearLayout
    private lateinit var cardNumberInput: EditText
    private lateinit var cardExpiryInput: EditText
    private lateinit var cardCvvInput: EditText

    private lateinit var eftDetailsLayout: LinearLayout
    private lateinit var eftRefInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        mainLayout = column()

        mainLayout.addView(sectionTitle("Make Payment", "Submit payment details for your booking"))

        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            showLoginRequired()
            screen.addView(mainLayout)
            setContentView(screen)
            return
        }

        fetchBookings(firebaseUser.uid)

        screen.addView(mainLayout)
        setContentView(screen)
    }

    private fun fetchBookings(userId: String) {
        db.collection("bookings")
            .whereEqualTo("customerId", userId)
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { documents ->
                bookings.clear()
                for (doc in documents) {
                    val rawId = doc.id.toLongOrNull() ?: System.currentTimeMillis()
                    val booking = AppData.Booking(
                        id = rawId,
                        docId = doc.id,
                        vehicle = doc.getString("vehicle") ?: "",
                        reg = doc.getString("reg") ?: "",
                        service = doc.getString("service") ?: "",
                        price = doc.getLong("price")?.toInt() ?: 0,
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        status = doc.getString("status") ?: "",
                        customerId = doc.getString("customerId")
                    )
                    bookings.add(booking)
                }

                if (bookings.isEmpty()) {
                    mainLayout.removeAllViews()
                    mainLayout.addView(sectionTitle("Make Payment", "Submit payment details"))
                    mainLayout.addView(card("No Bookings Found", "You have no pending bookings requiring payment."))
                    mainLayout.addView(button("BOOK A SERVICE") { go(BookingActivity::class.java) })
                    mainLayout.addView(outlineButton("BACK") { finish() })
                } else {
                    renderPaymentUI()
                }
            }
            .addOnFailureListener { e ->
                showAppMessage("Error loading bookings: ${e.message}")
            }
    }

    private fun renderPaymentUI() {
        mainLayout.removeAllViews()
        mainLayout.addView(sectionTitle("Make Payment", "Submit payment details for your booking"))

        mainLayout.addView(text("Select Pending Booking", 18f, true))

        val bookingNames = bookings.map { "Booking #${it.docId.take(8)} — ${it.service} (R${it.price})" }
        val bookingSpinner = Spinner(this)
        bookingSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bookingNames)
        mainLayout.addView(bookingSpinner)

        mainLayout.addView(text("Payment Amount (ZAR)", 18f, true))
        val amountInput = input("Amount")
        amountInput.isFocusable = false
        amountInput.isFocusableInTouchMode = false
        amountInput.setText("R ${bookings.first().price}")
        mainLayout.addView(amountInput)

        bookingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                amountInput.setText("R ${bookings[pos].price}")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        mainLayout.addView(text("Payment Method", 18f, true))
        val paymentMethod = Spinner(this)
        paymentMethod.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Select Method", "Card", "EFT", "Cash on Delivery")
        )
        mainLayout.addView(paymentMethod)

        // Card details view container
        cardDetailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        cardNumberInput = input("Card Number (16 digits)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        cardExpiryInput = input("Expiry Date (MM/YY)").apply {
            inputType = android.text.InputType.TYPE_CLASS_DATETIME
        }
        cardCvvInput = input("CVV (3 digits)").apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        cardDetailsLayout.addView(text("Card Information", 16f, true))
        cardDetailsLayout.addView(cardNumberInput)
        cardDetailsLayout.addView(cardExpiryInput)
        cardDetailsLayout.addView(cardCvvInput)
        mainLayout.addView(cardDetailsLayout)

        // EFT details view container
        eftDetailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        eftRefInput = input("EFT Reference Number")
        eftDetailsLayout.addView(text("EFT Information", 16f, true))
        eftDetailsLayout.addView(eftRefInput)
        mainLayout.addView(eftDetailsLayout)

        paymentMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                when (paymentMethod.selectedItem.toString()) {
                    "Card" -> {
                        cardDetailsLayout.visibility = View.VISIBLE
                        eftDetailsLayout.visibility = View.GONE
                    }
                    "EFT" -> {
                        cardDetailsLayout.visibility = View.GONE
                        eftDetailsLayout.visibility = View.VISIBLE
                    }
                    else -> {
                        cardDetailsLayout.visibility = View.GONE
                        eftDetailsLayout.visibility = View.GONE
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        mainLayout.addView(button("SUBMIT PAYMENT") {
            val pos = bookingSpinner.selectedItemPosition
            if (pos < 0 || pos >= bookings.size) return@button

            val selectedBooking = bookings[pos]
            val method = paymentMethod.selectedItem.toString()

            if (method == "Select Method") {
                showAppMessage("Please select a valid payment method.")
                return@button
            }

            if (method == "Card") {
                val cardNum = cardNumberInput.text.toString().trim().replace(" ", "")
                val expiry = cardExpiryInput.text.toString().trim()
                val cvv = cardCvvInput.text.toString().trim()

                if (cardNum.length < 13 || cardNum.length > 19 || !cardNum.all { it.isDigit() }) {
                    showAppMessage("Please enter a valid card number (13-19 digits).")
                    return@button
                }
                if (!expiry.matches(Regex("^(0[1-9]|1[0-2])/([0-9]{2})$"))) {
                    showAppMessage("Please enter a valid expiry date in MM/YY format.")
                    return@button
                }
                if (cvv.length < 3 || cvv.length > 4 || !cvv.all { it.isDigit() }) {
                    showAppMessage("Please enter a valid 3 or 4 digit CVV.")
                    return@button
                }
            } else if (method == "EFT") {
                val ref = eftRefInput.text.toString().trim()
                if (ref.length < 4) {
                    showAppMessage("Please enter a valid EFT reference number.")
                    return@button
                }
            }

            processTransaction(selectedBooking, method)
        })

        mainLayout.addView(outlineButton("BACK TO DASHBOARD") { go(CustomerDashboardActivity::class.java); finish() })
        mainLayout.addView(footer())
    }

    private fun processTransaction(booking: AppData.Booking, method: String) {
        val userId = auth.currentUser?.uid ?: return
        val docIdToUpdate = if (booking.docId.isNotBlank()) booking.docId else booking.id.toString()

        val paymentData = hashMapOf(
            "bookingId" to booking.id,
            "bookingDocId" to docIdToUpdate,
            "customerId" to userId,
            "amount" to booking.price,
            "method" to method,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "Paid"
        )

        db.collection("payments").add(paymentData).addOnSuccessListener {
            // Update the booking status to "Paid" in Firestore
            db.collection("bookings").document(docIdToUpdate)
                .update("status", "Paid")
                .addOnSuccessListener {
                    showSuccessDialog(docIdToUpdate)
                }
                .addOnFailureListener { e ->
                    showAppMessage("Payment recorded, but booking status update failed: ${e.message}")
                }
        }.addOnFailureListener { e ->
            showAppMessage("Payment failed: ${e.message}")
        }
    }

    private fun showSuccessDialog(docId: String) {
        AlertDialog.Builder(this@PaymentActivity)
            .setTitle("Payment Successful")
            .setMessage("Your payment for Booking #${docId.take(8)} has been successfully processed and updated to Paid.")
            .setPositiveButton("OK") { _, _ ->
                go(CustomerDashboardActivity::class.java)
                finish()
            }.show()
    }

    private fun showLoginRequired() {
        mainLayout.addView(card("Sign In Required", "Please sign in to access your payments."))
        mainLayout.addView(button("LOGIN") { go(LoginActivity::class.java); finish() })
    }
}
