package com.example.data

import kotlinx.coroutines.flow.Flow

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao
) {
    val allInvoices: Flow<List<InvoiceWithLineItems>> = invoiceDao.getAllInvoices()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

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
