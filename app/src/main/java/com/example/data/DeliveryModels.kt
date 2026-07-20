package com.example.data

import java.util.UUID

enum class DeliveryStatus(val label: String) {
    PENDING("Pending"),
    SCHEDULED("Scheduled"),
    ASSIGNED("Assigned"),
    ACCEPTED("Driver Accepted"),
    PICKED_UP("Picked Up"),
    IN_TRANSIT("In Transit"),
    ARRIVING("Arriving"),
    DELIVERED("Delivered"),
    FAILED("Delivery Failed"),
    RETURNED("Returned"),
    CANCELLED("Cancelled")
}

data class DeliveryDriver(
    val id: String = UUID.randomUUID().toString(),
    val employeeId: String,
    val name: String,
    val phone: String,
    val email: String,
    val licenseNumber: String,
    val licenseExpiry: String,
    val assignedVehicleReg: String = "",
    val status: String = "Available", // Available, On Delivery, Off Duty
    val completedCount: Int = 0,
    val onTimeRate: Double = 98.0,
    val rating: Double = 4.8,
    val totalDistanceKm: Double = 0.0,
    val totalFuelLitres: Double = 0.0
)

data class DeliveryVehicle(
    val registrationNumber: String,
    val type: String, // Mini Truck, Van, Motorcycle, Heavy Lorry
    val capacityKg: Double,
    val fuelType: String, // Diesel, Petrol, Electric
    val currentMileage: Double,
    val insuranceExpiry: String,
    val nextServiceSchedule: String,
    val assignedDriverId: String = "",
    val status: String = "Available", // Available, In Transit, Maintenance, Off Duty
    val gpsDeviceId: String = ""
)

data class DeliveryRecord(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val deliveryZone: String,
    val scheduledTime: Long,
    val priority: String = "Standard", // Express, Same-Day, Standard, Bulk
    var status: DeliveryStatus = DeliveryStatus.PENDING,
    var assignedDriverId: String? = null,
    var assignedVehicleReg: String? = null,
    val deliveryCharge: Double = 500.0,
    val weightKg: Double = 5.0,
    // Proof of Delivery
    var signaturePath: List<Pair<Float, Float>>? = null,
    var podPhotoUri: String? = null,
    var gpsCoords: String? = null,
    var podTimestamp: Long? = null,
    var driverNotes: String = "",
    var customerNotes: String = "",
    var verifiedOtp: String? = null,
    var isSynced: Boolean = true
)

data class FuelLog(
    val id: String = UUID.randomUUID().toString(),
    val vehicleReg: String,
    val date: Long,
    val litres: Double,
    val costPerLitre: Double,
    val totalCost: Double,
    val currentMileage: Double,
    val receiptPhoto: String = ""
)

data class DeliveryZone(
    val name: String,
    val basePrice: Double,
    val deliveryTimeHours: Double,
    val restrictions: String,
    val managerName: String
)
