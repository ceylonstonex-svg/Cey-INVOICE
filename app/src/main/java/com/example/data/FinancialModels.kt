package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "coa_accounts")
data class CoaAccountEntity(
    @PrimaryKey val code: String, // e.g. "1010", "1020"
    val name: String,
    val category: String, // Asset, Liability, Equity, Income, Expense
    val balance: Double = 0.0,
    val isSystem: Boolean = false,
    val bankAccountNo: String = ""
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val narration: String,
    val reference: String = "",
    val isApproved: Boolean = true,
    val approvedBy: String = "Admin",
    val type: String = "Manual" // Manual, Sales, Purchase, Expense, Payroll, Depreciation
)

@Entity(tableName = "journal_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class JournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryId: Int,
    val accountCode: String,
    val debit: Double,
    val credit: Double
)

data class JournalEntryWithLines(
    @Embedded val entry: JournalEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val lines: List<JournalLineEntity>
)

@Entity(tableName = "customer_receivables")
data class CustomerReceivableEntity(
    @PrimaryKey val customerId: Int,
    val balance: Double = 0.0,
    val creditLimit: Double = 50000.0,
    val lastPaymentDate: Long = 0L,
    val dueDate: Long = 0L
)

@Entity(tableName = "supplier_bills")
data class SupplierBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int,
    val billNumber: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val date: Long,
    val dueDate: Long,
    val status: String = "Unpaid" // Unpaid, Partially Paid, Paid
)

@Entity(tableName = "fixed_assets")
data class FixedAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val purchaseDate: Long,
    val cost: Double,
    val usefulLifeYears: Int = 5,
    val depreciationMethod: String = "Straight-Line", // Straight-Line, Declining Balance
    val currentValue: Double,
    val supplier: String = "",
    val accumulatedDepreciation: Double = 0.0,
    val disposedDate: Long = 0L
)

@Entity(tableName = "payrolls")
data class PayrollRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val employeeName: String,
    val month: String, // e.g., "July 2026"
    val basicSalary: Double,
    val allowances: Double = 0.0,
    val overtime: Double = 0.0,
    val bonuses: Double = 0.0,
    val deductions: Double = 0.0,
    val advances: Double = 0.0,
    val status: String = "Draft" // Draft, Paid
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountCode: String,
    val amount: Double,
    val month: String, // e.g., "2026-07"
    val department: String = "Operations" // Retail, Wholesale, Export, Operations
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user: String,
    val action: String,
    val previousValue: String = "",
    val newValue: String = "",
    val timestamp: Long,
    val device: String = "POS Terminal"
)

@Dao
interface FinancialDao {
    @Query("SELECT * FROM coa_accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<CoaAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: CoaAccountEntity)

    @Update
    suspend fun updateAccount(account: CoaAccountEntity)

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryWithLines>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()

    @Query("SELECT * FROM customer_receivables")
    fun getAllCustomerReceivables(): Flow<List<CustomerReceivableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerReceivable(rec: CustomerReceivableEntity)

    @Query("SELECT * FROM supplier_bills ORDER BY date DESC")
    fun getAllSupplierBills(): Flow<List<SupplierBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierBill(bill: SupplierBillEntity)

    @Query("SELECT * FROM fixed_assets ORDER BY purchaseDate DESC")
    fun getAllFixedAssets(): Flow<List<FixedAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedAsset(asset: FixedAssetEntity)

    @Query("SELECT * FROM payrolls ORDER BY month DESC")
    fun getAllPayrollRecords(): Flow<List<PayrollRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollRecord(payroll: PayrollRecordEntity)

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}
