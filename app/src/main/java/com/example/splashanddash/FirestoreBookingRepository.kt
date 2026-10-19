package com.example.splashanddash

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

data class OnlineBooking(
    val id: Long = 0,
    val firestoreId: String = "",
    val customerId: String = "",
    val vehicle: String = "",
    val reg: String = "",
    val service: String = "",
    val price: Int = 0,
    val date: String = "",
    val time: String = "",
    val status: String = "Pending"
)

object FirestoreBookingRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun createBooking(
        vehicle: String,
        reg: String,
        service: String,
        date: String,
        time: String,
        price: Int,
        status: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(false, "Please sign in first.")
            return
        }

        // Generate a numeric ID based on time
        val numericId = System.currentTimeMillis()

        // REMOVED "id" field to satisfy Firestore "hasOnly" rule
        val bookingData = hashMapOf(
            "customerId" to user.uid,
            "vehicle" to vehicle,
            "reg" to reg,
            "service" to service,
            "price" to price,
            "date" to date,
            "time" to time,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("bookings")
            .document(numericId.toString())
            .set(bookingData)
            .addOnSuccessListener {
                onResult(true, "Booking saved successfully.")
            }
            .addOnFailureListener { exception ->
                onResult(
                    false,
                    exception.localizedMessage ?: "Unable to save booking."
                )
            }
    }

    fun getCustomerBookings(
        onResult: (List<OnlineBooking>?, String?) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(null, "Please sign in first.")
            return
        }

        db.collection("bookings")
            .whereEqualTo("customerId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val bookings = snapshot.documents.mapNotNull { doc ->
                    val customerId = doc.getString("customerId") ?: return@mapNotNull null
                    val vehicle = doc.getString("vehicle") ?: return@mapNotNull null
                    val reg = doc.getString("reg") ?: return@mapNotNull null
                    val service = doc.getString("service") ?: return@mapNotNull null
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val time = doc.getString("time") ?: return@mapNotNull null

                    OnlineBooking(
                        // Retrieve the numeric ID from the document name itself
                        id = doc.id.toLongOrNull() ?: 0L,
                        firestoreId = doc.id,
                        customerId = customerId,
                        vehicle = vehicle,
                        reg = reg,
                        service = service,
                        price = doc.getLong("price")?.toInt() ?: 0,
                        date = date,
                        time = time,
                        status = doc.getString("status") ?: "Pending"
                    )
                }.sortedWith(
                    compareByDescending<OnlineBooking> { it.date }
                        .thenByDescending { it.time }
                )

                onResult(bookings, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load bookings."
                )
            }
    }
}
