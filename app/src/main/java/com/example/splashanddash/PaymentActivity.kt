package com.example.splashanddash

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class PaymentActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookings = mutableListOf<AppData.Booking>()
    private lateinit var mainLayout: LinearLayout

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
                    val booking = AppData.Booking(
                        // FIX: Use doc.id (String) to Long directly. Remove toInt() to prevent overflow.
                        id = doc.id.toLongOrNull() ?: 0L,
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
                    mainLayout.addView(card("No Bookings Found", "You have no pending bookings to pay for."))
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
        mainLayout.addView(sectionTitle("Make Payment", "Submit payment details"))

        mainLayout.addView(text("Select Booking", 20f, true))

        val bookingNames = bookings.map { "Booking #${it.id} — ${it.service} — R${it.price}" }
        val bookingSpinner = Spinner(this)
        bookingSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bookingNames)
        mainLayout.addView(bookingSpinner)

        mainLayout.addView(text("Payment Amount", 20f, true))
        val amountInput = input("Amount")
        amountInput.isFocusable = false
        amountInput.setText(bookings.first().price.toString())
        mainLayout.addView(amountInput)

        bookingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                amountInput.setText(bookings[pos].price.toString())
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        mainLayout.addView(text("Payment Method", 20f, true))
        val paymentMethod = Spinner(this)
        paymentMethod.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Select Method", "Cash", "Card", "EFT"))
        mainLayout.addView(paymentMethod)

        mainLayout.addView(button("SUBMIT PAYMENT") {
            val pos = bookingSpinner.selectedItemPosition
            val method = paymentMethod.selectedItem.toString()

            if (method == "Select Method") {
                showAppMessage("Please select a payment method.")
                return@button
            }

            processTransaction(bookings[pos], method)
        })

        mainLayout.addView(outlineButton("BACK TO DASHBOARD") { go(CustomerDashboardActivity::class.java) })
        mainLayout.addView(footer())
    }

    private fun processTransaction(booking: AppData.Booking, method: String) {
        val userId = auth.currentUser?.uid ?: return

        val paymentData = hashMapOf(
            "bookingId" to booking.id,
            "customerId" to userId,
            "amount" to booking.price,
            "method" to method,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "Paid"
        )

        db.collection("payments").add(paymentData).addOnSuccessListener {
            // Update the booking status to "Paid"
            db.collection("bookings").document(booking.id.toString())
                .update("status", "Paid")
                .addOnSuccessListener {
                    showSuccessDialog(booking.id.toString())
                }
                .addOnFailureListener { e ->
                    showAppMessage("Payment recorded, but booking status update failed: ${e.message}")
                }
        }.addOnFailureListener { e ->
            showAppMessage("Payment failed: ${e.message}")
        }
    }

    private fun showSuccessDialog(id: String) {
        AlertDialog.Builder(this@PaymentActivity)
            .setTitle("Success")
            .setMessage("Payment for Booking #$id recorded!")
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
