using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SplashDash.Api.Data;
using SplashDash.Api.Models;

namespace SplashDash.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class BookingsController : ControllerBase
{
    private readonly AppDbContext _context;

    public BookingsController(AppDbContext context)
    {
        _context = context;
    }

    [HttpGet("{customerId:int}")]
    public async Task<IActionResult> GetCustomerBookings(int customerId)
    {
        if (customerId <= 0)
        {
            return BadRequest(new
            {
                message = "Invalid customer."
            });
        }

        var bookings = await _context.Bookings
            .Where(b => b.CustomerId == customerId)
            .OrderByDescending(b => b.BookingDate)
            .Select(b => new
            {
                b.Id,
                b.VehicleMake,
                b.VehicleModel,
                b.VehicleRegistration,
                b.VehicleType,
                b.ServiceType,
                b.BookingDate,
                b.Status,
                b.Price
            })
            .ToListAsync();

        return Ok(bookings);
    }
    [HttpGet("all")]
    public async Task<IActionResult> GetAllBookings()
    {
        var bookings = await _context.Bookings
            .Include(b => b.Customer)
            .OrderBy(b => b.BookingDate)
            .Select(b => new
            {
                b.Id,
                b.CustomerId,
                CustomerName = b.Customer!.FullName,
                b.VehicleMake,
                b.VehicleModel,
                b.VehicleRegistration,
                b.VehicleType,
                b.ServiceType,
                b.BookingDate,
                b.Status,
                b.Price
            })
            .ToListAsync();

        return Ok(bookings);
    }

    [HttpPut("{id:int}/status")]
    public async Task<IActionResult> UpdateBookingStatus(
        int id,
        UpdateBookingStatusRequest request)
    {
        var booking = await _context.Bookings
            .FirstOrDefaultAsync(b => b.Id == id);

        if (booking == null)
        {
            return NotFound(new
            {
                message = "Booking not found."
            });
        }

        var allowedStatuses = new[]
        {
            "Pending",
            "In Progress",
            "Completed"
        };

        if (!allowedStatuses.Contains(request.Status))
        {
            return BadRequest(new
            {
                message = "Invalid booking status."
            });
        }

        booking.Status = request.Status;

        await _context.SaveChangesAsync();

        return Ok(new
        {
            message = "Booking status updated successfully.",
            booking.Id,
            booking.Status
        });
    }
    [HttpPost]
    public async Task<IActionResult> CreateBooking(
        CreateBookingRequest request)
    {
        if (request.CustomerId <= 0)
        {
            return BadRequest(new
            {
                message = "Invalid customer."
            });
        }

        if (string.IsNullOrWhiteSpace(request.VehicleMake) ||
            string.IsNullOrWhiteSpace(request.VehicleModel) ||
            string.IsNullOrWhiteSpace(request.VehicleRegistration) ||
            string.IsNullOrWhiteSpace(request.VehicleType))
        {
            return BadRequest(new
            {
                message = "Please complete all vehicle information."
            });
        }

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Id == request.CustomerId);

        if (customer == null)
        {
            return NotFound(new
            {
                message = "Customer account not found."
            });
        }

        string service = request.ServiceType.Trim();

        decimal price = service switch
        {
            "Exterior Wash" => 400m,
            "Interior Cleaning" => 500m,
            "Full Detail Package" => 800m,
            _ => 0m
        };

        if (price == 0)
        {
            return BadRequest(new
            {
                message = "Please select a valid car wash service."
            });
        }

        if (request.BookingDate <= DateTime.UtcNow)
        {
            return BadRequest(new
            {
                message = "Please select a future booking date and time."
            });
        }

        var booking = new Booking
        {
            CustomerId = customer.Id,
            VehicleMake = request.VehicleMake.Trim(),
            VehicleModel = request.VehicleModel.Trim(),
            VehicleRegistration =
                request.VehicleRegistration.Trim().ToUpperInvariant(),
            VehicleType = request.VehicleType.Trim(),
            ServiceType = service,
            BookingDate = request.BookingDate,
            Status = "Pending",
            Price = price,
            CreatedAt = DateTime.UtcNow
        };

        _context.Bookings.Add(booking);

        await _context.SaveChangesAsync();

        return Ok(new
        {
            message = "Your booking was created successfully.",
            booking = new
            {
                booking.Id,
                booking.CustomerId,
                booking.VehicleMake,
                booking.VehicleModel,
                booking.VehicleRegistration,
                booking.VehicleType,
                booking.ServiceType,
                booking.BookingDate,
                booking.Status,
                booking.Price
            }
        });
    }
}

public class CreateBookingRequest
{
    public int CustomerId { get; set; }

    public string VehicleMake { get; set; } = string.Empty;

    public string VehicleModel { get; set; } = string.Empty;

    public string VehicleRegistration { get; set; } = string.Empty;

    public string VehicleType { get; set; } = string.Empty;

    public string ServiceType { get; set; } = string.Empty;

    public DateTime BookingDate { get; set; }
}




public class UpdateBookingStatusRequest
{
    public string Status { get; set; } = string.Empty;
}

