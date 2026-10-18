Splash-and-Dash-
XISD6329 poe- Splash and Dash car wash
Project Overview

Splash & Dash is a car wash booking system made up of a **Web Interface** and an **Android Application**. The system is designed to make bookings easier for customers and help staff and administrators manage the car wash.

1. Web Project

How to Run

The website can be opened by opening `index.html` in a browser.

It can also be run using **Visual Studio Code and Live Server**:
1. Open the `splash_dash_html` folder.
2. Open `index.html`.
3. Select **Go Live**.

The folder structure should stays the same because the website uses relative paths for CSS and images.

 Web Structure

splash_dash_html/
├── index.html
├── login.html
├── register.html
├── gallery.html
├── contact.html
├── customer - dashboard.html
├── customer - bookings.html
├── customer - history.html
├── customer - payment.html
├── staff - dashboard.html
├── staff - service_queue.html
├── staff - update_status.html
├── admin - dashboard.html
├── admin - manage_bookings.html
├── admin - manage_customers.html
├── admin - manage_staff.html
├── admin - reports.html
├── css/
│ └── style.css
└── images/
    └── project images

The web project is currently a **front-end prototype** and is not connected to a live backend or database.

 2. Android Project

The Android application is developed using **Kotlin** and **SQLite**.

Main features include:

* Customer registration and login
* Creating car wash bookings
* Selecting services
* Calculating booking totals
* Viewing booking history
* Updating booking status
* Viewing customer profile
* Payment confirmation

The Android application currently stores its data locally using SQLite.

 Android Testing

The main features that should be tested are:
* Registration with valid and invalid information
* Login with correct and incorrect details
* Creating and saving bookings
* Calculating service totals
* Viewing booking history
* Updating booking status
* Viewing profile information
* Payment confirmation

 3. Technologies Used

| Component | Technology |
| ------------ | ---------- |
| Web | HTML, CSS |
| Android | Kotlin |
| Database | SQLite |
| UI | XML, CSS |
| Build System | Gradle |

 4. Security

The current version is mainly a prototype. For a real system, it should use:

* Password hashing
* Secure authentication
* Server-side validation
* HTTPS
* Access control
* Secure payment processing

 5. Limitations

Some current limitations are:

* The website is not connected to a live database.
* Android data is stored locally.
* Android passwords are not hashed.
* Payments are simulated.
* Web dashboards use prototype data.
* Admin and staff features are not fully connected to the Android application.

 6. Future Improvements
Future versions could include:
* A central online database
* Backend API integration
* Secure authentication
* Real online payments
* Booking notifications
* Booking cancellation
* Staff scheduling
* Customer reviews
* Improved admin reports

 7. Project Purpose

The purpose of Splash & Dash is to provide a simple digital solution for managing car wash bookings. The web system supports customers, staff and administrators, while the Android application gives customers another way to make and manage their bookings.


# Splash & Dash Car Wash Management System

## Project Overview
Splash & Dash is a car wash management system designed to
allow customers to explore services, manage vehicle details,
make bookings and view booking information.

The system is being developed as part of Task 2.

## Planned Features
- Responsive public website
- Customer registration and sign-in
- Customer dashboard and booking management
- Staff service queue
- Administrator management screens
- Input validation and secure authentication
- Backend API and relational database
- Google sign-in (SSO)
- Automated testing and continuous integration

## Technology Stack
The final backend, database and hosting technologies will be
documented after confirmation of the module requirements.

## Running the Frontend
1. Clone the repository.
2. Open the project folder in Visual Studio Code.
3. Open `index.html` in a browser, or use the Live Server
   extension.

## Testing
Automated tests and their execution instructions will be
added as the application is implemented.

## Team Contributions
Document each group member's responsibilities and contributions.

## Security
Never commit passwords, API keys, database credentials or
other secrets to the repository.
