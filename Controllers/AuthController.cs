using Microsoft.AspNetCore.Authorization;
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


    [AllowAnonymous]
    [HttpPost("bootstrap-admin")]
    public async Task<IActionResult> BootstrapAdmin(
        BootstrapAdminRequest request)
    {
        // Initial account creation is deliberately restricted
        // to the local Development environment.
        if (!HttpContext.RequestServices
            .GetRequiredService<IWebHostEnvironment>()
            .IsDevelopment())
        {
            return NotFound();
        }

        string? configuredKey =
            _configuration["Bootstrap:AdminKey"];

        string? suppliedKey =
            Request.Headers["X-Bootstrap-Key"].FirstOrDefault();

        if (string.IsNullOrWhiteSpace(configuredKey) ||
            string.IsNullOrWhiteSpace(suppliedKey))
        {
            return Unauthorized(new
            {
                message = "Invalid bootstrap credentials."
            });
        }

        bool validKey =
            System.Security.Cryptography.CryptographicOperations
                .FixedTimeEquals(
                    Encoding.UTF8.GetBytes(configuredKey),
                    Encoding.UTF8.GetBytes(suppliedKey));

        if (!validKey)
        {
            return Unauthorized(new
            {
                message = "Invalid bootstrap credentials."
            });
        }

        if (string.IsNullOrWhiteSpace(request.FullName) ||
            string.IsNullOrWhiteSpace(request.Email) ||
            string.IsNullOrWhiteSpace(request.ContactNumber) ||
            string.IsNullOrWhiteSpace(request.Username) ||
            string.IsNullOrWhiteSpace(request.TemporaryPassword))
        {
            return BadRequest(new
            {
                message = "All Admin account fields are required."
            });
        }

        if (request.TemporaryPassword.Length < 12)
        {
            return BadRequest(new
            {
                message = "Temporary password must contain at least 12 characters."
            });
        }

        bool adminExists = await _context.StaffAccounts
            .AnyAsync(s => s.Role == "Admin");

        if (adminExists)
        {
            return Conflict(new
            {
                message = "An Admin account already exists. Bootstrap is disabled."
            });
        }

        string email = request.Email.Trim().ToLowerInvariant();
        string username = request.Username.Trim();

        bool emailExists =
            await _context.StaffAccounts.AnyAsync(
                s => s.Email.ToLower() == email) ||
            await _context.Customers.AnyAsync(
                c => c.Email.ToLower() == email);

        if (emailExists)
        {
            return Conflict(new
            {
                message = "An account with this email already exists."
            });
        }

        bool usernameExists = await _context.StaffAccounts
            .AnyAsync(s => s.Username.ToLower() == username.ToLower());

        if (usernameExists)
        {
            return Conflict(new
            {
                message = "This username is already in use."
            });
        }

        var admin = new StaffAccount
        {
            FullName = request.FullName.Trim(),
            Email = email,
            ContactNumber = request.ContactNumber.Trim(),
            Username = username,
            Role = "Admin",
            IsActive = true,
            MustChangePassword = true,
            CreatedAt = DateTime.UtcNow
        };

        admin.PasswordHash = _staffHasher.HashPassword(
            admin,
            request.TemporaryPassword);

        _context.StaffAccounts.Add(admin);
        await _context.SaveChangesAsync();

        return Ok(new
        {
            message = "Initial Admin account created. Sign in and change the temporary password.",
            account = new
            {
                admin.Id,
                admin.FullName,
                admin.Email,
                admin.Username,
                admin.Role,
                admin.MustChangePassword
            }
        });
    }
    [Authorize(Roles = "Staff,Admin")]
    [HttpPost("change-password")]
    public async Task<IActionResult> ChangePassword(
        ChangePasswordRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.CurrentPassword) ||
            string.IsNullOrWhiteSpace(request.NewPassword))
        {
            return BadRequest(new
            {
                message = "Current password and new password are required."
            });
        }

        if (request.NewPassword.Length < 8)
        {
            return BadRequest(new
            {
                message = "New password must contain at least 8 characters."
            });
        }

        if (request.NewPassword == request.CurrentPassword)
        {
            return BadRequest(new
            {
                message = "Your new password must be different from your current password."
            });
        }

        string? idValue = User.FindFirstValue(
            ClaimTypes.NameIdentifier);

        string? role = User.FindFirstValue(ClaimTypes.Role);

        if (!int.TryParse(idValue, out int accountId) ||
            (role != "Staff" && role != "Admin"))
        {
            return Unauthorized(new
            {
                message = "Invalid account authentication."
            });
        }

        var staff = await _context.StaffAccounts
            .FirstOrDefaultAsync(s => s.Id == accountId);

        if (staff == null || !staff.IsActive || staff.Role != role)
        {
            return Unauthorized(new
            {
                message = "Staff account could not be verified."
            });
        }

        var verification = _staffHasher.VerifyHashedPassword(
            staff,
            staff.PasswordHash,
            request.CurrentPassword);

        if (verification == PasswordVerificationResult.Failed)
        {
            return BadRequest(new
            {
                message = "Current password is incorrect."
            });
        }

        staff.PasswordHash = _staffHasher.HashPassword(
            staff,
            request.NewPassword);

        staff.MustChangePassword = false;

        await _context.SaveChangesAsync();

        return Ok(new
        {
            message = "Password changed successfully.",
            login = CreateLoginResponse(
                staff.Id,
                staff.FullName,
                staff.Email,
                staff.Role,
                false)
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

public class ChangePasswordRequest
{
    public string CurrentPassword { get; set; } = string.Empty;
    public string NewPassword { get; set; } = string.Empty;
}

public class BootstrapAdminRequest
{
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string ContactNumber { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string TemporaryPassword { get; set; } = string.Empty;
}
