package com.example.splashanddash

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    // ---------------------------------------------------------
    // Shared Colours
    // ---------------------------------------------------------

    protected val navy: Int
        get() = getColor(R.color.navy)

    protected val blue: Int
        get() = getColor(R.color.blue)

    protected val brightBlue: Int
        get() = getColor(R.color.bright_blue)

    protected val backgroundColor: Int
        get() = getColor(R.color.background)

    protected val white: Int
        get() = getColor(R.color.white)

    protected val textColor: Int
        get() = getColor(R.color.text)

    protected val muted: Int
        get() = getColor(R.color.muted)

    protected val border: Int
        get() = getColor(R.color.border)

    protected val success: Int
        get() = getColor(R.color.success)

    protected val warning: Int
        get() = getColor(R.color.warning)

    protected val danger: Int
        get() = getColor(R.color.danger)


    // ---------------------------------------------------------
    // Activity Setup
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = blue
        window.navigationBarColor = navy
    }

    override fun onStart() {
        super.onStart()

        val requiredRole = when (this) {

            is AdminDashboardActivity,
            is ManageBookingsActivity,
            is ManageCustomersActivity,
            is ManageStaffActivity,
            is ReportsActivity -> "Admin"

            is StaffDashboardActivity,
            is ServiceQueueActivity -> "Staff"

            is CustomerDashboardActivity,
            is BookingActivity,
            is BookingHistoryActivity,
            is PaymentActivity -> "Customer"

            else -> null
        }

        if (requiredRole == null) {
            return
        }

        // Check the Firebase authenticated user.
        val firebaseUser =
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser

        if (firebaseUser == null) {

            android.util.Log.d(
                "SplashDashAuth",
                "No Firebase session. Redirecting to Login."
            )

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )

            finish()
            return
        }

        // Check the role loaded from Firestore.
        if (AppData.role != requiredRole) {

            android.util.Log.w(
                "SplashDashAuth",
                "Role mismatch: required=$requiredRole, " +
                        "current=${AppData.role}"
            )

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )

            finish()
            return
        }

        android.util.Log.d(
            "SplashDashAuth",
            "Access granted to ${javaClass.simpleName}"
        )
    }


    // ---------------------------------------------------------
    // Screen Container Helpers
    // ---------------------------------------------------------

    open fun createScreen(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
    }

    protected fun content(): android.widget.ScrollView {
        return android.widget.ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
        }
    }

    protected fun column(
        padding: Int = 20
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (padding > 0) {
                setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            }
        }
    }


    // ---------------------------------------------------------
    // Navigation Bar
    // ---------------------------------------------------------

    protected fun navBar(): LinearLayout {

        val navigation = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
            )

            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    navy,
                    blue
                )
            ).apply {

                cornerRadius =
                    dp(18).toFloat()
            }
        }

        // ---------------------------------------------------------
        // APP NAME
        // ---------------------------------------------------------

        val logo = TextView(this).apply {

            text = "SplashDash"

            textSize = 19f

            setTextColor(Color.WHITE)

            typeface =
                Typeface.DEFAULT_BOLD

            gravity =
                Gravity.CENTER_VERTICAL
        }

        navigation.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // ---------------------------------------------------------
        // SETTINGS ONLY
        // ---------------------------------------------------------

        val settingsButton =
            navButton("⚙ Settings")

        settingsButton.setOnClickListener {

            go(
                SettingsActivity::class.java
            )
        }

        navigation.addView(
            settingsButton
        )

        return navigation
    }

    protected fun navBar(title: String, subtitle: String? = null): LinearLayout {
        return navBar()
    }

    private fun navButton(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            isFocusable = true
            alpha = 0.95f
        }
    }


    // ---------------------------------------------------------
    // Navigation Helper
    // ---------------------------------------------------------

    protected fun go(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
    }


    // ---------------------------------------------------------
    // Text Helper
    // ---------------------------------------------------------

    protected fun text(
        value: String,
        size: Float = 16f,
        isBold: Boolean = false,
        color: Int = textColor
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (isBold) {
                typeface = Typeface.DEFAULT_BOLD
            }
            setPadding(0, dp(4), 0, dp(4))
        }
    }


    // ---------------------------------------------------------
    // Page Header & Section Title
    // ---------------------------------------------------------

    protected fun pageHeader(
        title: String,
        subtitle: String? = null
    ): LinearLayout {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(22), dp(4), dp(18))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 28f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(titleView)

        if (!subtitle.isNullOrBlank()) {
            val subtitleView = TextView(this).apply {
                text = subtitle
                textSize = 15f
                setTextColor(muted)
                setPadding(0, dp(6), 0, 0)
            }
            header.addView(subtitleView)
        }

        return header
    }

    open fun sectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(brightBlue)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(10))
        }
    }

    protected fun sectionTitle(title: String, subtitle: String?): View {
        return pageHeader(title, subtitle)
    }


    // ---------------------------------------------------------
    // Card Components
    // ---------------------------------------------------------

    protected fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundResource(R.drawable.rounded_card)
        }
    }

    protected fun card(
        title: String,
        body: String,
        actionLabel: String? = null,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val container = card().apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(6), 0, dp(12))
            }
            layoutParams = params
        }

        container.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
        })

        container.addView(TextView(this).apply {
            text = body
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(6), 0, dp(6))
        })

        if (actionLabel != null || onClick != null) {
            val btnLabel = actionLabel ?: "Open"
            container.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                addView(TextView(this@BaseActivity).apply {
                    text = btnLabel
                    textSize = 14f
                    setTextColor(brightBlue)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                })
            })
            if (onClick != null) {
                container.setOnClickListener { onClick() }
                container.isClickable = true
                container.isFocusable = true
            }
        }

        return container
    }

    protected fun metricCard(
        title: String,
        value: String
    ): LinearLayout {
        val card = card().apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(6), 0, dp(12))
            }
            layoutParams = params
        }

        val valueView = TextView(this).apply {
            text = value
            textSize = 28f
            setTextColor(brightBlue)
            typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(valueView)

        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(5), 0, 0)
        }
        card.addView(titleView)

        return card
    }


    // ---------------------------------------------------------
    // Buttons & Inputs
    // ---------------------------------------------------------

    protected fun primaryButton(title: String): Button {
        return Button(this).apply {
            text = title
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(brightBlue)
                cornerRadius = dp(14).toFloat()
            }
            isAllCaps = false
            stateListAnimator = null
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
    }

    protected fun button(title: String, onClick: () -> Unit): Button {
        return primaryButton(title).apply {
            setOnClickListener { onClick() }
        }
    }

    protected fun outlineButton(title: String): Button {
        return Button(this).apply {
            text = title
            textSize = 15f
            setTextColor(brightBlue)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), brightBlue)
                cornerRadius = dp(14).toFloat()
            }
            isAllCaps = false
            stateListAnimator = null
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
    }

    protected fun outlineButton(title: String, onClick: () -> Unit): Button {
        return outlineButton(title).apply {
            setOnClickListener { onClick() }
        }
    }

    protected fun input(hint: String): android.widget.EditText {
        return android.widget.EditText(this).apply {
            setHint(hint)
            textSize = 16f
            setTextColor(textColor)
            setHintTextColor(muted)
            styleInput(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(6), 0, dp(12))
            }
            layoutParams = params
        }
    }

    protected fun styleInput(view: View) {
        view.setBackgroundResource(R.drawable.edit_bg)
        view.setPadding(dp(14), dp(12), dp(14), dp(12))
    }


    // ---------------------------------------------------------
    // Spacing & Footer Helpers
    // ---------------------------------------------------------

    protected fun addSpacing(parent: LinearLayout, height: Int = 12) {
        val spacer = View(this)
        parent.addView(
            spacer,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height))
        )
    }

    protected fun addVerticalSpace(height: Int = 12): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(height)
            )
        }
    }

    protected fun addVerticalSpace(parent: LinearLayout, height: Int) {
        addSpacing(parent, height)
    }

    protected fun footer(): TextView {
        return TextView(this).apply {
            text = "© 2026 SplashDash Car Wash"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(10))
        }
    }

    protected fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    protected fun showAppMessage(
        message: String,
        title: String = "SplashDash"
    ) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
