const HISTORY_API_URL = "http://localhost:5160/api/Bookings";

document.addEventListener("DOMContentLoaded", async () => {
    const table = document.getElementById("bookingHistoryTable");

    if (!table) return;

    const customerData = sessionStorage.getItem("splashDashCustomer");

    if (!customerData) {
        alert("Please log in to view your booking history.");
        window.location.href = "login.html";
        return;
    }

    const customer = JSON.parse(customerData);

    const oldRows = Array.from(table.rows).slice(1);
    oldRows.forEach(row => row.remove());

    const tbody = document.createElement("tbody");
    table.appendChild(tbody);

    try {
        const response = await fetch(
            `${HISTORY_API_URL}/${customer.id}`
        );

        const bookings = await response.json();

        if (!response.ok) {
            throw new Error(
                bookings.message || "Could not load booking history."
            );
        }

        if (bookings.length === 0) {
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 8;
            cell.textContent = "You have no bookings yet.";
            return;
        }

        bookings.forEach(booking => {
            const row = tbody.insertRow();

            const vehicle = `${booking.vehicleMake} ${booking.vehicleModel}`;

            const bookingDate = new Date(booking.bookingDate);

            const date = bookingDate.toLocaleDateString("en-ZA");
            const time = bookingDate.toLocaleTimeString("en-ZA", {
                hour: "2-digit",
                minute: "2-digit"
            });

            const status = booking.status.toLowerCase();

            const values = [
                booking.id,
                vehicle,
                booking.vehicleRegistration,
                booking.serviceType,
                `R${Number(booking.price).toFixed(2)}`,
                date,
                time
            ];

            values.forEach(value => {
                const cell = row.insertCell();
                cell.textContent = value;
            });

            const statusCell = row.insertCell();
            const statusBadge = document.createElement("span");

            statusBadge.textContent = booking.status;
            statusBadge.className =
                status === "pending" ? "pending" :
                status === "completed" ? "completed" :
                status === "in progress" ? "progress" : "";

            statusCell.appendChild(statusBadge);
        });

    } catch (error) {
        const row = tbody.insertRow();
        const cell = row.insertCell();

        cell.colSpan = 8;
        cell.textContent = error.message;
    }
});
