package com.example.splashanddash

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PaymentActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookings = mutableListOf<AppData.Booking>()

    private lateinit var mainLayout: LinearLayout
    private lateinit var eftDetailsLayout: LinearLayout
    private lateinit var eftRefInput: EditText

    private var isSubmittingPayment = false

    companion object {
        private const val API_BASE_URL =
            "https://giver-pebble-exemplify.ngrok-free.dev"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        mainLayout = column()

        mainLayout.addView(
            sectionTitle("Make Payment", "Choose how you would like to pay")
        )

        val user = auth.currentUser

        if (user == null) {
            showLoginRequired()
            screen.addView(mainLayout)
            setContentView(screen)
            return
        }

        screen.addView(mainLayout)
        setContentView(screen)

        fetchBookings(user.uid)
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
                        id = doc.getLong("bookingId")
                            ?: System.currentTimeMillis(),
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

                    mainLayout.addView(
                        sectionTitle("Make Payment", "Your bookings")
                    )

                    mainLayout.addView(
                        card(
                            "No Pending Bookings",
                            "You currently have no pending bookings requiring payment."
                        )
                    )

                    mainLayout.addView(button("BOOK A SERVICE") {
                        go(BookingActivity::class.java)
                    })

                    mainLayout.addView(
                        outlineButton("BACK") { finish() }
                    )
                } else {
                    renderPaymentUI()
                }
            }
            .addOnFailureListener { e ->
                showAppMessage("Unable to load bookings: ${e.message}")
            }
    }

    private fun renderPaymentUI() {
        mainLayout.removeAllViews()

        mainLayout.addView(
            sectionTitle("Make Payment", "Select a booking and payment method")
        )

        mainLayout.addView(text("Select Booking", 18f, true))

        val bookingNames = bookings.map {
            "Booking #${it.docId.take(8)} — ${it.service} (R${it.price})"
        }

        val bookingSpinner = Spinner(this)

        bookingSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            bookingNames
        )

        mainLayout.addView(bookingSpinner)

        mainLayout.addView(text("Payment Amount", 18f, true))

        val amountInput = input("Amount")

        amountInput.isFocusable = false
        amountInput.isFocusableInTouchMode = false
        amountInput.setText("R ${bookings.first().price}")

        mainLayout.addView(amountInput)

        bookingSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position in bookings.indices) {
                        amountInput.setText("R ${bookings[position].price}")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        mainLayout.addView(text("Payment Method", 18f, true))

        val paymentMethod = Spinner(this)

        paymentMethod.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "Select Method",
                "Card",
                "EFT",
                "Cash on Delivery"
            )
        )

        mainLayout.addView(paymentMethod)

        eftDetailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        eftRefInput = input("EFT Reference Number")

        eftDetailsLayout.addView(
            text("EFT Information", 16f, true)
        )

        eftDetailsLayout.addView(eftRefInput)
        mainLayout.addView(eftDetailsLayout)

        paymentMethod.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    eftDetailsLayout.visibility =
                        if (paymentMethod.selectedItem.toString() == "EFT") {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        mainLayout.addView(button("SUBMIT PAYMENT") {

            if (isSubmittingPayment) {
                return@button
            }

            val bookingPosition = bookingSpinner.selectedItemPosition

            if (bookingPosition !in bookings.indices) {
                showAppMessage("Please select a valid booking.")
                return@button
            }

            val selectedBooking = bookings[bookingPosition]
            val method = paymentMethod.selectedItem.toString()

            when (method) {

                "Select Method" -> {
                    showAppMessage("Please select a payment method.")
                }

                "Card" -> {
                    startPayFastCheckout(selectedBooking)
                }

                "EFT" -> {
                    val reference = eftRefInput.text.toString().trim()

                    if (reference.length < 4) {
                        showAppMessage(
                            "Please enter a valid EFT reference number."
                        )
                        return@button
                    }

                    submitPayment(
                        selectedBooking,
                        "EFT",
                        "Awaiting Verification",
                        reference
                    )
                }

                "Cash on Delivery" -> {
                    submitPayment(
                        selectedBooking,
                        "Cash on Delivery",
                        "Awaiting Collection",
                        null
                    )
                }
            }
        })

        mainLayout.addView(
            outlineButton("BACK TO DASHBOARD") {
                go(CustomerDashboardActivity::class.java)
                finish()
            }
        )

        mainLayout.addView(footer())
    }

    private fun startPayFastCheckout(booking: AppData.Booking) {

        if (isSubmittingPayment) return

        val user = auth.currentUser

        if (user == null) {
            showLoginRequired()
            return
        }

        if (booking.docId.isBlank()) {
            showAppMessage("The booking reference is invalid.")
            return
        }

        isSubmittingPayment = true

        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->

                val idToken = tokenResult.token

                if (idToken.isNullOrBlank()) {
                    isSubmittingPayment = false
                    showAppMessage("Unable to authenticate your account.")
                    return@addOnSuccessListener
                }

                Thread {
                    var connection: HttpURLConnection? = null

                    try {
                        val url = URL(
                            "$API_BASE_URL/api/payments/payfast/checkout"
                        )

                        connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 15000
                        connection.readTimeout = 20000
                        connection.doOutput = true
                        connection.setRequestProperty(
                            "Content-Type",
                            "application/json"
                        )
                        connection.setRequestProperty(
                            "ngrok-skip-browser-warning",
                            "true"
                        )
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $idToken"
                        )

                        val requestBody = JSONObject()
                            .put("bookingDocId", booking.docId)
                            .toString()

                        connection.outputStream.use { output ->
                            output.write(requestBody.toByteArray(Charsets.UTF_8))
                        }

                        val responseCode = connection.responseCode

                        val responseStream =
                            if (responseCode in 200..299) {
                                connection.inputStream
                            } else {
                                connection.errorStream
                            }

                        val responseText = responseStream
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            .orEmpty()

                        if (responseCode !in 200..299) {
                            throw Exception(
                                "Checkout request failed ($responseCode): $responseText"
                            )
                        }

                        val responseJson = JSONObject(responseText)
                        val processUrl = responseJson.getString("processUrl")
                        val fields = responseJson.getJSONObject("fields")

                        runOnUiThread {
                            isSubmittingPayment = false
                            showPayFastCheckout(processUrl, fields)
                        }

                    } catch (e: Exception) {

                        runOnUiThread {
                            isSubmittingPayment = false
                            showAppMessage(
                                "Unable to start card payment: ${e.message}"
                            )
                        }

                    } finally {
                        connection?.disconnect()
                    }

                }.start()
            }
            .addOnFailureListener { error ->
                isSubmittingPayment = false
                showAppMessage(
                    "Unable to authenticate your account: ${error.message}"
                )
            }
    }

    private fun showPayFastCheckout(
        processUrl: String,
        fields: JSONObject
    ) {

        val checkoutUri = android.net.Uri.parse(processUrl)

        if (checkoutUri.scheme != "https" ||
            checkoutUri.host?.endsWith("payfast.co.za") != true
        ) {
            showAppMessage("The payment gateway address is invalid.")
            return
        }

        val html = buildString {
            append(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Secure PayFast Checkout</title>
                </head>
                <body>
                    <p>Connecting to PayFast secure checkout...</p>
                    <form id="payfastForm" method="post"
                          action="${escapeHtml(processUrl)}">
                """.trimIndent()
            )

            val keys = fields.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val value = fields.opt(key)?.toString() ?: ""

                append(
                    """<input type="hidden" name="${escapeHtml(key)}" value="${escapeHtml(value)}">"""
                )
            }

            append(
                """
                    </form>
                    <script>
                        document.getElementById('payfastForm').submit();
                    </script>
                </body>
                </html>
                """.trimIndent()
            )
        }

        mainLayout.removeAllViews()

        mainLayout.addView(
            sectionTitle(
                "Secure Card Payment",
                "Complete your payment on PayFast"
            )
        )

        mainLayout.addView(
            text(
                "Your card details are entered securely on PayFast. " +
                        "They are not collected by this app.",
                14f,
                false
            )
        )

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }

        val height = (560 * resources.displayMetrics.density).toInt()

        mainLayout.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        )

        mainLayout.addView(
            outlineButton("CLOSE CHECKOUT") {
                webView.stopLoading()
                webView.destroy()
                renderPaymentUI()
            }
        )

        webView.loadDataWithBaseURL(
            processUrl,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun submitPayment(
        booking: AppData.Booking,
        method: String,
        paymentStatus: String,
        eftReference: String?
    ) {
        if (isSubmittingPayment) return

        val userId = auth.currentUser?.uid

        if (userId == null) {
            showLoginRequired()
            return
        }

        val bookingDocId = booking.docId

        if (bookingDocId.isBlank()) {
            showAppMessage("The booking reference is invalid.")
            return
        }

        isSubmittingPayment = true

        val paymentData = hashMapOf<String, Any>(
            "bookingId" to booking.id,
            "bookingDocId" to bookingDocId,
            "customerId" to userId,
            "amount" to booking.price,
            "method" to method,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to paymentStatus
        )

        if (!eftReference.isNullOrBlank()) {
            paymentData["eftReference"] = eftReference
        }

        val bookingReference = db.collection("bookings")
            .document(bookingDocId)

        val paymentReference = db.collection("payments")
            .document(bookingDocId)

        db.runBatch { batch ->

            batch.set(paymentReference, paymentData)

            batch.update(
                bookingReference,
                "paymentStatus",
                paymentStatus
            )

            batch.update(
                bookingReference,
                "paymentMethod",
                method
            )

        }.addOnSuccessListener {

            isSubmittingPayment = false

            showPaymentStatusDialog(
                bookingDocId,
                method,
                paymentStatus
            )

        }.addOnFailureListener { error ->

            isSubmittingPayment = false

            showAppMessage(
                "Unable to submit payment details: ${error.message}"
            )
        }
    }

    private fun showPaymentStatusDialog(
        bookingDocId: String,
        method: String,
        paymentStatus: String
    ) {
        val message = when (method) {

            "EFT" ->
                "Your EFT reference has been submitted. " +
                        "Your payment is awaiting verification. " +
                        "Your booking has not been marked as paid."

            "Cash on Delivery" ->
                "Your Cash on Delivery option has been recorded. " +
                        "Payment is awaiting collection. " +
                        "Your booking has not been marked as paid."

            else ->
                "Your payment status is $paymentStatus."
        }

        AlertDialog.Builder(this)
            .setTitle("Payment Submitted")
            .setMessage(
                "$message\n\nBooking #${bookingDocId.take(8)}"
            )
            .setPositiveButton("OK") { _, _ ->
                go(CustomerDashboardActivity::class.java)
                finish()
            }
            .show()
    }

    private fun showLoginRequired() {
        mainLayout.addView(
            card(
                "Sign In Required",
                "Please sign in to access your payments."
            )
        )

        mainLayout.addView(button("LOGIN") {
            go(LoginActivity::class.java)
            finish()
        })
    }
}



