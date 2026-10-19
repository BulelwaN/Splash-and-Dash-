package com.example.splashanddash

import android.os.Bundle
import android.widget.Toast

class ManageStaffActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = content()
        val main = column()

        main.addView(
            sectionTitle(
                "Manage Staff",
                "Add and view Splash & Dash staff members"
            )
        )

        // ---------------------------------------------------------
        // ADD STAFF
        // ---------------------------------------------------------

        main.addView(
            text(
                "Add Staff Member",
                20f,
                true
            )
        )

        val name = input(
            "Staff Name"
        )

        val role = input(
            "Staff Role"
        )

        val phone = input(
            "Contact Number"
        )

        main.addView(name)
        main.addView(role)
        main.addView(phone)

        main.addView(
            button("ADD STAFF") {

                if (
                    name.text.isBlank() ||
                    role.text.isBlank() ||
                    phone.text.isBlank()
                ) {
                    showAppMessage("Please complete all staff details.")

                    return@button
                }

                Toast.makeText(
                    this,
                    "${name.text} added successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                name.text.clear()
                role.text.clear()
                phone.text.clear()
            }
        )

        // ---------------------------------------------------------
        // STAFF LIST
        // ---------------------------------------------------------

        main.addView(
            sectionTitle(
                "Staff Members",
                "Current active team members"
            )
        )

        val staffMembers = listOf(
            "S001 — Alex Dlamini — Washer — 071 111 1111",
            "S002 — Lerato Nkosi — Supervisor — 072 222 2222",
            "S003 — James Naidoo — Washer — 073 333 3333"
        )

        staffMembers.forEach { staff ->

            main.addView(
                card(
                    "Active Staff Member",
                    staff
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