const BOOKINGS_API_URL = "http://localhost:5160/api/Bookings";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const messageBox = document.querySelector(".success");

    if (!form) return;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const customerData = sessionStorage.getItem("splashDashCustomer");

        if (!customerData) {
            alert("Please log in before creating a booking.");
            window.location.href = "login.html";
            return;
        }

        const customer = JSON.parse(customerData);

        const bookingDate = form.querySelector(
            '[name="booking_date"]'
        ).value;

        const bookingTime = form.querySelector(
            '[name="booking_time"]'
        ).value;

        const requestBody = {
            customerId: customer.id,
            vehicleMake: form.querySelector(
                '[name="vehicle_make"]'
            ).value.trim(),

            vehicleModel: form.querySelector(
                '[name="vehicle_model"]'
            ).value.trim(),

            vehicleRegistration: form.querySelector(
                '[name="registration_number"]'
            ).value.trim(),

            vehicleType: form.querySelector(
                '[name="vehicle_type"]'
            ).value,

            serviceType: form.querySelector(
                '[name="service_id"]'
            ).value,

            bookingDate: new Date(`${bookingDate}T${bookingTime}:00`).toISOString()
        };

        const submitButton = form.querySelector(
            'button[type="submit"]'
        );

        submitButton.disabled = true;
        submitButton.textContent = "Creating booking...";

        messageBox.textContent = "";
        messageBox.style.display = "none";

        try {
            const response = await fetch(BOOKINGS_API_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(requestBody)
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(
                    result.message || "Could not create booking."
                );
            }

            messageBox.textContent =
                `${result.message} Booking reference: ${result.booking.id}.`;

            messageBox.style.display = "block";

            form.reset();

        } catch (error) {
            messageBox.textContent =
                error.message === "Failed to fetch"
                    ? "Could not connect to the server. Please check that the API is running."
                    : error.message;

            messageBox.style.display = "block";

        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Create Booking";
        }
    });
});
