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
    public DbSet<StaffAccount> StaffAccounts => Set<StaffAccount>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<StaffAccount>()
            .HasIndex(s => s.Username)
            .IsUnique();

        modelBuilder.Entity<StaffAccount>()
            .HasIndex(s => s.Email)
            .IsUnique();
    }
}
