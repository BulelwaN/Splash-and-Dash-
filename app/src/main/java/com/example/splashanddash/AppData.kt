package com.example.splashanddash

object AppData {

    data class Booking(
        var id: Long, // Changed from Int to Long to prevent overflow
        var vehicle: String,
        var reg: String,
        var service: String,
        var price: Int,
        var date: String,
        var time: String,
        var status: String,
        var customerId: String? = null
    )

    val bookings = mutableListOf<Booking>()

    var currentUser = "Guest"
    var currentUserId: String? = null
    var role = "Customer"

    fun signIn(account: AccountStore.Account) {
        currentUserId = account.id.toString()
        currentUser = account.name
        role = account.role
    }

    fun signOut() {
        currentUserId = null
        currentUser = "Guest"
        role = "Customer"
    }

    fun nextId(): Long {
        return System.currentTimeMillis()
    }
}
