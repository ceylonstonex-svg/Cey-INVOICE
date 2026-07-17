package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class InvoiceViewModel(
    private val repository: InvoiceRepository,
    private val internetService: InternetService,
    val pdfService: PdfService,
    val googleDriveService: GoogleDriveService
) : ViewModel() {

    // List of all invoices
    val invoicesList: StateFlow<List<InvoiceWithLineItems>> = repository.allInvoices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productsList: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            try {
                val current = repository.allProducts.first()
                if (current.isEmpty()) {
                    val defaults = listOf(
                        ProductEntity(name = "Cinnamon", pricePerGram = 15.0),
                        ProductEntity(name = "Black Pepper", pricePerGram = 8.0),
                        ProductEntity(name = "Cardamom", pricePerGram = 35.0),
                        ProductEntity(name = "Cloves", pricePerGram = 18.0),
                        ProductEntity(name = "Nutmeg", pricePerGram = 20.0)
                    )
                    defaults.forEach { repository.insertProduct(it) }
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to initialize product defaults"
            }
        }
    }

    fun saveProduct(name: String, pricePerGram: Double, id: Int = 0) {
        viewModelScope.launch {
            if (id == 0) {
                repository.insertProduct(ProductEntity(name = name, pricePerGram = pricePerGram))
                _statusMessage.value = "Product '$name' added to catalog"
            } else {
                repository.updateProduct(ProductEntity(id = id, name = name, pricePerGram = pricePerGram))
                _statusMessage.value = "Product '$name' updated"
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _statusMessage.value = "Product '${product.name}' removed from catalog"
        }
    }

    // Editing states (null means we are viewing the invoice list)
    private val _currentInvoice = MutableStateFlow<InvoiceEntity?>(null)
    val currentInvoice: StateFlow<InvoiceEntity?> = _currentInvoice.asStateFlow()

    private val _lineItems = MutableStateFlow<List<LineItemEntity>>(emptyList())
    val lineItems: StateFlow<List<LineItemEntity>> = _lineItems.asStateFlow()

    // Status messages and sync progress
    private val _statusMessage = MutableStateFlow<String>("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Real-time calculations using high-precision calculations
    val computedSubTotal: StateFlow<Double> = _lineItems
        .map { items ->
            items.sumOf { it.quantity * it.unitPrice }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val computedTaxAmount: StateFlow<Double> = combine(computedSubTotal, _currentInvoice) { subTotal, invoice ->
        val rate = invoice?.taxRate ?: 0.08
        Math.round(subTotal * rate * 100.0) / 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val computedGrandTotal: StateFlow<Double> = combine(
        computedSubTotal,
        computedTaxAmount,
        _currentInvoice
    ) { subTotal, tax, invoice ->
        if (invoice == null) 0.0 else {
            val discount = invoice.discount
            val delivery = invoice.deliveryCharge
            Math.round((subTotal - discount + delivery + tax) * 100.0) / 100.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun createNewInvoice() {
        val nextInvoiceNum = "INV-${System.currentTimeMillis().toString().takeLast(5)}"
        _currentInvoice.value = InvoiceEntity(
            invoiceNumber = nextInvoiceNum,
            invoiceDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000), // 14 days later
            customerName = "",
            customerEmail = "",
            customerPhone = ""
        )
        _lineItems.value = listOf(
            LineItemEntity(invoiceId = 0, description = "Ceyvana Premium Ceylon Cinnamon", quantity = 10, unitPrice = 2025.0)
        )
        _statusMessage.value = "New Invoice Created"
    }

    fun selectInvoice(invoiceWithItems: InvoiceWithLineItems) {
        _currentInvoice.value = invoiceWithItems.invoice
        _lineItems.value = invoiceWithItems.lineItems
        _statusMessage.value = "Loaded Invoice #${invoiceWithItems.invoice.invoiceNumber}"
    }

    fun exitEditor() {
        _currentInvoice.value = null
        _lineItems.value = emptyList()
        _statusMessage.value = ""
    }

    fun updateInvoiceFields(
        number: String? = null,
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        discount: Double? = null,
        delivery: Double? = null,
        taxRate: Double? = null,
        currencyCode: String? = null,
        currencySymbol: String? = null,
        invoiceDate: Long? = null,
        dueDate: Long? = null
    ) {
        val current = _currentInvoice.value ?: return
        _currentInvoice.value = current.copy(
            invoiceNumber = number ?: current.invoiceNumber,
            customerName = name ?: current.customerName,
            customerEmail = email ?: current.customerEmail,
            customerPhone = phone ?: current.customerPhone,
            discount = discount ?: current.discount,
            deliveryCharge = delivery ?: current.deliveryCharge,
            taxRate = taxRate ?: current.taxRate,
            currencyCode = currencyCode ?: current.currencyCode,
            currencySymbol = currencySymbol ?: current.currencySymbol,
            invoiceDate = invoiceDate ?: current.invoiceDate,
            dueDate = dueDate ?: current.dueDate
        )
    }

    fun addLineItem() {
        val currentList = _lineItems.value.toMutableList()
        currentList.add(LineItemEntity(invoiceId = _currentInvoice.value?.id ?: 0, description = "New Service", quantity = 1, unitPrice = 0.0, unit = "pack"))
        _lineItems.value = currentList
    }

    fun updateLineItem(index: Int, description: String? = null, quantity: Int? = null, unitPrice: Double? = null, unit: String? = null) {
        val currentList = _lineItems.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            val newDesc = description ?: item.description
            val newUnit = unit ?: item.unit
            val newQty = quantity ?: item.quantity

            // Find a matching product from our pre-set catalog
            val matchedProduct = productsList.value.find { 
                newDesc.contains(it.name, ignoreCase = true) 
            }

            var newPrice = unitPrice ?: item.unitPrice

            // If the unit is "gram" and we have a matched product:
            // Auto-populate the unit price to the pre-set price if it wasn't manually overridden in this call.
            if (matchedProduct != null && newUnit.equals("gram", ignoreCase = true)) {
                if (unitPrice == null && (quantity != null || description != null || unit != null)) {
                    newPrice = matchedProduct.pricePerGram
                }
            }

            currentList[index] = item.copy(
                description = newDesc,
                quantity = newQty,
                unitPrice = newPrice,
                unit = newUnit
            )
            _lineItems.value = currentList
        }
    }

    fun deleteLineItem(index: Int) {
        val currentList = _lineItems.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _lineItems.value = currentList
        }
    }

    fun saveInvoice() {
        val current = _currentInvoice.value ?: return
        val items = _lineItems.value

        viewModelScope.launch {
            val updatedInvoice = current.copy(
                subTotal = computedSubTotal.value
            )
            val invoiceId = repository.saveInvoice(updatedInvoice, items)
            _statusMessage.value = "Invoice Saved Successfully"
            _currentInvoice.value = null
            _lineItems.value = emptyList()
        }
    }

    suspend fun saveInvoiceForPos(invoice: InvoiceEntity, items: List<LineItemEntity>): Int {
        return repository.saveInvoice(invoice, items)
    }

    fun deleteInvoice(invoiceWithLineItems: InvoiceWithLineItems) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceWithLineItems)
            _statusMessage.value = "Invoice Deleted"
        }
    }

    fun checkNetworkConnectivity(context: Context) {
        viewModelScope.launch {
            _statusMessage.value = "Checking Connection..."
            val hasLocal = internetService.isLocalNetworkConnected(context)
            if (!hasLocal) {
                _statusMessage.value = "No network adapter active (Offline)"
                return@launch
            }
            val hasWan = internetService.verifyWanAccess()
            if (hasWan) {
                _statusMessage.value = "Online: WAN connection available to Google DNS."
            } else {
                _statusMessage.value = "Limited Connection: Local adapter active, but WAN unreachable."
            }
        }
    }

    fun syncInvoiceCloud(context: Context) {
        val current = _currentInvoice.value ?: return
        val items = _lineItems.value

        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Syncing invoice to httpbin.org..."

            val invoiceWithItems = InvoiceWithLineItems(
                invoice = current.copy(subTotal = computedSubTotal.value),
                lineItems = items
            )

            val success = internetService.syncInvoice(invoiceWithItems)
            _isSyncing.value = false
            if (success) {
                _statusMessage.value = "Cloud Sync Success: Echo received from backend!"
            } else {
                _statusMessage.value = "Sync Failed: Server returned error or is unreachable."
            }
        }
    }

    fun getInvoiceWithItemsForPdf(): InvoiceWithLineItems? {
        val current = _currentInvoice.value ?: return null
        val items = _lineItems.value
        return InvoiceWithLineItems(
            invoice = current.copy(subTotal = computedSubTotal.value),
            lineItems = items
        )
    }

    fun generatePdf(context: Context): File? {
        val invoiceWithItems = getInvoiceWithItemsForPdf() ?: return null
        return try {
            pdfService.generateInvoicePdf(context, invoiceWithItems)
        } catch (e: Exception) {
            _statusMessage.value = "PDF Generation Error: ${e.localizedMessage}"
            null
        }
    }

    // Google Drive integration
    private val _googleAccount = MutableStateFlow<com.google.android.gms.auth.api.signin.GoogleSignInAccount?>(null)
    val googleAccount: StateFlow<com.google.android.gms.auth.api.signin.GoogleSignInAccount?> = _googleAccount.asStateFlow()

    fun updateGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        _googleAccount.value = account
        if (account != null) {
            _statusMessage.value = "Google Drive Linked: ${account.email}"
        } else {
            _statusMessage.value = "Google Drive Unlinked"
        }
    }

    fun initGoogleDrive(context: Context) {
        val account = googleDriveService.getLastSignedInAccount()
        if (account != null) {
            _googleAccount.value = account
        }
    }

    fun backupToGoogleDrive(context: Context) {
        val account = _googleAccount.value
        if (account == null) {
            _statusMessage.value = "Backup Failed: Link Google Drive first."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Saving backup JSON to Google Drive..."
            
            val accessToken = googleDriveService.getAccessToken(account)
            if (accessToken == null) {
                _statusMessage.value = "Authorization Failed: Token unavailable"
                _isSyncing.value = false
                return@launch
            }

            try {
                val invoices = invoicesList.value
                val products = productsList.value
                val jsonBackup = googleDriveService.exportDatabaseToJson(invoices, products)
                val fileData = jsonBackup.toByteArray(Charsets.UTF_8)

                val fileId = googleDriveService.uploadOrUpdateFile(
                    accessToken = accessToken,
                    fileName = "ceyvana_invoice_backup.json",
                    mimeType = "application/json",
                    fileData = fileData
                )

                _isSyncing.value = false
                if (fileId != null) {
                    _statusMessage.value = "Success: Backup saved to Google Drive!"
                } else {
                    _statusMessage.value = "Backup Failed: Drive upload error."
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _statusMessage.value = "Backup Error: ${e.localizedMessage}"
            }
        }
    }

    fun restoreFromGoogleDrive(context: Context) {
        val account = _googleAccount.value
        if (account == null) {
            _statusMessage.value = "Restore Failed: Link Google Drive first."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Searching backup in Google Drive..."

            val accessToken = googleDriveService.getAccessToken(account)
            if (accessToken == null) {
                _statusMessage.value = "Authorization Failed: Token unavailable"
                _isSyncing.value = false
                return@launch
            }

            try {
                val fileId = googleDriveService.searchFileByName(accessToken, "ceyvana_invoice_backup.json")
                if (fileId == null) {
                    _statusMessage.value = "Restore Failed: No backup file found on Drive."
                    _isSyncing.value = false
                    return@launch
                }

                _statusMessage.value = "Downloading backup data..."
                val bytes = googleDriveService.downloadFile(accessToken, fileId)
                if (bytes == null) {
                    _statusMessage.value = "Restore Failed: Could not download backup file."
                    _isSyncing.value = false
                    return@launch
                }

                _statusMessage.value = "Restoring database content..."
                val jsonStr = String(bytes, Charsets.UTF_8)
                val rootJson = org.json.JSONObject(jsonStr)

                // Parse Products
                val productsListRestored = mutableListOf<ProductEntity>()
                val productsArr = rootJson.optJSONArray("products")
                if (productsArr != null) {
                    for (i in 0 until productsArr.length()) {
                        val pObj = productsArr.getJSONObject(i)
                        productsListRestored.add(
                            ProductEntity(
                                id = pObj.optInt("id", 0),
                                name = pObj.getString("name"),
                                pricePerGram = pObj.getDouble("pricePerGram"),
                                defaultUnit = pObj.optString("defaultUnit", "gram")
                            )
                        )
                    }
                }

                // Parse Invoices & Items
                val invoicesListRestored = mutableListOf<InvoiceWithLineItems>()
                val invoicesArr = rootJson.optJSONArray("invoices")
                if (invoicesArr != null) {
                    for (i in 0 until invoicesArr.length()) {
                        val iObj = invoicesArr.getJSONObject(i)
                        val inv = InvoiceEntity(
                            id = iObj.optInt("id", 0),
                            invoiceNumber = iObj.getString("invoiceNumber"),
                            invoiceDate = iObj.optLong("invoiceDate", System.currentTimeMillis()),
                            dueDate = iObj.optLong("dueDate", System.currentTimeMillis()),
                            customerName = iObj.getString("customerName"),
                            customerEmail = iObj.optString("customerEmail", ""),
                            customerPhone = iObj.optString("customerPhone", ""),
                            currencyCode = iObj.optString("currencyCode", "LKR"),
                            currencySymbol = iObj.optString("currencySymbol", "Rs."),
                            subTotal = iObj.optDouble("subTotal", 0.0),
                            discount = iObj.optDouble("discount", 0.0),
                            deliveryCharge = iObj.optDouble("deliveryCharge", 0.0),
                            taxRate = iObj.optDouble("taxRate", 0.15)
                        )

                        val itemsListRestored = mutableListOf<LineItemEntity>()
                        val itemsArr = iObj.optJSONArray("lineItems")
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val itemObj = itemsArr.getJSONObject(j)
                                itemsListRestored.add(
                                    LineItemEntity(
                                        id = itemObj.optInt("id", 0),
                                        invoiceId = itemObj.optInt("invoiceId", 0),
                                        description = itemObj.getString("description"),
                                        quantity = itemObj.optInt("quantity", 1),
                                        unitPrice = itemObj.getDouble("unitPrice"),
                                        unit = itemObj.optString("unit", "gram")
                                    )
                                )
                            }
                        }

                        invoicesListRestored.add(
                            InvoiceWithLineItems(
                                invoice = inv,
                                lineItems = itemsListRestored
                            )
                        )
                    }
                }

                repository.restoreDatabase(invoicesListRestored, productsListRestored)
                _isSyncing.value = false
                _statusMessage.value = "Success: Restored ${invoicesListRestored.size} invoices and ${productsListRestored.size} products!"
            } catch (e: Exception) {
                _isSyncing.value = false
                _statusMessage.value = "Restore Error: ${e.localizedMessage}"
            }
        }
    }

    fun backupInvoicePdfToGoogleDrive(context: Context, invoiceWithItems: InvoiceWithLineItems) {
        val account = _googleAccount.value
        if (account == null) {
            _statusMessage.value = "Backup Failed: Link Google Drive first."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Generating invoice PDF..."
            
            val file = try {
                pdfService.generateInvoicePdf(context, invoiceWithItems)
            } catch (e: Exception) {
                _isSyncing.value = false
                _statusMessage.value = "PDF Generation Failed: ${e.localizedMessage}"
                null
            } ?: return@launch

            _statusMessage.value = "Uploading PDF to Google Drive..."
            val accessToken = googleDriveService.getAccessToken(account)
            if (accessToken == null) {
                _statusMessage.value = "Authorization Failed: Token unavailable"
                _isSyncing.value = false
                return@launch
            }

            try {
                val fileData = file.readBytes()
                val fileName = "Invoice_${invoiceWithItems.invoice.invoiceNumber}.pdf"
                
                val fileId = googleDriveService.uploadOrUpdateFile(
                    accessToken = accessToken,
                    fileName = fileName,
                    mimeType = "application/pdf",
                    fileData = fileData
                )

                _isSyncing.value = false
                if (fileId != null) {
                    _statusMessage.value = "Success: Saved '$fileName' to Google Drive!"
                } else {
                    _statusMessage.value = "Backup Failed: Drive upload error."
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _statusMessage.value = "Upload Error: ${e.localizedMessage}"
            }
        }
    }
}

class InvoiceViewModelFactory(
    private val repository: InvoiceRepository,
    private val internetService: InternetService,
    private val pdfService: PdfService,
    private val googleDriveService: GoogleDriveService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InvoiceViewModel(repository, internetService, pdfService, googleDriveService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
