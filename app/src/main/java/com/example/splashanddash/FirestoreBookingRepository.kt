package com.example.splashanddash

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
    val location: String = "",
    val status: String = "Pending"
)

object FirestoreBookingRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val manageableStatuses = setOf("pending", "confirmed")

    // CREATE BOOKING
    fun createBooking(
        vehicle: String,
        reg: String,
        service: String,
        date: String,
        time: String,
        price: Int,
        status: String,
        location: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(false, "Please sign in first.")
            return
        }

        if (vehicle.isBlank() || reg.isBlank() ||
            service.isBlank() || date.isBlank() ||
            time.isBlank() || location.isBlank() || price <= 0
        ) {
            onResult(false, "Please provide all required booking details.")
            return
        }

        val document = db.collection("bookings").document()
        val numericId = System.currentTimeMillis()

        val bookingData = hashMapOf(
            "bookingId" to numericId,
            "customerId" to user.uid,
            "vehicle" to vehicle,
            "reg" to reg,
            "service" to service,
            "price" to price,
            "date" to date,
            "time" to time,
            "location" to location,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )

        document.set(bookingData)
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

    // GET BOOKINGS FOR CURRENT CUSTOMER
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

                    val customerId =
                        doc.getString("customerId") ?: return@mapNotNull null
                    val vehicle =
                        doc.getString("vehicle") ?: return@mapNotNull null
                    val reg =
                        doc.getString("reg") ?: return@mapNotNull null
                    val service =
                        doc.getString("service") ?: return@mapNotNull null
                    val date =
                        doc.getString("date") ?: return@mapNotNull null
                    val time =
                        doc.getString("time") ?: return@mapNotNull null

                    OnlineBooking(
                        id = doc.getLong("bookingId")
                            ?: doc.id.toLongOrNull()
                            ?: 0L,
                        firestoreId = doc.id,
                        customerId = customerId,
                        vehicle = vehicle,
                        reg = reg,
                        service = service,
                        price = doc.getLong("price")?.toInt() ?: 0,
                        date = date,
                        time = time,
                        location = doc.getString("location") ?: "",
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

    // UPDATE AN EXISTING BOOKING

    fun updateBooking(
        firestoreId: String,
        vehicle: String,
        reg: String,
        service: String,
        date: String,
        time: String,
        price: Int,
        location: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(false, "Please sign in first.")
            return
        }

        if (firestoreId.isBlank() || vehicle.isBlank() ||
            reg.isBlank() || service.isBlank() ||
            date.isBlank() || time.isBlank() ||
            location.isBlank() || price <= 0
        ) {
            onResult(false, "Please provide all required booking details.")
            return
        }

        val document = db.collection("bookings").document(firestoreId)

        document.get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    onResult(false, "Booking could not be found.")
                    return@addOnSuccessListener
                }

                if (snapshot.getString("customerId") != user.uid) {
                    onResult(false, "You cannot edit another customer's booking.")
                    return@addOnSuccessListener
                }

                val status = snapshot.getString("status")?.lowercase() ?: ""

                if (status !in manageableStatuses) {
                    onResult(false, "Only pending or confirmed bookings can be edited.")
                    return@addOnSuccessListener
                }

                if (!snapshot.getString("paymentStatus").isNullOrBlank()) {
                    onResult(false, "This booking cannot be edited because a payment has already been submitted.")
                    return@addOnSuccessListener
                }

                // Check whether a payment record already exists.
                db.collection("payments")
                    .document(firestoreId)
                    .get()
                    .addOnSuccessListener { paymentSnapshot ->

                        if (paymentSnapshot.exists()) {
                            onResult(false, "This booking cannot be edited because a payment has already been submitted.")
                            return@addOnSuccessListener
                        }

                        val updates = mapOf(
                            "vehicle" to vehicle,
                            "reg" to reg,
                            "service" to service,
                            "date" to date,
                            "time" to time,
                            "location" to location,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )

                        // Price is intentionally excluded.
                        // Customers cannot change the booking price.

                        document.update(updates)
                            .addOnSuccessListener {
                                onResult(true, "Booking updated successfully.")
                            }
                            .addOnFailureListener { exception ->
                                onResult(
                                    false,
                                    exception.localizedMessage ?: "Unable to update booking."
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        onResult(
                            false,
                            exception.localizedMessage ?: "Unable to verify payment status."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onResult(
                    false,
                    exception.localizedMessage ?: "Unable to verify booking."
                )
            }
    }
    // CANCEL AN EXISTING BOOKING
    fun cancelBooking(
        firestoreId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(false, "Please sign in first.")
            return
        }

        if (firestoreId.isBlank()) {
            onResult(false, "Invalid booking reference.")
            return
        }

        val document = db.collection("bookings").document(firestoreId)

        document.get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    onResult(false, "Booking could not be found.")
                    return@addOnSuccessListener
                }

                if (snapshot.getString("customerId") != user.uid) {
                    onResult(false, "You cannot cancel another customer's booking.")
                    return@addOnSuccessListener
                }

                val status = snapshot.getString("status")?.lowercase() ?: ""

                if (status !in manageableStatuses) {
                    onResult(false, "Only pending or confirmed bookings can be cancelled.")
                    return@addOnSuccessListener
                }

                if (!snapshot.getString("paymentStatus").isNullOrBlank()) {
                    onResult(false, "This booking cannot be cancelled because a payment has already been submitted.")
                    return@addOnSuccessListener
                }

                // Check whether a payment record already exists.
                db.collection("payments")
                    .document(firestoreId)
                    .get()
                    .addOnSuccessListener { paymentSnapshot ->

                        if (paymentSnapshot.exists()) {
                            onResult(false, "This booking cannot be cancelled because a payment has already been submitted.")
                            return@addOnSuccessListener
                        }

                        val updates = mapOf(
                            "status" to "Cancelled",
                            "cancelledAt" to FieldValue.serverTimestamp()
                        )

                        document.update(updates)
                            .addOnSuccessListener {
                                onResult(true, "Booking cancelled successfully.")
                            }
                            .addOnFailureListener { exception ->
                                onResult(
                                    false,
                                    exception.localizedMessage ?: "Unable to cancel booking."
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        onResult(
                            false,
                            exception.localizedMessage ?: "Unable to verify payment status."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onResult(
                    false,
                    exception.localizedMessage ?: "Unable to verify booking."
                )
            }
    }
}


