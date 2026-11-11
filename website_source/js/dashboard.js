document.addEventListener("DOMContentLoaded", () => {
    const customerData = sessionStorage.getItem("splashDashCustomer");

    if (!customerData) {
        window.location.href = "login.html";
        return;
    }

    const customer = JSON.parse(customerData);

    const welcomeMessage = document.querySelector(".header p");

    if (welcomeMessage) {
        welcomeMessage.textContent = `Welcome, ${customer.fullName}!`;
    }

    const logoutLink = document.querySelector('.navbar a[href="login.html"]');

    if (logoutLink) {
        logoutLink.addEventListener("click", (event) => {
            event.preventDefault();
            sessionStorage.removeItem("splashDashCustomer");
            window.location.href = "login.html";
        });
    }
});
