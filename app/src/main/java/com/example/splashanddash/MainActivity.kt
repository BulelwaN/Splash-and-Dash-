package com.example.splashanddash

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : BaseActivity() {

    override fun onCreate(b: Bundle?) {

        super.onCreate(b)

        val root = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setBackgroundColor(backgroundColor)
        }

        // -------------------------------------------------
        // HEADER
        // -------------------------------------------------

        val header = navBar(
            "Splash & Dash",
            "Professional vehicle cleaning services"
        )

        root.addView(header)

        // -------------------------------------------------
        // NAVIGATION
        // -------------------------------------------------

        val nav = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER

            setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(4)
            )

            setBackgroundColor(
                Color.rgb(
                    8,
                    42,
                    68
                )
            )
        }

        listOf(
            "Home",
            "Gallery",
            "Contact",
            "Login"
        ).forEach { label ->

            val item =
                TextView(this).apply {

                    text = label

                    textSize = 14f

                    typeface =
                        Typeface.create(
                            "sans",
                            Typeface.BOLD
                        )

                    setTextColor(Color.WHITE)

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        dp(12),
                        dp(12),
                        dp(12),
                        dp(12)
                    )

                    setOnClickListener {

                        when (label) {

                            "Gallery" ->
                                go(
                                    GalleryActivity::class.java
                                )

                            "Contact" ->
                                go(
                                    ContactActivity::class.java
                                )

                            "Login" ->
                                go(
                                    LoginActivity::class.java
                                )
                        }
                    }
                }

            nav.addView(
                item,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                )
            )
        }

        root.addView(nav)

        // -------------------------------------------------
        // SCROLL CONTENT
        // -------------------------------------------------

        val scroll = content()

        val c = column()

        // -------------------------------------------------
        // HERO
        // -------------------------------------------------

        val hero =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(20)
                )

                background =
                    GradientDrawable(
                        GradientDrawable
                            .Orientation.TL_BR,
                        intArrayOf(
                            Color.rgb(
                                0,
                                168,
                                255
                            ),
                            Color.rgb(
                                8,
                                42,
                                68
                            )
                        )
                    ).apply {

                        cornerRadius =
                            dp(20).toFloat()
                    }

                elevation =
                    dp(3).toFloat()
            }

        hero.addView(
            text(
                "PROFESSIONAL CAR CARE",
                12f,
                true
            ).apply {

                setTextColor(
                    Color.rgb(
                        220,
                        245,
                        255
                    )
                )

                letterSpacing = 0.08f
            }
        )

        hero.addView(
            text(
                "Keep Your Vehicle\nLooking Brand New",
                30f,
                true
            ).apply {

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(8)
                )
            }
        )

        hero.addView(
            text(
                "Fast, affordable and professional vehicle cleaning services — booked in a few simple steps.",
                15f
            ).apply {

                setTextColor(
                    Color.rgb(
                        232,
                        247,
                        255
                    )
                )

                setLineSpacing(
                    0f,
                    1.1f
                )
            }
        )

        val heroButton =
            button(
                "Book a Wash"
            ) {

                go(
                    RegisterActivity::class.java
                )
            }

        heroButton.background =
            GradientDrawable().apply {

                cornerRadius =
                    dp(13).toFloat()

                setColor(
                    Color.WHITE
                )
            }

        heroButton.setTextColor(
            Color.rgb(
                8,
                42,
                68
            )
        )

        hero.addView(heroButton)

        c.addView(
            hero,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {

                setMargins(
                    0,
                    0,
                    0,
                    dp(14)
                )
            }
        )

        // -------------------------------------------------
        // BANNER IMAGE
        // -------------------------------------------------

        val banner =
            ImageView(this).apply {

                setImageResource(
                    R.drawable.carwash_banner
                )

                scaleType =
                    ImageView.ScaleType.CENTER_CROP

                clipToOutline = true

                outlineProvider =
                    object :
                        android.view.ViewOutlineProvider() {

                        override fun getOutline(
                            view: android.view.View,
                            outline: android.graphics.Outline
                        ) {

                            outline.setRoundRect(
                                0,
                                0,
                                view.width,
                                view.height,
                                dp(18).toFloat()
                            )
                        }
                    }
            }

        c.addView(
            banner,
            LinearLayout.LayoutParams(
                -1,
                dp(210)
            ).apply {

                setMargins(
                    0,
                    0,
                    0,
                    dp(14)
                )
            }
        )

        // -------------------------------------------------
        // WHY US
        // -------------------------------------------------

        c.addView(
            text(
                "WHY SPLASH & DASH?",
                12f,
                true
            ).apply {

                setTextColor(
                    Color.rgb(
                        0,
                        126,
                        191
                    )
                )

                letterSpacing = 0.08f
            }
        )

        c.addView(
            text(
                "Simple, reliable vehicle care",
                25f,
                true
            )
        )

        c.addView(
            text(
                "Everything you need to keep your vehicle looking its best, from booking to payment.",
                15f
            ).apply {

                setTextColor(
                    Color.rgb(
                        101,
                        114,
                        126
                    )
                )
            }
        )

        // -------------------------------------------------
        // FEATURES
        // -------------------------------------------------

        listOf(

            "Fast service" to
                    "Quick turnaround times with convenient online booking.",

            "Great value" to
                    "Professional packages designed for every budget.",

            "Quality care" to
                    "A clean, polished finish from an experienced team.",

            "Easy online" to
                    "Book services and track your bookings from anywhere."

        ).forEach { (title, body) ->

            c.addView(
                card(
                    title,
                    body
                )
            )
        }

        // -------------------------------------------------
        // SERVICES
        // -------------------------------------------------

        c.addView(
            text(
                "OUR SERVICES",
                12f,
                true
            ).apply {

                setTextColor(
                    Color.rgb(
                        0,
                        126,
                        191
                    )
                )

                letterSpacing = 0.08f

                setPadding(
                    0,
                    dp(14),
                    0,
                    dp(4)
                )
            }
        )

        c.addView(
            text(
                "Choose your car wash package",
                25f,
                true
            )
        )

        service(
            c,
            "Exterior Wash",
            "Professional exterior wash including rinse, shampoo and tyre shine.",
            400,
            R.drawable.wash0
        )

        service(
            c,
            "Interior Cleaning",
            "Vacuum cleaning, dashboard treatment and interior sanitisation.",
            500,
            R.drawable.wash12
        )

        service(
            c,
            "Full Detail Package",
            "Complete interior and exterior detailing, polish and protective finish.",
            800,
            R.drawable.wash13
        )

        // -------------------------------------------------
        // FINAL CTA
        // -------------------------------------------------

        val cta = card(
            "READY WHEN YOU ARE",
            "Give your car the care it deserves. Create an account and book your next wash online.",
            "Create an Account"
        ) {

            go(
                RegisterActivity::class.java
            )
        }

        c.addView(cta)

        c.addView(
            footer()
        )

        scroll.addView(c)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // SERVICE CARD
    // ---------------------------------------------------------

    private fun service(
        c: LinearLayout,
        name: String,
        description: String,
        price: Int,
        image: Int
    ) {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14)
                )

                background =
                    GradientDrawable().apply {

                        cornerRadius =
                            dp(18).toFloat()

                        setColor(
                            Color.WHITE
                        )

                        setStroke(
                            dp(1),
                            Color.rgb(
                                224,
                                231,
                                237
                            )
                        )
                    }

                elevation =
                    dp(1).toFloat()
            }

        box.addView(
            ImageView(this).apply {

                setImageResource(image)

                scaleType =
                    ImageView.ScaleType.CENTER_CROP

                clipToOutline = true

                outlineProvider =
                    object :
                        android.view.ViewOutlineProvider() {

                        override fun getOutline(
                            view: android.view.View,
                            outline: android.graphics.Outline
                        ) {

                            outline.setRoundRect(
                                0,
                                0,
                                view.width,
                                view.height,
                                dp(14).toFloat()
                            )
                        }
                    }

            },
            LinearLayout.LayoutParams(
                -1,
                dp(170)
            )
        )

        box.addView(
            text(
                name,
                20f,
                true
            ).apply {

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(3)
                )
            }
        )

        box.addView(
            text(
                description,
                14f
            ).apply {

                setTextColor(
                    Color.rgb(
                        101,
                        114,
                        126
                    )
                )

                setLineSpacing(
                    0f,
                    1.08f
                )
            }
        )

        box.addView(
            text(
                "R${price}  •  Premium service",
                16f,
                true
            ).apply {

                setTextColor(
                    Color.rgb(
                        8,
                        42,
                        68
                    )
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(3)
                )
            }
        )

        box.addView(
            button(
                "Book Service"
            ) {

                go(
                    RegisterActivity::class.java
                )
            }
        )

        c.addView(
            box,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {

                setMargins(
                    0,
                    dp(7),
                    0,
                    dp(7)
                )
            }
        )
    }
}