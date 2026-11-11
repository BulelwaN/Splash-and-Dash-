const STAFF_API_URL = "http://localhost:5160/api/Bookings";

document.addEventListener("DOMContentLoaded", loadServiceQueue);

async function loadServiceQueue() {
    const table = document.getElementById("serviceQueueTable");

    if (!table) return;

    const oldRows = Array.from(table.rows).slice(1);
    oldRows.forEach(row => row.remove());

    const tbody = document.createElement("tbody");
    table.appendChild(tbody);

    try {
        const response = await fetch(`${STAFF_API_URL}/all`);
        const bookings = await response.json();

        if (!response.ok) {
            throw new Error("Could not retrieve service queue.");
        }

        if (bookings.length === 0) {
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 6;
            cell.textContent = "No bookings available.";
            return;
        }

        bookings.forEach(booking => {
            const row = tbody.insertRow();

            const vehicle = `${booking.vehicleMake} ${booking.vehicleModel}`;

            [
                booking.id,
                vehicle,
                booking.vehicleRegistration,
                booking.serviceType
            ].forEach(value => {
                const cell = row.insertCell();
                cell.textContent = value;
            });

            const statusCell = row.insertCell();
            statusCell.textContent = booking.status;

            const actionCell = row.insertCell();

            const select = document.createElement("select");

            ["Pending", "In Progress", "Completed"].forEach(status => {
                const option = document.createElement("option");
                option.value = status;
                option.textContent = status;
                option.selected = status === booking.status;
                select.appendChild(option);
            });

            const button = document.createElement("button");
            button.textContent = "Save";

            button.addEventListener("click", async () => {
                try {
                    const updateResponse = await fetch(
                        `${STAFF_API_URL}/${booking.id}/status`,
                        {
                            method: "PUT",
                            headers: {
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify({
                                status: select.value
                            })
                        }
                    );

                    const result = await updateResponse.json();

                    if (!updateResponse.ok) {
                        throw new Error(
                            result.message || "Status update failed."
                        );
                    }

                    alert("Booking status updated successfully.");
                    loadServiceQueue();

                } catch (error) {
                    alert(error.message);
                }
            });

            actionCell.appendChild(select);
            actionCell.appendChild(button);
        });

    } catch (error) {
        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 6;
        cell.textContent = error.message;
    }
}
