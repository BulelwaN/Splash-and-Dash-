using System.Globalization;
using System.Net;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Google.Cloud.Firestore;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace SplashDash.Api.Controllers;

[ApiController]
[Route("api/payments")]
public class PaymentsController : ControllerBase
{
    private static readonly HttpClient HttpClient = new();

    private readonly FirestoreDb _firestore;
    private readonly IConfiguration _configuration;

    private static readonly string[] PayFastIpRanges =
    {
        "197.97.145.144/28",
        "41.74.179.192/27",
        "102.216.36.0/28",
        "102.216.36.128/28",
        "144.126.193.139/32"
    };

    public PaymentsController(
        FirestoreDb firestore,
        IConfiguration configuration)
    {
        _firestore = firestore;
        _configuration = configuration;
    }

    // ---------------------------------------------------------
    // CREATE PAYFAST CHECKOUT
    // ---------------------------------------------------------

    [HttpPost("payfast/checkout")]
    [Authorize(AuthenticationSchemes = "Firebase")]
    public async Task<IActionResult> CreateCheckout(
        [FromBody] CheckoutRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.BookingDocId))
            return BadRequest("Booking ID is required.");

        var customerId =
            User.FindFirstValue("user_id") ??
            User.FindFirstValue(ClaimTypes.NameIdentifier) ??
            User.FindFirstValue("sub");

        if (string.IsNullOrWhiteSpace(customerId))
            return Unauthorized();

        var email = User.FindFirstValue(ClaimTypes.Email)
                    ?? User.FindFirstValue("email");

        email = email?.Trim();

        if (string.IsNullOrWhiteSpace(email))
            return BadRequest("Your Firebase account needs a valid email address.");

        var bookingRef = _firestore
            .Collection("bookings")
            .Document(request.BookingDocId);

        var booking = await bookingRef.GetSnapshotAsync();

        if (!booking.Exists)
            return NotFound("Booking not found.");

        if (booking.GetValue<string>("customerId") != customerId)
            return Forbid();

        if (booking.GetValue<string>("status") != "Pending")
            return BadRequest("Booking is not available for payment.");

        var paymentStatus =
            booking.ContainsField("paymentStatus")
                ? booking.GetValue<string>("paymentStatus")
                : "";

        if (!string.IsNullOrWhiteSpace(paymentStatus))
            return BadRequest("A payment has already been submitted.");

        var paymentRef = _firestore
            .Collection("payments")
            .Document(request.BookingDocId);

        var existingPayment = await paymentRef.GetSnapshotAsync();

        if (existingPayment.Exists)
            return BadRequest("A payment record already exists.");

        // The amount is obtained from Firestore, never from Android.
        var price = booking.GetValue<long>("price");

        if (price <= 0)
            return BadRequest("Invalid booking amount.");

        var amount = ((decimal)price).ToString(
            "0.00",
            CultureInfo.InvariantCulture);

        var merchantId = _configuration["PayFast:MerchantId"];
        var merchantKey = _configuration["PayFast:MerchantKey"];
        var passphrase = _configuration["PayFast:Passphrase"];
        var processUrl = _configuration["PayFast:ProcessUrl"];
        var publicUrl = _configuration["PayFast:PublicBaseUrl"];

        if (string.IsNullOrWhiteSpace(merchantId) ||
            string.IsNullOrWhiteSpace(merchantKey) ||
            string.IsNullOrWhiteSpace(passphrase) ||
            string.IsNullOrWhiteSpace(processUrl) ||
            string.IsNullOrWhiteSpace(publicUrl))
        {
            return StatusCode(500, "PayFast configuration is incomplete.");
        }

        publicUrl = publicUrl.TrimEnd('/');

        // Keep this field order consistent with PayFast's
        // custom checkout signature documentation.
        var fields = new Dictionary<string, string>
        {
            ["merchant_id"] = merchantId,
            ["merchant_key"] = merchantKey,
            ["return_url"] = publicUrl + "/api/payments/payfast/return",
            ["cancel_url"] = publicUrl + "/api/payments/payfast/cancel",
            ["notify_url"] = publicUrl + "/api/payments/payfast/itn",
            ["email_address"] = email,
            ["m_payment_id"] = request.BookingDocId,
            ["amount"] = amount,
            ["item_name"] = "Splash and Dash Booking"
        };

        fields["signature"] =
            GenerateSignature(fields, passphrase, false);

        return Ok(new
        {
            processUrl,
            fields
        });
    }

    // ---------------------------------------------------------
    // RECEIVE PAYFAST ITN
    // ---------------------------------------------------------

    [HttpPost("payfast/itn")]
    [AllowAnonymous]
    [Consumes("application/x-www-form-urlencoded")]
    public async Task<IActionResult> PayFastItn()
    {
        // Check the originating IP before processing the notification.
        var clientIp = GetClientIp();

        if (clientIp == null || !IsPayFastIp(clientIp))
            return StatusCode(403, "Unrecognised notification source.");

        var form = await Request.ReadFormAsync();

        if (!form.ContainsKey("signature"))
            return BadRequest("Missing signature.");

        // PayFast sends its signature after the notification fields.
        var keys = form.Keys.ToList();
        var signatureIndex = keys.FindIndex(
            key => key.Equals("signature", StringComparison.OrdinalIgnoreCase));

        if (signatureIndex < 0 || signatureIndex != keys.Count - 1)
            return BadRequest("Invalid notification field order.");

        var receivedSignature = form["signature"].ToString();

        var passphrase = _configuration["PayFast:Passphrase"];

        if (string.IsNullOrWhiteSpace(passphrase))
            return StatusCode(500, "PayFast passphrase is not configured.");

        var itnFields = new List<KeyValuePair<string, string>>();

        for (var i = 0; i < signatureIndex; i++)
        {
            var key = keys[i];

            itnFields.Add(
                new KeyValuePair<string, string>(
                    key,
                    form[key].ToString()));
        }

        var expectedSignature =
            GenerateSignature(itnFields, passphrase, true);

        if (!FixedTimeSignatureEquals(
            receivedSignature,
            expectedSignature))
        {
            return BadRequest("Invalid PayFast signature.");
        }

        var data = itnFields.ToDictionary(
            item => item.Key,
            item => item.Value);

        var merchantId = _configuration["PayFast:MerchantId"];

        if (!data.TryGetValue("merchant_id", out var receivedMerchant) ||
            receivedMerchant != merchantId)
        {
            return BadRequest("Invalid merchant.");
        }

        if (!data.TryGetValue("payment_status", out var status) ||
            status != "COMPLETE")
        {
            return Ok("Notification received; payment is not complete.");
        }

        if (!data.TryGetValue("m_payment_id", out var bookingId) ||
            string.IsNullOrWhiteSpace(bookingId))
        {
            return BadRequest("Missing booking ID.");
        }

        if (!data.TryGetValue("pf_payment_id", out var payFastPaymentId) ||
            string.IsNullOrWhiteSpace(payFastPaymentId))
        {
            return BadRequest("Missing PayFast payment ID.");
        }

        if (!data.TryGetValue("amount_gross", out var grossAmount) ||
            !decimal.TryParse(
                grossAmount,
                NumberStyles.Number,
                CultureInfo.InvariantCulture,
                out var receivedAmount))
        {
            return BadRequest("Invalid payment amount.");
        }

        var bookingRef = _firestore
            .Collection("bookings")
            .Document(bookingId);

        var paymentRef = _firestore
            .Collection("payments")
            .Document(bookingId);

        // Confirm the notification directly with PayFast.
        var validationUrl = _configuration["PayFast:ValidateUrl"];

        if (string.IsNullOrWhiteSpace(validationUrl))
            return StatusCode(500, "PayFast validation URL is missing.");

        using var validationContent =
            new FormUrlEncodedContent(itnFields);

        using var validationResponse = await HttpClient.PostAsync(
            validationUrl,
            validationContent);

        var validationResult =
            (await validationResponse.Content.ReadAsStringAsync()).Trim();

        if (!validationResponse.IsSuccessStatusCode ||
            validationResult != "VALID")
        {
            return BadRequest("PayFast server validation failed.");
        }

        // Update the payment and booking atomically.
        var result = await _firestore.RunTransactionAsync(
            async transaction =>
            {
                var latestBooking =
                    await transaction.GetSnapshotAsync(bookingRef);

                var latestPayment =
                    await transaction.GetSnapshotAsync(paymentRef);

                if (!latestBooking.Exists)
                    return "Booking not found.";

                var expectedAmount =
                    (decimal)latestBooking.GetValue<long>("price");

                if (receivedAmount != expectedAmount)
                    return "Payment amount does not match booking.";

                // Handle a repeated notification safely.
                if (latestPayment.Exists)
                {
                    var existingMethod =
                        latestPayment.ContainsField("method")
                            ? latestPayment.GetValue<string>("method")
                            : "";

                    var existingStatus =
                        latestPayment.ContainsField("status")
                            ? latestPayment.GetValue<string>("status")
                            : "";

                    var existingPayFastId =
                        latestPayment.ContainsField("payfastPaymentId")
                            ? latestPayment.GetValue<string>("payfastPaymentId")
                            : "";

                    if (existingMethod == "Card" &&
                        existingStatus == "Paid" &&
                        existingPayFastId == payFastPaymentId)
                    {
                        transaction.Update(bookingRef,
                            new Dictionary<string, object>
                            {
                                ["paymentStatus"] = "Paid",
                                ["paymentMethod"] = "Card",
                                ["status"] = "Confirmed"
                            });

                        return "Payment already processed. Booking confirmed.";
                    }

                    return "A different payment record already exists.";
                }

                var currentPaymentStatus =
                    latestBooking.ContainsField("paymentStatus")
                        ? latestBooking.GetValue<string>("paymentStatus")
                        : "";

                if (!string.IsNullOrWhiteSpace(currentPaymentStatus))
                    return "Booking already has a payment status.";

                var customerId =
                    latestBooking.GetValue<string>("customerId");

                var paymentData = new Dictionary<string, object>
                {
                    ["bookingId"] = bookingId,
                    ["bookingDocId"] = bookingId,
                    ["customerId"] = customerId,
                    ["amount"] = (long)expectedAmount,
                    ["method"] = "Card",
                    ["status"] = "Paid",
                    ["provider"] = "PayFast",
                    ["payfastPaymentId"] = payFastPaymentId,
                    ["timestamp"] = Timestamp.GetCurrentTimestamp()
                };

                transaction.Set(paymentRef, paymentData);

                transaction.Update(bookingRef,
                    new Dictionary<string, object>
                    {
                        ["paymentStatus"] = "Paid",
                        ["paymentMethod"] = "Card",
                        ["status"] = "Confirmed"
                    });
                return "Payment successfully verified.";
            });

        if (result == "Booking not found.")
            return NotFound(result);

        if (result == "Payment amount does not match booking.")
            return BadRequest(result);

        if (result == "A different payment record already exists." ||
            result == "Booking already has a payment status.")
        {
            return Conflict(result);
        }

        return Ok(result);
    }

    // ---------------------------------------------------------
    // RETURN AND CANCEL PAGES
    // ---------------------------------------------------------

    [HttpGet("payfast/return")]
    [AllowAnonymous]
    public IActionResult PayFastReturn()
    {
        return Content(
            "Thank you. Your payment is being verified. " +
            "Please check your booking status.",
            "text/plain");
    }

    [HttpGet("payfast/cancel")]
    [AllowAnonymous]
    public IActionResult PayFastCancel()
    {
        return Content(
            "Payment was cancelled. No payment confirmation was received.",
            "text/plain");
    }

    // ---------------------------------------------------------
    // SIGNATURE GENERATION
    // ---------------------------------------------------------

    private static string GenerateSignature(
        IEnumerable<KeyValuePair<string, string>> fields,
        string passphrase,
        bool includeEmpty)
    {
        var parts = new List<string>();

        foreach (var item in fields)
        {
            if (!includeEmpty && string.IsNullOrEmpty(item.Value))
                continue;

            parts.Add(
                $"{item.Key}={WebUtility.UrlEncode(item.Value.Trim())}");
        }

        var parameterString = string.Join("&", parts);

        parameterString +=
            "&passphrase=" + WebUtility.UrlEncode(passphrase.Trim());

        var hash = MD5.HashData(
            Encoding.UTF8.GetBytes(parameterString));

        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private static bool FixedTimeSignatureEquals(
        string received,
        string expected)
    {
        try
        {
            var receivedBytes = Convert.FromHexString(received);
            var expectedBytes = Convert.FromHexString(expected);

            return receivedBytes.Length == expectedBytes.Length &&
                CryptographicOperations.FixedTimeEquals(
                    receivedBytes,
                    expectedBytes);
        }
        catch (FormatException)
        {
            return false;
        }
    }

    // ---------------------------------------------------------
    // PAYFAST IP VALIDATION
    // ---------------------------------------------------------

    private IPAddress? GetClientIp()
    {
        // ngrok forwards the original connecting IP in this header.
        // The last entry is the address appended by the proxy.
        var forwardedFor =
            Request.Headers["X-Forwarded-For"].ToString();

        if (!string.IsNullOrWhiteSpace(forwardedFor))
        {
            var addresses = forwardedFor.Split(
                ',',
                StringSplitOptions.RemoveEmptyEntries);

            if (addresses.Length > 0 &&
                IPAddress.TryParse(
                    addresses[^1].Trim(),
                    out var forwardedIp))
            {
                return NormalizeIp(forwardedIp);
            }

            return null;
        }

        return Request.HttpContext.Connection.RemoteIpAddress
            is { } remoteIp
                ? NormalizeIp(remoteIp)
                : null;
    }

    private static IPAddress NormalizeIp(IPAddress ip)
    {
        return ip.IsIPv4MappedToIPv6
            ? ip.MapToIPv4()
            : ip;
    }

    private static bool IsPayFastIp(IPAddress ip)
    {
        foreach (var range in PayFastIpRanges)
        {
            var parts = range.Split('/');
            var network = IPAddress.Parse(parts[0]);
            var prefixLength = int.Parse(parts[1]);

            if (IsInNetwork(ip, network, prefixLength))
                return true;
        }

        return false;
    }

    private static bool IsInNetwork(
        IPAddress address,
        IPAddress network,
        int prefixLength)
    {
        var addressBytes = address.GetAddressBytes();
        var networkBytes = network.GetAddressBytes();

        if (addressBytes.Length != networkBytes.Length)
            return false;

        var fullBytes = prefixLength / 8;
        var remainingBits = prefixLength % 8;

        for (var i = 0; i < fullBytes; i++)
        {
            if (addressBytes[i] != networkBytes[i])
                return false;
        }

        if (remainingBits > 0)
        {
            var mask = (byte)(0xFF << (8 - remainingBits));

            if ((addressBytes[fullBytes] & mask) !=
                (networkBytes[fullBytes] & mask))
            {
                return false;
            }
        }

        return true;
    }
}

public class CheckoutRequest
{
    public string BookingDocId { get; set; } = "";
}
