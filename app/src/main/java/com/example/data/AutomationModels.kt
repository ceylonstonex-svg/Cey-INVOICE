package com.example.data

data class AutomationRule(
    val id: String,
    val name: String,
    val trigger: String,
    val action: String,
    val category: String,
    val isActive: Boolean = true,
    val lastTriggered: Long = 0L
)

data class ScheduledJob(
    val id: String,
    val name: String,
    val cronExpression: String,
    val lastRun: Long = 0L,
    val nextRun: Long = 0L,
    val status: String = "Scheduled", // Scheduled, Running, Paused
    val type: String // Backup, Report, Stock Check, Customer Promo
)

data class WorkflowApproval(
    val id: String,
    val type: String, // Purchase Order, Large Discount, Refund, Expense, Stock Adjustment, Employee Leave, High-Value Payment
    val description: String,
    val requestedBy: String,
    val amount: Double = 0.0,
    val date: Long,
    val approvalLevelRequired: String, // Manager, Finance Manager, Director, Owner
    val status: String = "Pending", // Pending, Approved, Rejected
    val justification: String = "",
    val approvedBy: String? = null,
    val resolvedDate: Long = 0L
)

data class AutomationLog(
    val id: String,
    val ruleName: String,
    val triggerType: String,
    val executionTime: Long,
    val status: String, // Success, Failed
    val user: String,
    val result: String,
    val errorDetails: String = "",
    val retryCount: Int = 0
)

data class InvoiceNumberConfig(
    val id: String = "DEFAULT",
    val prefix: String = "CEY-",
    val separator: String = "-",
    val useYear: Boolean = true,
    val includeBranch: Boolean = false,
    val branchPrefix: String = "COL-",
    val resetCycle: String = "Yearly", // Daily, Monthly, Yearly
    val lastResetTime: Long = 0L,
    val currentCounter: Int = 1,
    val formatDigits: Int = 6,
    val numberingType: String = "Retail" // Retail, Wholesale, Export, Purchase, DeliveryNote, CreditNote, DebitNote
)

data class StockAlertItem(
    val productId: Int,
    val productName: String,
    val currentStock: Double,
    val reorderLevel: Double,
    val alertLevel: String, // Low Stock, Critical Stock, Out of Stock, Expiring Soon, Expired Products
    val expiryDate: String = "",
    val configThreshold: Double
)

data class SuggestedPurchaseOrder(
    val id: String,
    val productNames: List<String>,
    val quantities: List<Double>,
    val preferredSupplier: String,
    val estimatedCost: Double,
    val leadTimeDays: Int,
    val status: String = "Draft", // Draft, Approved, Sent to Supplier
    val dateGenerated: Long
)
