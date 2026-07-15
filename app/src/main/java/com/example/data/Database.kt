package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val invoiceDate: Long,
    val dueDate: Long,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val currencyCode: String = "LKR",
    val currencySymbol: String = "Rs.",
    val subTotal: Double = 0.0,
    val discount: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val taxRate: Double = 0.08
)

@Entity(tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val unit: String = "pack"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pricePerGram: Double,
    val defaultUnit: String = "gram"
)

data class InvoiceWithLineItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lineItems: List<LineItemEntity>
) {
    // Computed helper properties utilizing high precision calculations
    val taxAmount: Double
        get() = Math.round(invoice.subTotal * invoice.taxRate * 100.0) / 100.0

    val grandTotal: Double
        get() = Math.round((invoice.subTotal - invoice.discount + invoice.deliveryCharge + taxAmount) * 100.0) / 100.0
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun getProductByName(name: String): ProductEntity?
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<InvoiceWithLineItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceById(id: Int): Flow<InvoiceWithLineItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()

    @Query("DELETE FROM line_items")
    suspend fun deleteAllLineItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<LineItemEntity>)

    @Query("DELETE FROM line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteLineItemsForInvoice(invoiceId: Int)

    @Transaction
    suspend fun saveInvoiceWithItems(invoice: InvoiceEntity, items: List<LineItemEntity>): Int {
        val invoiceId = if (invoice.id == 0) {
            insertInvoice(invoice).toInt()
        } else {
            updateInvoice(invoice)
            invoice.id
        }
        deleteLineItemsForInvoice(invoiceId)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        insertLineItems(itemsWithId)
        return invoiceId
    }

    @Transaction
    suspend fun deleteInvoiceWithItems(invoiceWithLineItems: InvoiceWithLineItems) {
        deleteLineItemsForInvoice(invoiceWithLineItems.invoice.id)
        deleteInvoice(invoiceWithLineItems.invoice)
    }
}

@Database(entities = [InvoiceEntity::class, LineItemEntity::class, ProductEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "invoice_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
