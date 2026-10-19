using System.ComponentModel.DataAnnotations;

namespace SplashDash.Api.Models;

public class Booking
{
    public int Id { get; set; }

    public int CustomerId { get; set; }

    public Customer? Customer { get; set; }

    [Required, MaxLength(100)]
    public string VehicleRegistration { get; set; } = string.Empty;

    [Required, MaxLength(100)]
    public string ServiceType { get; set; } = string.Empty;

    public DateTime BookingDate { get; set; }

    [MaxLength(30)]
    public string Status { get; set; } = "Pending";

    public decimal Price { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
