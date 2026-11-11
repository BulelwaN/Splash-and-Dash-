using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SplashDash.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddVehicleDetailsToBooking : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "VehicleMake",
                table: "Bookings",
                type: "character varying(100)",
                maxLength: 100,
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<string>(
                name: "VehicleModel",
                table: "Bookings",
                type: "character varying(100)",
                maxLength: 100,
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<string>(
                name: "VehicleType",
                table: "Bookings",
                type: "character varying(50)",
                maxLength: 50,
                nullable: false,
                defaultValue: "");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "VehicleMake",
                table: "Bookings");

            migrationBuilder.DropColumn(
                name: "VehicleModel",
                table: "Bookings");

            migrationBuilder.DropColumn(
                name: "VehicleType",
                table: "Bookings");
        }
    }
}
