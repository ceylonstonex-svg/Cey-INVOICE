package com.example

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = InvoiceRepository(database.invoiceDao(), database.productDao(), database.erpDao(), database.financialDao())
        val internetService = InternetService()
        val pdfService = PdfService()
        val googleDriveService = com.example.data.GoogleDriveService(applicationContext)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: InvoiceViewModel = viewModel(
                        factory = InvoiceViewModelFactory(application, repository, internetService, pdfService, googleDriveService)
                    )
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        viewModel.initGoogleDrive(applicationContext)
                    }
                    InvoiceAppMain(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.ui.ZenMusicManager.stop()
    }
}

// Global share PDF file helper
fun sharePdfFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Share Invoice PDF")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun shareViaEmail(context: Context, file: File, invoice: com.example.data.InvoiceEntity, grandTotal: Double) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (invoice.customerEmail.isNotBlank()) {
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(invoice.customerEmail.trim()))
            }
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Invoice ${invoice.invoiceNumber} from Ceyvana")
            
            val formattedTotal = String.format(Locale.US, "%,.2f", grandTotal)
            val emailBody = """
                Dear ${invoice.customerName},

                Please find attached your invoice ${invoice.invoiceNumber} for ${invoice.currencySymbol} $formattedTotal.

                Invoice Summary:
                - Invoice Number: ${invoice.invoiceNumber}
                - Total Amount: ${invoice.currencySymbol} $formattedTotal
                
                Thank you for your business!
                
                Best regards,
                Ceyvana Spices
            """.trimIndent()
            
            putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Send Email")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sending email: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun sharePdfViaWhatsApp(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (ex: android.content.ActivityNotFoundException) {
            try {
                // Fallback to WhatsApp Business
                intent.setPackage("com.whatsapp.w4b")
                context.startActivity(intent)
            } catch (ex2: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
                // Fallback to standard chooser
                val chooser = android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share Invoice PDF"
                )
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing to WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun sendWhatsAppTextMessage(context: Context, invoice: com.example.data.InvoiceEntity, grandTotal: Double) {
    try {
        val formattedTotal = String.format(Locale.US, "%,.2f", grandTotal)
        val textMessage = """
            *Dear ${invoice.customerName}*,

            Here is your invoice *${invoice.invoiceNumber}* for *${invoice.currencySymbol} $formattedTotal*.

            *Invoice Details:*
            - Invoice No: ${invoice.invoiceNumber}
            - Total Amount: ${invoice.currencySymbol} $formattedTotal

            Thank you for your business!
            _Ceyvana Spices_
        """.trimIndent()

        val encodedText = android.net.Uri.encode(textMessage)
        val phone = invoice.customerPhone.filter { it.isDigit() }
        
        val uriString = if (phone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$phone&text=$encodedText"
        } else {
            "https://api.whatsapp.com/send?text=$encodedText"
        }
        
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error launching WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun InvoiceShareDialog(
    invoiceWithItems: com.example.data.InvoiceWithLineItems,
    pdfService: com.example.data.PdfService,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val invoice = invoiceWithItems.invoice
    val grandTotal = invoiceWithItems.grandTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Share Invoice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Invoice: #${invoice.invoiceNumber}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Customer: ${invoice.customerName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Total Amount: ${invoice.currencySymbol} ${String.format(Locale.US, "%,.2f", grandTotal)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShareOptionRow(
                        icon = Icons.Default.Send,
                        iconTint = androidx.compose.ui.graphics.Color(0xFF25D366),
                        title = "Send PDF via WhatsApp",
                        subtitle = "Share the invoice PDF document directly",
                        onClick = {
                            val file = pdfService.generateInvoicePdf(context, invoiceWithItems)
                            sharePdfViaWhatsApp(context, file)
                            onDismiss()
                        }
                    )

                    ShareOptionRow(
                        icon = Icons.Outlined.Message,
                        iconTint = androidx.compose.ui.graphics.Color(0xFF075E54),
                        title = "Send Summary via WhatsApp",
                        subtitle = if (invoice.customerPhone.isNotBlank()) "Direct to ${invoice.customerPhone}" else "Share prefilled text summary",
                        onClick = {
                            sendWhatsAppTextMessage(context, invoice, grandTotal)
                            onDismiss()
                        }
                    )

                    ShareOptionRow(
                        icon = Icons.Default.Email,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Send via Email",
                        subtitle = if (invoice.customerEmail.isNotBlank()) "Direct to ${invoice.customerEmail}" else "Pre-fills recipient, subject and PDF",
                        onClick = {
                            val file = pdfService.generateInvoicePdf(context, invoiceWithItems)
                            shareViaEmail(context, file, invoice, grandTotal)
                            onDismiss()
                        }
                    )

                    ShareOptionRow(
                        icon = Icons.Default.Share,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Other Apps / System Share",
                        subtitle = "Open standard Android share dialog",
                        onClick = {
                            val file = pdfService.generateInvoicePdf(context, invoiceWithItems)
                            sharePdfFile(context, file)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ShareOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.12f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InvoiceAppMain(viewModel: InvoiceViewModel, modifier: Modifier = Modifier) {
    val currentInvoice by viewModel.currentInvoice.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentInvoice != null,
            transitionSpec = {
                if (targetState) {
                    // Slide in from right, fade out to left
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    // Slide in from left, fade out to right
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "ScreenTransition"
        ) { isEditing ->
            if (isEditing) {
                InvoiceDetailScreen(viewModel = viewModel)
            } else {
                InvoiceListScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun InvoiceListScreen(viewModel: InvoiceViewModel) {
    val invoices by viewModel.invoicesList.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Invoices, 2 = Dashboard, 3 = Reports, 4 = Products, 5 = POS
    var showGlobalCalculator by remember { mutableStateOf(false) }

    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) {
            invoices
        } else {
            invoices.filter {
                it.invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                        it.invoice.customerName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Calculations for summary card
    val totalInvoiced = remember(invoices) {
        invoices.sumOf { it.grandTotal }
    }
    val outstandingCount = invoices.size

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Invoices") },
                    label = { Text("Invoices") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Products") },
                    label = { Text("Products") }
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "POS") },
                    label = { Text("POS") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { viewModel.createNewInvoice() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("add_invoice_fab")
                        .padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New Invoice"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { selectedTab = it }
                    )
                }
                1 -> {
                    InvoiceListContent(
                        invoices = invoices,
                        filteredInvoices = filteredInvoices,
                        totalInvoiced = totalInvoiced,
                        outstandingCount = outstandingCount,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        viewModel = viewModel
                    )
                }
                2 -> {
                    CeyvanaErpSuiteScreen(viewModel = viewModel)
                }
                3 -> {
                    SpicesReportsScreen(invoices = invoices)
                }
                4 -> {
                    ProductScreen(viewModel = viewModel)
                }
                5 -> {
                    PosScreen(viewModel = viewModel)
                }
            }

            val isMusicPlaying by com.example.ui.ZenMusicManager.isPlayingFlow.collectAsState()

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTab != 0) {
                    FloatingActionButton(
                        onClick = { showGlobalCalculator = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("global_calc_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Open Calculator",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { com.example.ui.ZenMusicManager.togglePlay() },
                        containerColor = if (isMusicPlaying) Color(0xFF0F2E20) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isMusicPlaying) com.example.ui.theme.AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("global_music_fab")
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = "Toggle Background Music",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    if (showGlobalCalculator) {
        SpiceCalculatorSheet(onDismiss = { showGlobalCalculator = false })
    }
}

@Composable
fun InvoiceListContent(
    invoices: List<InvoiceWithLineItems>,
    filteredInvoices: List<InvoiceWithLineItems>,
    totalInvoiced: Double,
    outstandingCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewModel: InvoiceViewModel
) {
    val context = LocalContext.current
    val googleAccount by viewModel.googleAccount.collectAsState()
    var sharingInvoice by remember { mutableStateOf<com.example.data.InvoiceWithLineItems?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Professional Polish Header Card with premium gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Invoice Manager",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Professional Local Billing Portal",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.img_ceyvana_logo),
                        contentDescription = "Ceyvana Logo",
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(3.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dashboard statistics summary block
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Billing Ledger",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rs. " + String.format(Locale.US, "%,.2f", totalInvoiced),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Active Invoices",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$outstandingCount total",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Search Bar & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search by customer or invoice number...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("search_input")
        )

        // Dynamic List view
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = "Empty Invoices",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Invoices Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try adjusting your search query." else "Tap the green '+' button to craft your very first invoice.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredInvoices, key = { _, item -> item.invoice.id }) { _, invoiceWithItems ->
                    val invoice = invoiceWithItems.invoice
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectInvoice(invoiceWithItems) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${invoice.invoiceNumber}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormat.format(Date(invoice.invoiceDate)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = invoice.customerName.ifBlank { "Unassigned Customer" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Currency: ${invoice.currencyCode} (${invoice.currencySymbol})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${invoice.currencySymbol} " + String.format(Locale.US, "%,.2f", invoiceWithItems.grandTotal),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (googleAccount != null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.backupInvoicePdfToGoogleDrive(context, invoiceWithItems)
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("backup_pdf_drive_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = "Backup PDF to Drive",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Quick Share Button
                                    IconButton(
                                        onClick = {
                                            sharingInvoice = invoiceWithItems
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("share_pdf_list_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share PDF",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { viewModel.deleteInvoice(invoiceWithItems) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("delete_invoice_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Invoice",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    sharingInvoice?.let { invoiceWithItems ->
        InvoiceShareDialog(
            invoiceWithItems = invoiceWithItems,
            pdfService = viewModel.pdfService,
            onDismiss = { sharingInvoice = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val invoice by viewModel.currentInvoice.collectAsState()
    val lineItems by viewModel.lineItems.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val products by viewModel.productsList.collectAsState()

    val subTotal by viewModel.computedSubTotal.collectAsState()
    val taxAmount by viewModel.computedTaxAmount.collectAsState()
    val grandTotal by viewModel.computedGrandTotal.collectAsState()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val currentInv = invoice ?: return
    var showShareDialog by remember { mutableStateOf(false) }

    // Setup date dialog triggers
    fun showDatePicker(currentTimestamp: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSelected(selectedCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Details", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.exitEditor() },
                        modifier = Modifier.testTag("exit_editor_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to list")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Form Scroll container
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Billing & Meta Details Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUSTOMER & BILLING DETAILS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                // Elegant Badge matching Design HTML
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Verified",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = currentInv.invoiceNumber,
                                onValueChange = { viewModel.updateInvoiceFields(number = it) },
                                label = { Text("Invoice Number") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invoice_number_input")
                            )

                            OutlinedTextField(
                                value = currentInv.customerName,
                                onValueChange = { viewModel.updateInvoiceFields(name = it) },
                                label = { Text("Customer Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_name_input")
                            )

                            OutlinedTextField(
                                value = currentInv.customerEmail,
                                onValueChange = { viewModel.updateInvoiceFields(email = it) },
                                label = { Text("Customer Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_email_input")
                            )

                            OutlinedTextField(
                                value = currentInv.customerPhone,
                                onValueChange = { viewModel.updateInvoiceFields(phone = it) },
                                label = { Text("Customer Phone") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_phone_input")
                            )

                            // Date pickers row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Invoice Date
                                OutlinedTextField(
                                    value = dateFormat.format(Date(currentInv.invoiceDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Invoice Date") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Select Date",
                                            modifier = Modifier.clickable {
                                                showDatePicker(currentInv.invoiceDate) { viewModel.updateInvoiceFields(invoiceDate = it) }
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showDatePicker(currentInv.invoiceDate) { viewModel.updateInvoiceFields(invoiceDate = it) }
                                        }
                                )

                                // Due Date
                                OutlinedTextField(
                                    value = dateFormat.format(Date(currentInv.dueDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Due Date") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Select Due Date",
                                            modifier = Modifier.clickable {
                                                showDatePicker(currentInv.dueDate) { viewModel.updateInvoiceFields(dueDate = it) }
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showDatePicker(currentInv.dueDate) { viewModel.updateInvoiceFields(dueDate = it) }
                                        }
                                )
                            }

                            // Currency overrides row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentInv.currencyCode,
                                    onValueChange = { viewModel.updateInvoiceFields(currencyCode = it) },
                                    label = { Text("Currency Code") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = currentInv.currencySymbol,
                                    onValueChange = { viewModel.updateInvoiceFields(currencySymbol = it) },
                                    label = { Text("Symbol") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }
                    }
                }

                // Section 2: Line Items Collector
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LINE ITEMS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Button(
                                    onClick = { viewModel.addLineItem() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_line_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item icon", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic loop for items inside card
                            lineItems.forEachIndexed { index, lineItem ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Item #${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { viewModel.deleteLineItem(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Item",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Scoped selected margin state for Ceyvana
                                        var selectedMargin by remember(lineItem.id) { mutableStateOf(0.35) }

                                        // Quick Ceyvana Spices presets row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Presets:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                products.forEach { product ->
                                                    val fullName = "Ceyvana Premium Ceylon ${product.name}"
                                                    val isSelected = lineItem.description == fullName
                                                    
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.clickable {
                                                            viewModel.updateLineItem(
                                                                index,
                                                                description = fullName,
                                                                unit = "gram",
                                                                unitPrice = product.pricePerGram
                                                            )
                                                        }
                                                    ) {
                                                        Text(
                                                            text = product.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = lineItem.description,
                                            onValueChange = { viewModel.updateLineItem(index, description = it) },
                                            placeholder = { Text("Item / Service Description") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Auto-Profit Calculator Section for Ceyvana
                                        val isCeyvana = lineItem.description.contains("Ceyvana", ignoreCase = true)
                                        if (isCeyvana) {
                                            var costInputStr by remember(lineItem.id) {
                                                val cost = lineItem.unitPrice / (1.0 + selectedMargin)
                                                mutableStateOf(if (lineItem.unitPrice > 0) String.format(Locale.US, "%.2f", cost) else "")
                                            }

                                            LaunchedEffect(lineItem.unitPrice, selectedMargin) {
                                                val currentCost = lineItem.unitPrice / (1.0 + selectedMargin)
                                                val currentCostParsed = costInputStr.toDoubleOrNull() ?: 0.0
                                                if (Math.abs(currentCost - currentCostParsed) > 0.01) {
                                                    costInputStr = if (lineItem.unitPrice > 0) String.format(Locale.US, "%.2f", currentCost) else ""
                                                }
                                            }

                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.TrendingUp,
                                                                contentDescription = "Profit Calculator",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Ceyvana Profit Auto-Calculator",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "+${(selectedMargin * 100).toInt()}% PROFIT",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Margin Selector Row (30%, 35%, 40%, 45%)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Margin:",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        val marginOptions = listOf(0.30, 0.35, 0.40, 0.45)
                                                        marginOptions.forEach { margin ->
                                                            val isMarginSelected = Math.abs(selectedMargin - margin) < 0.001
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = if (isMarginSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                                modifier = Modifier.clickable {
                                                                    val previousMargin = selectedMargin
                                                                    val cost = costInputStr.toDoubleOrNull() ?: if (lineItem.unitPrice > 0) (lineItem.unitPrice / (1.0 + previousMargin)) else 0.0
                                                                    selectedMargin = margin
                                                                    if (cost > 0.0) {
                                                                        viewModel.updateLineItem(index, unitPrice = cost * (1.0 + margin))
                                                                    }
                                                                }
                                                            ) {
                                                                Text(
                                                                    text = "${(margin * 100).toInt()}%",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (isMarginSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isMarginSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        OutlinedTextField(
                                                            value = costInputStr,
                                                            onValueChange = { newValue ->
                                                                costInputStr = newValue
                                                                val cost = newValue.toDoubleOrNull()
                                                                if (cost != null) {
                                                                    viewModel.updateLineItem(index, unitPrice = cost * (1.0 + selectedMargin))
                                                                } else {
                                                                    viewModel.updateLineItem(index, unitPrice = 0.0)
                                                                }
                                                            },
                                                            label = { Text("Cost Price (Rs.)", fontSize = 11.sp) },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.weight(1.2f)
                                                        )
                                                        
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            val calculatedCost = lineItem.unitPrice / (1.0 + selectedMargin)
                                                            val profit = lineItem.unitPrice - calculatedCost
                                                            Text(
                                                                text = "Selling Price:",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "Rs. " + String.format(Locale.US, "%,.2f", lineItem.unitPrice),
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onBackground
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Profit: Rs. " + String.format(Locale.US, "%,.2f", profit),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF047857)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Unit Selector Row (Pack, Gram, Kilogram)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Unit:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val unitOptions = listOf("pack", "gram", "kilogram")
                                            unitOptions.forEach { opt ->
                                                val isUnitSelected = lineItem.unit.equals(opt, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isUnitSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.clickable {
                                                        viewModel.updateLineItem(index, unit = opt)
                                                    }
                                                ) {
                                                    Text(
                                                        text = opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isUnitSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isUnitSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = lineItem.quantity.toString(),
                                                onValueChange = {
                                                    val qty = it.toIntOrNull() ?: 0
                                                    viewModel.updateLineItem(index, quantity = qty)
                                                },
                                                label = { Text("Qty (${lineItem.unit})") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = if (lineItem.unitPrice == 0.0) "" else lineItem.unitPrice.toString(),
                                                onValueChange = {
                                                    val price = it.toDoubleOrNull() ?: 0.0
                                                    viewModel.updateLineItem(index, unitPrice = price)
                                                },
                                                label = { Text("Unit Price") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1.5f)
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .align(Alignment.CenterVertically),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text("Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = "${currentInv.currencySymbol} " + String.format(Locale.US, "%,.2f", lineItem.quantity * lineItem.unitPrice),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Pricing Overrides & Financial Totals Block
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ADJUSTMENTS & RATES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (currentInv.discount == 0.0) "" else currentInv.discount.toString(),
                                    onValueChange = {
                                        val disc = it.toDoubleOrNull() ?: 0.0
                                        viewModel.updateInvoiceFields(discount = disc)
                                    },
                                    label = { Text("Discount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("discount_input")
                                )

                                OutlinedTextField(
                                    value = if (currentInv.deliveryCharge == 0.0) "" else currentInv.deliveryCharge.toString(),
                                    onValueChange = {
                                        val deliv = it.toDoubleOrNull() ?: 0.0
                                        viewModel.updateInvoiceFields(delivery = deliv)
                                    },
                                    label = { Text("Delivery Charge") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("delivery_input")
                                )

                                OutlinedTextField(
                                    value = currentInv.taxRate.toString(),
                                    onValueChange = {
                                        val rate = it.toDoubleOrNull() ?: 0.08
                                        viewModel.updateInvoiceFields(taxRate = rate)
                                    },
                                    label = { Text("Tax Rate (0.08)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tax_rate_input")
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            // Reactive Financial breakdown
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currentInv.currencySymbol} " + String.format(Locale.US, "%,.2f", subTotal), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("- ${currentInv.currencySymbol} " + String.format(Locale.US, "%,.2f", currentInv.discount), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Delivery Charge:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currentInv.currencySymbol} " + String.format(Locale.US, "%,.2f", currentInv.deliveryCharge), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val taxPct = String.format(Locale.US, "%.0f%%", currentInv.taxRate * 100.0)
                                    Text("Tax ($taxPct):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currentInv.currencySymbol} " + String.format(Locale.US, "%,.2f", taxAmount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Grand Total:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${currentInv.currencySymbol} " + String.format(Locale.US, "%,.2f", grandTotal),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom control panel / status feedback banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (statusMsg.isNotEmpty()) {
                    Text(
                        text = statusMsg,
                        fontSize = 12.sp,
                        color = if (statusMsg.contains("Success", true) || statusMsg.contains("saved", true) || statusMsg.contains("Online", true))
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Check internet (fast diagnostic)
                    IconButton(
                        onClick = { viewModel.checkNetworkConnectivity(context) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .weight(1f)
                            .testTag("check_internet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Check Connection",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Cloud sync post check
                    IconButton(
                        onClick = { viewModel.syncInvoiceCloud(context) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .weight(1f)
                            .testTag("sync_cloud_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Sync Cloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Native PDF generator and immediate share sheets
                    Button(
                        onClick = {
                            showShareDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(2.5f)
                            .testTag("generate_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Share", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save invoice
                    Button(
                        onClick = { viewModel.saveInvoice() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(3f)
                            .testTag("save_invoice_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (showShareDialog) {
        val currentInvoiceWithItems = viewModel.getInvoiceWithItemsForPdf()
        if (currentInvoiceWithItems != null) {
            InvoiceShareDialog(
                invoiceWithItems = currentInvoiceWithItems,
                pdfService = viewModel.pdfService,
                onDismiss = { showShareDialog = false }
            )
        } else {
            Toast.makeText(context, "Please enter some invoice details first", Toast.LENGTH_SHORT).show()
            showShareDialog = false
        }
    }
}
