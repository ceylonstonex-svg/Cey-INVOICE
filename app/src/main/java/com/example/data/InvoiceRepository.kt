package com.example.data

import kotlinx.coroutines.flow.Flow

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val erpDao: ErpDao
) {
    val allInvoices: Flow<List<InvoiceWithLineItems>> = invoiceDao.getAllInvoices()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allCustomers: Flow<List<CustomerEntity>> = erpDao.getAllCustomers()
    val allSuppliers: Flow<List<SupplierEntity>> = erpDao.getAllSuppliers()
    val allExpenses: Flow<List<ExpenseEntity>> = erpDao.getAllExpenses()
    val allEmployees: Flow<List<EmployeeEntity>> = erpDao.getAllEmployees()
    val allInventoryTransactions: Flow<List<InventoryTransactionEntity>> = erpDao.getAllInventoryTransactions()

    fun getInvoiceById(id: Int): Flow<InvoiceWithLineItems?> = invoiceDao.getInvoiceById(id)

    suspend fun saveInvoice(invoice: InvoiceEntity, items: List<LineItemEntity>): Int {
        return invoiceDao.saveInvoiceWithItems(invoice, items)
    }

    suspend fun deleteInvoice(invoiceWithLineItems: InvoiceWithLineItems) {
        invoiceDao.deleteInvoiceWithItems(invoiceWithLineItems)
    }

    suspend fun insertProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun insertCustomer(customer: CustomerEntity) {
        erpDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        erpDao.deleteCustomer(customer)
    }

    suspend fun insertSupplier(supplier: SupplierEntity) {
        erpDao.insertSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) {
        erpDao.deleteSupplier(supplier)
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        erpDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        erpDao.deleteExpense(expense)
    }

    suspend fun insertEmployee(employee: EmployeeEntity) {
        erpDao.insertEmployee(employee)
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        erpDao.deleteEmployee(employee)
    }

    suspend fun insertInventoryTransaction(tx: InventoryTransactionEntity) {
        erpDao.insertInventoryTransaction(tx)
    }

    suspend fun restoreDatabase(
        invoices: List<InvoiceWithLineItems>,
        products: List<ProductEntity>
    ) {
        invoiceDao.deleteAllInvoices()
        invoiceDao.deleteAllLineItems()
        productDao.deleteAllProducts()

        for (prod in products) {
            productDao.insertProduct(prod)
        }

        for (invWithItems in invoices) {
            invoiceDao.insertInvoice(invWithItems.invoice)
            invoiceDao.insertLineItems(invWithItems.lineItems)
        }
    }
}
