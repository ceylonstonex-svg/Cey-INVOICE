package com.example.data

import kotlinx.coroutines.flow.Flow

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val erpDao: ErpDao,
    private val financialDao: FinancialDao
) {
    val allInvoices: Flow<List<InvoiceWithLineItems>> = invoiceDao.getAllInvoices()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allCustomers: Flow<List<CustomerEntity>> = erpDao.getAllCustomers()
    val allSuppliers: Flow<List<SupplierEntity>> = erpDao.getAllSuppliers()
    val allExpenses: Flow<List<ExpenseEntity>> = erpDao.getAllExpenses()
    val allEmployees: Flow<List<EmployeeEntity>> = erpDao.getAllEmployees()
    val allInventoryTransactions: Flow<List<InventoryTransactionEntity>> = erpDao.getAllInventoryTransactions()

    // Financial Module Flows
    val allAccounts: Flow<List<CoaAccountEntity>> = financialDao.getAllAccounts()
    val allJournalEntries: Flow<List<JournalEntryWithLines>> = financialDao.getAllJournalEntries()
    val allCustomerReceivables: Flow<List<CustomerReceivableEntity>> = financialDao.getAllCustomerReceivables()
    val allSupplierBills: Flow<List<SupplierBillEntity>> = financialDao.getAllSupplierBills()
    val allFixedAssets: Flow<List<FixedAssetEntity>> = financialDao.getAllFixedAssets()
    val allPayrollRecords: Flow<List<PayrollRecordEntity>> = financialDao.getAllPayrollRecords()
    val allBudgets: Flow<List<BudgetEntity>> = financialDao.getAllBudgets()
    val allAuditLogs: Flow<List<AuditLogEntity>> = financialDao.getAllAuditLogs()

    suspend fun insertAccount(account: CoaAccountEntity) {
        financialDao.insertAccount(account)
    }

    suspend fun updateAccount(account: CoaAccountEntity) {
        financialDao.updateAccount(account)
    }

    suspend fun insertJournalEntry(entry: JournalEntryEntity, lines: List<JournalLineEntity>) {
        val entryId = financialDao.insertJournalEntry(entry).toInt()
        val linesWithId = lines.map { it.copy(entryId = entryId) }
        financialDao.insertJournalLines(linesWithId)
    }

    suspend fun deleteAllJournalEntries() {
        financialDao.deleteAllJournalEntries()
    }

    suspend fun insertCustomerReceivable(rec: CustomerReceivableEntity) {
        financialDao.insertCustomerReceivable(rec)
    }

    suspend fun insertSupplierBill(bill: SupplierBillEntity) {
        financialDao.insertSupplierBill(bill)
    }

    suspend fun insertFixedAsset(asset: FixedAssetEntity) {
        financialDao.insertFixedAsset(asset)
    }

    suspend fun insertPayrollRecord(payroll: PayrollRecordEntity) {
        financialDao.insertPayrollRecord(payroll)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        financialDao.insertBudget(budget)
    }

    suspend fun insertAuditLog(log: AuditLogEntity) {
        financialDao.insertAuditLog(log)
    }

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
