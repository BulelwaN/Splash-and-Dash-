
using Microsoft.EntityFrameworkCore;
using SplashDash.Api.Models;

namespace SplashDash.Api.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options)
        : base(options)
    {
    }

    public DbSet<Customer> Customers => Set<Customer>();

    public DbSet<Booking> Bookings => Set<Booking>();
}
