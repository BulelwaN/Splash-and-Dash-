# Splash & Dash — Android Studio Application

This project is a native Android conversion of the supplied `splash_dash_html` website.

## What was converted

- Splash & Dash branding, navy/blue colour palette and supplied logo/banner/images
- Home page with hero section, welcome section, service packages and call-to-action
- Login and registration flows
- Customer dashboard
- Customer booking form with vehicle type, registration, service, date picker and time
- Booking history with status tracking
- Payment screen with Cash/Card/EFT options
- Staff dashboard and service queue with status updates
- Admin dashboard
- Manage Customers, Manage Bookings and Manage Staff screens
- Reports screen
- Gallery
- Contact form

## Important note

The original website is mostly a front-end template and contains empty dynamic fields where the PHP/database backend would normally supply data. This Android version therefore includes a lightweight in-memory mock data layer (`AppData.kt`) so the app can demonstrate the workflows without a server.

Bookings created in the app appear in Booking History and can be changed from Pending to Completed. Staff/Admin status updates affect the same in-memory booking list while the app is running.

## Open in Android Studio

1. Extract the ZIP.
2. Open the `SplashDashAndroid` folder in Android Studio.
3. Allow Android Studio to sync Gradle.
4. Use an Android 7.0 (API 24) or newer emulator/device.
5. Run the `app` configuration.

## Login demo

The login screen is intentionally a demo role selector because the supplied HTML does not contain a working backend authentication system.

- Customer → Customer Dashboard
- Staff → Staff Dashboard
- Admin → Admin Dashboard

Any email/password can be entered for this demo.

## Project structure

`app/src/main/java/com/example/splashanddash/` contains Kotlin activities and the shared mock data layer.

`app/src/main/res/drawable-nodpi/` contains the original supplied Splash & Dash images.

`website_source/` contains the original supplied HTML/CSS/images for reference.

## 🎥 Demo Video

Watch the SplashDash demo here:
[Click to Watch Demo](https://youtu.be/Zxfxhz75BLs?feature=shared)
