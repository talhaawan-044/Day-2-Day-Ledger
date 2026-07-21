package com.example.awancoalledger.data

import androidx.room.*

enum class PartyType {
    BUYER, SUPPLIER
}

enum class PaymentType {
    THEY_PAID, I_PAID
}

enum class ExpenseCategory {
    FOOD, TRANSPORT, BUSINESS, UTILITIES, OTHERS
}

enum class ReminderCategory {
    PAYMENT, DELIVERY, STOCK, AUDIT, COLLECTION, MEETING, PERSONAL, GENERAL
}

enum class ReminderPriority {
    NONE, LOW, MEDIUM, HIGH
}

enum class ReminderRecurrence {
    NONE, DAILY, WEEKLY, BI_WEEKLY, MONTHLY, YEARLY, CUSTOM
}

enum class NotificationType {
    SYNC, ADD, DELETE, UPDATE, ERROR, INFO
}

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType = NotificationType.INFO,
    val isRead: Boolean = false,
    val details: String? = null
)

data class CountryConfig(val name: String, val code: String, val maxDigits: Int)

val SUPPORTED_COUNTRIES = listOf(
    CountryConfig("Pakistan", "+92", 10),
    CountryConfig("India", "+91", 10),
    CountryConfig("United States", "+1", 10),
    CountryConfig("United Kingdom", "+44", 11),
    CountryConfig("United Arab Emirates", "+971", 9),
    CountryConfig("Saudi Arabia", "+966", 9),
    CountryConfig("Australia", "+61", 9),
    CountryConfig("Germany", "+49", 11)
)

@Entity(tableName = "parties")
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String,
    val type: PartyType,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partyId")]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int,
    val date: Long = System.currentTimeMillis(),
    val truckNumber: String? = null,
    val mine: String? = null,
    val warehouse: String? = null,
    val weight: Double? = null,
    val rate: Double? = null,
    val fare: Double? = null,
    val advPayment: Double? = null,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partyId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partyId: Int,
    val date: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: PaymentType,
    val note: String? = null,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class PartyWithDetails(
    @Embedded val party: Party,
    @Relation(
        parentColumn = "id",
        entityColumn = "partyId"
    )
    val entries: List<LedgerEntry>,
    @Relation(
        parentColumn = "id",
        entityColumn = "partyId"
    )
    val payments: List<Payment>
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: ExpenseCategory,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "reminder_lists")
data class ReminderList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Int, // Hex Color Int
    val iconName: String, // Name of Material Icon
    val order: Int = 0,
    val isDefault: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ReminderList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("parentId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String? = null,
    val url: String? = null,
    val listId: Int,
    val parentId: Int? = null, // For Sub-tasks
    val dueDate: Long? = null,
    val remindTime: Long? = null,
    val isCompleted: Boolean = false,
    val isFlagged: Boolean = false,
    val priority: ReminderPriority = ReminderPriority.NONE,
    val category: ReminderCategory = ReminderCategory.GENERAL,
    val recurrence: ReminderRecurrence = ReminderRecurrence.NONE,
    val partyId: Int? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mineName: String,
    val totalWeight: Double = 0.0,
    val peakWeight: Double = 0.0, // Used for the "Peak" progress bar
    val lastWarehouse: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "stock_entries",
    foreignKeys = [
        ForeignKey(
            entity = Stock::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stockId")]
)
data class StockEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stockId: Int,
    val weight: Double,
    val warehouse: String,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val date: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val folderId: Int? = null,
    val color: Int? = null,
    val textColor: Int? = null,
    val isLocked: Boolean = false,
    val fontSize: Float? = null,
    val bgImageId: Int? = null,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val plateNumber: String,
    val type: String = "TRUCK", // TRUCK, CAR, BIKE, etc.
    val currentMileage: Double = 0.0,
    val isPrimary: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
@Entity(
    tableName = "fuel_entries",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int = 1, // Default to first vehicle for migration
    val mileage: Double,
    val liters: Double,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "maintenance_entries",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class MaintenanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int = 1, // Default to first vehicle for migration
    val mileage: Double,
    val cost: Double,
    val description: String,
    val type: String = "OIL_CHANGE",
    val date: Long = System.currentTimeMillis(),
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class RecentActivity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val partyName: String,
    val partyType: PartyType,
    val amount: Double,
    val date: Long,
    val isPayment: Boolean,
    val entry: LedgerEntry? = null,
    val payment: Payment? = null
)

fun PartyWithDetails.getBalance(): Double {
    val totalTruckValue = entries.sumOf {
        ((it.weight ?: 0.0) * (it.rate ?: 0.0)) + (it.fare ?: 0.0)
    }
    val totalTheyPaid = payments.filter { it.type == PaymentType.THEY_PAID }.sumOf { it.amount }
    val totalIPaid = payments.filter { it.type == PaymentType.I_PAID }.sumOf { it.amount }

    return if (party.type == PartyType.BUYER) {
        // Buyer: He owes for coal (+), he pays me (-), I give him money (+)
        totalTruckValue - totalTheyPaid + totalIPaid
    } else {
        // Supplier: I owe him for coal (+), I pay him (-), he gives me money (+)
        totalTruckValue - totalIPaid + totalTheyPaid
    }
}
