package com.example.splashanddash

import android.os.Bundle

class ManageCustomersActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Manage Customers",
                "View registered customer accounts"
            )
        )

        val customers = listOf(
            "C001 — John Smith — john@example.com",
            "C002 — Sarah Jones — sarah@example.com",
            "C003 — Michael Brown — michael@example.com",
            "C004 — Thandi Mokoena — thandi@example.com"
        )

        customers.forEachIndexed { index, customer ->

            main.addView(
                card(
                    "Customer ${index + 1}",
                    customer
                )
            )
        }

        main.addView(
            outlineButton("BACK TO ADMIN DASHBOARD") {
                go(AdminDashboardActivity::class.java)
            }
        )

        main.addView(footer())

        screen.addView(main)

        setContentView(screen)
    }
}