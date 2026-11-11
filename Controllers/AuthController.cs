using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using SplashDash.Api.Data;
using SplashDash.Api.Models;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace SplashDash.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly AppDbContext _context;
    private readonly IConfiguration _configuration;
    private readonly PasswordHasher<Customer> _customerHasher = new();
    private readonly PasswordHasher<StaffAccount> _staffHasher = new();

    public AuthController(
        AppDbContext context,
        IConfiguration configuration)
    {
        _context = context;
        _configuration = configuration;
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login(LoginRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Identifier) ||
            string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new
            {
                message = "Username/email and password are required."
            });
        }

        string identifier = request.Identifier.Trim();
        string normalized = identifier.ToLowerInvariant();

        // A staff username is checked first so staff can sign in
        // even if their email is also used by a customer account.
        var staffByUsername = await _context.StaffAccounts
            .FirstOrDefaultAsync(s =>
                s.Username.ToLower() == normalized);

        if (staffByUsername != null)
        {
            return AuthenticateStaff(staffByUsername, request.Password);
        }

        // Customers sign in using their registered email.
        var customer = await _context.Customers
            .FirstOrDefaultAsync(c =>
                c.Email.ToLower() == normalized);

        if (customer != null)
        {
            var result = _customerHasher.VerifyHashedPassword(
                customer,
                customer.PasswordHash,
                request.Password);

            if (result == PasswordVerificationResult.Failed)
            {
                return Unauthorized(new
                {
                    message = "Invalid username/email or password."
                });
            }

            return Ok(CreateLoginResponse(
                customer.Id,
                customer.FullName,
                customer.Email,
                "Customer",
                false));
        }

        // Staff and Admin can also use their email address.
        var staffByEmail = await _context.StaffAccounts
            .FirstOrDefaultAsync(s =>
                s.Email.ToLower() == normalized);

        if (staffByEmail != null)
        {
            return AuthenticateStaff(staffByEmail, request.Password);
        }

        return Unauthorized(new
        {
            message = "Invalid username/email or password."
        });
    }

    private IActionResult AuthenticateStaff(
        StaffAccount staff,
        string password)
    {
        if (!staff.IsActive)
        {
            return Unauthorized(new
            {
                message = "This account is inactive. Contact an administrator."
            });
        }

        var result = _staffHasher.VerifyHashedPassword(
            staff,
            staff.PasswordHash,
            password);

        if (result == PasswordVerificationResult.Failed)
        {
            return Unauthorized(new
            {
                message = "Invalid username/email or password."
            });
        }

        if (staff.Role != "Staff" && staff.Role != "Admin")
        {
            return Unauthorized(new
            {
                message = "This account has an invalid role."
            });
        }

        return Ok(CreateLoginResponse(
            staff.Id,
            staff.FullName,
            staff.Email,
            staff.Role,
            staff.MustChangePassword));
    }

    private object CreateLoginResponse(
        int accountId,
        string fullName,
        string email,
        string role,
        bool mustChangePassword)
    {
        string key = _configuration["Jwt:Key"]
            ?? throw new InvalidOperationException(
                "JWT signing key is not configured.");

        string issuer = _configuration["Jwt:Issuer"]
            ?? throw new InvalidOperationException(
                "JWT issuer is not configured.");

        string audience = _configuration["Jwt:Audience"]
            ?? throw new InvalidOperationException(
                "JWT audience is not configured.");

        var now = DateTime.UtcNow;
        var expires = now.AddHours(8);

        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, accountId.ToString()),
            new(ClaimTypes.Name, fullName),
            new(ClaimTypes.Email, email),
            new(ClaimTypes.Role, role),
            new("must_change_password",
                mustChangePassword.ToString().ToLowerInvariant())
        };

        var signingKey = new SymmetricSecurityKey(
            Encoding.UTF8.GetBytes(key));

        var credentials = new SigningCredentials(
            signingKey,
            SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: issuer,
            audience: audience,
            claims: claims,
            notBefore: now,
            expires: expires,
            signingCredentials: credentials);

        return new
        {
            message = "Login successful.",
            token = new JwtSecurityTokenHandler().WriteToken(token),
            tokenType = "Bearer",
            expiresAt = expires,
            account = new
            {
                id = accountId,
                fullName,
                email,
                role,
                mustChangePassword
            }
        };
    }
}

public class LoginRequest
{
    public string Identifier { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}
