
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SplashDash.Api.Data;
using SplashDash.Api.Models;

namespace SplashDash.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class CustomersController : ControllerBase
{
    private readonly AppDbContext _context;
    private readonly PasswordHasher<Customer> _passwordHasher = new();

    public CustomersController(AppDbContext context)
    {
        _context = context;
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register(RegisterCustomerRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.FullName) ||
            string.IsNullOrWhiteSpace(request.Email) ||
            string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new { message = "All fields are required." });
        }

        if (request.Password.Length < 8)
        {
            return BadRequest(new
            {
                message = "Password must contain at least 8 characters."
            });
        }

        string email = request.Email.Trim().ToLowerInvariant();

        bool emailExists = await _context.Customers
            .AnyAsync(c => c.Email.ToLower() == email);

        if (emailExists)
        {
            return Conflict(new
            {
                message = "An account with this email already exists."
            });
        }

        var customer = new Customer
        {
            FullName = request.FullName.Trim(),
            Email = email,
            CreatedAt = DateTime.UtcNow
        };

        customer.PasswordHash = _passwordHasher.HashPassword(
            customer, request.Password);

        _context.Customers.Add(customer);
        await _context.SaveChangesAsync();

        return Ok(new
        {
            message = "Registration successful.",
            customer = new
            {
                customer.Id,
                customer.FullName,
                customer.Email
            }
        });
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login(LoginCustomerRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Email) ||
            string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new
            {
                message = "Email and password are required."
            });
        }

        string email = request.Email.Trim().ToLowerInvariant();

        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.Email.ToLower() == email);

        if (customer == null)
        {
            return Unauthorized(new
            {
                message = "Invalid email or password."
            });
        }

        var result = _passwordHasher.VerifyHashedPassword(
            customer,
            customer.PasswordHash,
            request.Password);

        if (result == PasswordVerificationResult.Failed)
        {
            return Unauthorized(new
            {
                message = "Invalid email or password."
            });
        }

        return Ok(new
        {
            message = "Login successful.",
            customer = new
            {
                customer.Id,
                customer.FullName,
                customer.Email
            }
        });
    }
}

public class RegisterCustomerRequest
{
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public class LoginCustomerRequest
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}
