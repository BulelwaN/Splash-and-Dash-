package com.example.splashanddash

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteConstraintException
import android.util.Patterns
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AccountStore(context: Context) :
    SQLiteOpenHelper(context, "splashdash_accounts.db", null, 1) {

    data class Account(
        val id: Long,
        val name: String,
        val email: String,
        val role: String
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                role TEXT NOT NULL
                    CHECK(role IN ('Customer', 'Staff', 'Admin')),
                salt BLOB NOT NULL,
                password_hash BLOB NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Future database migrations go here.
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        address: String,
        password: String
    ): Account {

        val cleanEmail = email.trim().lowercase()

        require(name.trim().length >= 2) {
            "Enter your full name."
        }

        require(
            Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
        ) {
            "Enter a valid email address."
        }

        require(phone.trim().length >= 7) {
            "Enter a valid phone number."
        }

        require(
            password.length >= 10 &&
                    password.any(Char::isDigit) &&
                    password.any(Char::isLetter)
        ) {
            "Password must contain at least 10 characters, " +
                    "including letters and numbers."
        }

        val salt = ByteArray(16).also {
            SecureRandom().nextBytes(it)
        }

        val values = ContentValues().apply {
            put("name", name.trim())
            put("email", cleanEmail)
            put("phone", phone.trim())
            put("address", address.trim())
            put("role", "Customer")
            put("salt", salt)
            put("password_hash", hash(password, salt))
        }

        val id = try {
            writableDatabase.insertOrThrow(
                "accounts",
                null,
                values
            )
        } catch (e: SQLiteConstraintException) {
            throw IllegalArgumentException(
                "This email is already registered."
            )
        }

        return Account(
            id = id,
            name = name.trim(),
            email = cleanEmail,
            role = "Customer"
        )
    }

    fun authenticate(
        email: String,
        password: String
    ): Account? {

        readableDatabase.query(
            "accounts",
            arrayOf(
                "id",
                "name",
                "email",
                "role",
                "salt",
                "password_hash"
            ),
            "email = ?",
            arrayOf(email.trim().lowercase()),
            null,
            null,
            null
        ).use { cursor ->

            if (!cursor.moveToFirst()) {
                return null
            }

            val salt = cursor.getBlob(4)
            val actualHash = hash(password, salt)
            val storedHash = cursor.getBlob(5)

            if (!MessageDigest.isEqual(actualHash, storedHash)) {
                return null
            }

            return Account(
                id = cursor.getLong(0),
                name = cursor.getString(1),
                email = cursor.getString(2),
                role = cursor.getString(3)
            )
        }
    }

    private fun hash(
        password: String,
        salt: ByteArray
    ): ByteArray {

        val algorithm =
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                "PBKDF2WithHmacSHA256"
            } else {
                "PBKDF2WithHmacSHA1"
            }

        val specification = PBEKeySpec(
            password.toCharArray(),
            salt,
            120_000,
            256
        )

        return try {

            SecretKeyFactory.getInstance(algorithm)
                .generateSecret(specification)
                .encoded

        } finally {
            specification.clearPassword()
        }
    }
}