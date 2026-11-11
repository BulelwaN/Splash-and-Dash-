const API_BASE_URL = "http://localhost:5160/api/Customers";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");

    if (!form) return;

    const isRegisterPage = window.location.pathname.toLowerCase().includes("register");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = form.querySelector('input[name="email"]').value.trim();
        const password = form.querySelector('input[name="password"]').value;

        const requestBody = {
            email: email,
            password: password
        };

        let endpoint = `${API_BASE_URL}/login`;

        if (isRegisterPage) {
            const fullName = form.querySelector('input[name="full_name"]').value.trim();

            requestBody.fullName = fullName;
            endpoint = `${API_BASE_URL}/register`;
        }

        const messageBox = document.querySelector(
            isRegisterPage ? ".success" : ".error"
        );

        const submitButton = form.querySelector('button[type="submit"]');

        submitButton.disabled = true;
        submitButton.textContent = "Please wait...";

        if (messageBox) {
            messageBox.textContent = "";
            messageBox.style.display = "none";
        }

        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(requestBody)
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.message || "Something went wrong.");
            }

            if (messageBox) {
                messageBox.textContent = result.message;
                messageBox.style.display = "block";
            }

            if (isRegisterPage) {
                form.reset();
            } else {
                sessionStorage.setItem(
                    "splashDashCustomer",
                    JSON.stringify(result.customer)
                );

                window.location.href = "customer - dashboard.html";
                return;
            }

        } catch (error) {
            if (messageBox) {
                messageBox.textContent =
                    error.message === "Failed to fetch"
                        ? "Could not connect to the server. Please check that the API is running."
                        : error.message;

                messageBox.style.display = "block";
            }
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = isRegisterPage ? "Register" : "Login";
        }
    });
});

