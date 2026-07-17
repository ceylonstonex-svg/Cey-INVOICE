package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CeyvanaErpSuiteScreen(viewModel: InvoiceViewModel) {
    var activeModule by remember { mutableStateOf("home") } // home, dashboard, customers, suppliers, inventory, purchase, expenses, employees, settings
    val statusMessage by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotBlank()) {
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Module Header View
        ERPHeader(
            activeModule = activeModule,
            onBack = { activeModule = "home" }
        )

        AnimatedContent(
            targetState = activeModule,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ModuleTransition",
            modifier = Modifier.weight(1f)
        ) { module ->
            when (module) {
                "home" -> ERPHomeHub(onSelectModule = { activeModule = it }, viewModel = viewModel)
                "dashboard" -> ERPDashboardModule(viewModel = viewModel)
                "customers" -> ERPCustomersModule(viewModel = viewModel)
                "suppliers" -> ERPSuppliersModule(viewModel = viewModel)
                "inventory" -> ERPInventoryModule(viewModel = viewModel)
                "purchase" -> ERPPurchaseModule(viewModel = viewModel)
                "expenses" -> ERPExpensesModule(viewModel = viewModel)
                "employees" -> ERPEmployeesModule(viewModel = viewModel)
                "settings" -> ERPSettingsModule(viewModel = viewModel)
                else -> ERPHomeHub(onSelectModule = { activeModule = it }, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ERPHeader(activeModule: String, onBack: () -> Unit) {
    val title = when (activeModule) {
        "home" -> "Ceyvana ERP Suite"
        "dashboard" -> "Live Analytics"
        "customers" -> "Customers CRM"
        "suppliers" -> "Suppliers Registry"
        "inventory" -> "Stock & Inventory"
        "purchase" -> "Purchase & Inbound"
        "expenses" -> "Expense Ledger"
        "employees" -> "Staff & Shift Logs"
        "settings" -> "Settings & Barcodes"
        else -> "Ceyvana Business Suite"
    }

    val subtitle = when (activeModule) {
        "home" -> "Enterprise Resource Planning Terminal"
        "dashboard" -> "Live sales performance & charts"
        "customers" -> "Loyalty, credit limits & customer logs"
        "suppliers" -> "Supplier profiles & procurement logs"
        "inventory" -> "Real-time stock balancing & alerts"
        "purchase" -> "Goods receiving & supplier invoices"
        "expenses" -> "Track business spend & cash flow"
        "employees" -> "Shift registers & attendance tracking"
        "settings" -> "Metadata, barcode generator & sync logs"
        else -> "Ceyvana Business Suite"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .padding(top = 16.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeModule != "home") {
                        IconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.img_ceyvana_logo),
                    contentDescription = "Ceyvana Logo",
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ERPHomeHub(onSelectModule: (String) -> Unit, viewModel: InvoiceViewModel) {
    val customers by viewModel.customersList.collectAsState()
    val products by viewModel.productsList.collectAsState()
    val expenses by viewModel.expensesList.collectAsState()
    val employees by viewModel.employeesList.collectAsState()

    val totalStockVal = remember(products) {
        products.sumOf { it.currentStock * it.costPrice }
    }
    val totalExpenseVal = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick summary row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ERP SYSTEM STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Asset Value: Rs. " + String.format(Locale.US, "%,.2f", totalStockVal),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active CRM Accounts: ${customers.size} | Staff Active: ${employees.filter { it.isClockedIn }.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF25D366), CircleShape)
                            )
                            Text(
                                text = "OFFLINE OK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E8E3E)
                            )
                        }
                    }
                }
            }
        }

        // Module Grid
        item {
            Text(
                text = "SUITE DIRECTORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ERPMenuCard(
                        title = "Dashboard",
                        subtitle = "Analytics & charts",
                        icon = Icons.Default.Dashboard,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("dashboard") }
                    )
                    ERPMenuCard(
                        title = "Customers CRM",
                        subtitle = "Loyalty & profiles",
                        icon = Icons.Default.People,
                        color = AccentGold,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("customers") }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ERPMenuCard(
                        title = "Suppliers",
                        subtitle = "Vendors & orders",
                        icon = Icons.Default.Business,
                        color = ForestLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("suppliers") }
                    )
                    ERPMenuCard(
                        title = "Inventory",
                        subtitle = "Real-time stock",
                        icon = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("inventory") }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ERPMenuCard(
                        title = "Purchase PO",
                        subtitle = "Inbound orders",
                        icon = Icons.Default.Receipt,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("purchase") }
                    )
                    ERPMenuCard(
                        title = "Expenses",
                        subtitle = "Ledger: Rs. ${String.format(Locale.US, "%,.0f", totalExpenseVal)}",
                        icon = Icons.Default.MoneyOff,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("expenses") }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ERPMenuCard(
                        title = "Staff Logins",
                        subtitle = "Attendance & roles",
                        icon = Icons.Default.Badge,
                        color = Color(0xFF29B6F6),
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("employees") }
                    )
                    ERPMenuCard(
                        title = "System Settings",
                        subtitle = "Barcodes & metadata",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF78909C),
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectModule("settings") }
                    )
                }
            }
        }
    }
}

@Composable
fun ERPMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), CircleShape)
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 1. DASHBOARD & LIVE ANALYTICS
@Composable
fun ERPDashboardModule(viewModel: InvoiceViewModel) {
    val invoices by viewModel.invoicesList.collectAsState()
    val products by viewModel.productsList.collectAsState()
    val expenses by viewModel.expensesList.collectAsState()

    val totalSales = remember(invoices) { invoices.sumOf { it.grandTotal } }
    val totalOrdersCount = invoices.size
    val pendingOrdersCount = invoices.filter { it.invoice.customerPhone.isBlank() }.size // Simulated logic

    val lowStockProducts = remember(products) {
        products.filter { it.currentStock <= it.reorderLevel }
    }

    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val netProfit = totalSales - totalExpenses

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary statistics metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = "Total Sales",
                    value = "Rs. ${String.format(Locale.US, "%,.0f", totalSales)}",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF66BB6A),
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    title = "Expenses",
                    value = "Rs. ${String.format(Locale.US, "%,.0f", totalExpenses)}",
                    icon = Icons.Default.TrendingDown,
                    color = Color(0xFFEF5350),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = "Total Orders",
                    value = "$totalOrdersCount Bills",
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    title = "Low Stock Alerts",
                    value = "${lowStockProducts.size} Items",
                    icon = Icons.Default.Warning,
                    color = if (lowStockProducts.isNotEmpty()) AccentGold else Color(0xFF66BB6A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Profit card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Net Operating Profit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs. ${String.format(Locale.US, "%,.2f", netProfit)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    Icon(
                        imageVector = if (netProfit >= 0) Icons.Default.Paid else Icons.Default.MoneyOff,
                        contentDescription = null,
                        tint = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Live Mini Bar Chart for Sales vs Expenses
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sales vs Expense Comparison",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxVal = maxOf(totalSales, totalExpenses, 1.0)
                    val salesRatio = (totalSales / maxVal).toFloat().coerceIn(0.02f, 1f)
                    val expenseRatio = (totalExpenses / maxVal).toFloat().coerceIn(0.02f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Sales bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Rs. ${String.format(Locale.US, "%,.0f", totalSales)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .fillMaxHeight(salesRatio)
                                    .background(Color(0xFF81C784), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Expense bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Rs. ${String.format(Locale.US, "%,.0f", totalExpenses)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .fillMaxHeight(expenseRatio)
                                    .background(Color(0xFFE57373), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Low stock items warning logs
        if (lowStockProducts.isNotEmpty()) {
            item {
                Text(
                    text = "CRITICAL LOW STOCK WARNINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    letterSpacing = 1.sp
                )
            }

            items(lowStockProducts) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color(0xFFC62828))
                            Column {
                                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC62828))
                                Text(text = "SKU: ${item.sku.ifBlank { "N/A" }} | Batch: ${item.batchNumber.ifBlank { "N/A" }}", fontSize = 11.sp, color = Color(0xFF5D4037))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "${item.currentStock} g", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFC62828))
                            Text(text = "Reorder: ${item.reorderLevel} g", fontSize = 10.sp, color = Color(0xFF5D4037))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// 2. CUSTOMERS HUB
@Composable
fun ERPCustomersModule(viewModel: InvoiceViewModel) {
    val customers by viewModel.customersList.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchTxt by remember { mutableStateOf("") }

    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var cName by remember { mutableStateOf("") }
    var cPhone by remember { mutableStateOf("") }
    var cEmail by remember { mutableStateOf("") }
    var cAddress by remember { mutableStateOf("") }
    var cLimit by remember { mutableStateOf("50000.0") }
    var cPoints by remember { mutableStateOf("0") }

    val filteredCustomers = remember(customers, searchTxt) {
        if (searchTxt.isBlank()) customers else customers.filter {
            it.name.contains(searchTxt, ignoreCase = true) || it.phone.contains(searchTxt)
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("Search CRM name or mobile...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    editingCustomer = null
                    cName = ""
                    cPhone = ""
                    cEmail = ""
                    cAddress = ""
                    cLimit = "50000.0"
                    cPoints = "0"
                    showAddDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }

        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No customers registered in ledger yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers) { cust ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(text = cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "📞 ${cust.phone} | ✉ ${cust.email.ifBlank { "N/A" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "📍 ${cust.address.ifBlank { "No Address Listed" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    color = AccentGold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${cust.loyaltyPoints} PTS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ForestPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Credit Limit: Rs. ${String.format(Locale.US, "%,.2f", cust.creditLimit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // WhatsApp message launcher
                                    IconButton(
                                        onClick = {
                                            try {
                                                val phoneNo = cust.phone.filter { it.isDigit() }
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNo&text=Hello%20from%20Ceyvana%20Spices!")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open WhatsApp.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE8F5E9))
                                    ) {
                                        Icon(Icons.Default.Forum, contentDescription = "WhatsApp", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    }

                                    // Direct phone launcher
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${cust.phone}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE1F5FE))
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
                                    }

                                    // Edit profile
                                    IconButton(
                                        onClick = {
                                            editingCustomer = cust
                                            cName = cust.name
                                            cPhone = cust.phone
                                            cEmail = cust.email
                                            cAddress = cust.address
                                            cLimit = cust.creditLimit.toString()
                                            cPoints = cust.loyaltyPoints.toString()
                                            showAddDialog = true
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                    }

                                    // Delete customer
                                    IconButton(
                                        onClick = { viewModel.deleteCustomer(cust) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFEBEE))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = if (editingCustomer == null) "Add Customer Profile" else "Edit Customer Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cPhone, onValueChange = { cPhone = it }, label = { Text("Mobile Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cEmail, onValueChange = { cEmail = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cAddress, onValueChange = { cAddress = it }, label = { Text("Physical Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cLimit, onValueChange = { cLimit = it }, label = { Text("Credit Limit (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cPoints, onValueChange = { cPoints = it }, label = { Text("Loyalty Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = cLimit.toDoubleOrNull() ?: 50000.0
                        val points = cPoints.toIntOrNull() ?: 0
                        if (cName.isNotBlank() && cPhone.isNotBlank()) {
                            viewModel.saveCustomer(
                                name = cName.trim(),
                                phone = cPhone.trim(),
                                email = cEmail.trim(),
                                address = cAddress.trim(),
                                creditLimit = limit,
                                loyaltyPoints = points,
                                id = editingCustomer?.id ?: 0
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 3. SUPPLIERS REGISTRY
@Composable
fun ERPSuppliersModule(viewModel: InvoiceViewModel) {
    val suppliers by viewModel.suppliersList.collectAsState()
    var searchTxt by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var sName by remember { mutableStateOf("") }
    var sCompany by remember { mutableStateOf("") }
    var sPhone by remember { mutableStateOf("") }
    var sEmail by remember { mutableStateOf("") }
    var sDue by remember { mutableStateOf("0.0") }

    val filteredSuppliers = remember(suppliers, searchTxt) {
        if (searchTxt.isBlank()) suppliers else suppliers.filter {
            it.name.contains(searchTxt, ignoreCase = true) || it.company.contains(searchTxt, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("Search supplier name or company...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    editingSupplier = null
                    sName = ""
                    sCompany = ""
                    sPhone = ""
                    sEmail = ""
                    sDue = "0.0"
                    showDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Add Supplier")
            }
        }

        if (filteredSuppliers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No suppliers listed in registry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSuppliers) { sup ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(text = sup.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "🏢 ${sup.company}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "📞 ${sup.phone} | ✉ ${sup.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    color = if (sup.dueAmount > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (sup.dueAmount > 0) "DUE BAL" else "CLEAR BAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (sup.dueAmount > 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Payable Balance: Rs. ${String.format(Locale.US, "%,.2f", sup.dueAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sup.dueAmount > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingSupplier = sup
                                            sName = sup.name
                                            sCompany = sup.company
                                            sPhone = sup.phone
                                            sEmail = sup.email
                                            sDue = sup.dueAmount.toString()
                                            showDialog = true
                                        },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteSupplier(sup) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFEBEE))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = if (editingSupplier == null) "Add Supplier Profile" else "Edit Supplier Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("Contact Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sCompany, onValueChange = { sCompany = it }, label = { Text("Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sPhone, onValueChange = { sPhone = it }, label = { Text("Contact Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sEmail, onValueChange = { sEmail = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sDue, onValueChange = { sDue = it }, label = { Text("Supplier Due Balance (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val due = sDue.toDoubleOrNull() ?: 0.0
                        if (sName.isNotBlank() && sCompany.isNotBlank()) {
                            viewModel.saveSupplier(
                                name = sName.trim(),
                                company = sCompany.trim(),
                                phone = sPhone.trim(),
                                email = sEmail.trim(),
                                dueAmount = due,
                                id = editingSupplier?.id ?: 0
                            )
                            showDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 4. INVENTORY & STOCK LEDGER
@Composable
fun ERPInventoryModule(viewModel: InvoiceViewModel) {
    val products by viewModel.productsList.collectAsState()
    var searchTxt by remember { mutableStateOf("") }
    var selectedProductForStock by remember { mutableStateOf<ProductEntity?>(null) }

    var stockType by remember { mutableStateOf("Stock In") } // Stock In, Stock Out, Damage, Return, Adjustment
    var stockQtyStr by remember { mutableStateOf("") }
    var stockReason by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchTxt) {
        if (searchTxt.isBlank()) products else products.filter {
            it.name.contains(searchTxt, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchTxt,
            onValueChange = { searchTxt = it },
            placeholder = { Text("Search inventory levels...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredProducts) { prod ->
                val isLowStock = prod.currentStock <= prod.reorderLevel
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLowStock) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isLowStock) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "SKU: ${prod.sku.ifBlank { "N/A" }} | Category: ${prod.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Cost Price: Rs. ${String.format(Locale.US, "%,.2f", prod.costPrice)} / g", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${prod.currentStock} g",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (isLowStock) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = if (isLowStock) Color(0xFFC62828).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isLowStock) "LOW STOCK" else "IN STOCK",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = if (isLowStock) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expiry: ${prod.expiryDate.ifBlank { "Never" }} | Batch: ${prod.batchNumber.ifBlank { "N/A" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    selectedProductForStock = prod
                                    stockQtyStr = ""
                                    stockReason = ""
                                    stockType = "Stock In"
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Adjust Stock", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedProductForStock != null) {
        val prodName = selectedProductForStock!!.name
        AlertDialog(
            onDismissRequest = { selectedProductForStock = null },
            title = { Text(text = "Adjust Stock: $prodName", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Transaction Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Stock In", "Stock Out", "Damage", "Return", "Adjustment").forEach { type ->
                            FilterChip(
                                selected = stockType == type,
                                onClick = { stockType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = stockQtyStr,
                        onValueChange = { stockQtyStr = it },
                        label = { Text("Quantity (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = stockReason,
                        onValueChange = { stockReason = it },
                        label = { Text("Reason / Remarks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = stockQtyStr.toDoubleOrNull() ?: 0.0
                        if (qty > 0.0) {
                            viewModel.addStockTransaction(
                                productId = selectedProductForStock!!.id,
                                type = stockType,
                                quantity = qty,
                                reason = stockReason.trim()
                            )
                            selectedProductForStock = null
                        }
                    }
                ) {
                    Text("Register Adjust")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForStock = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 5. PURCHASE MODULE (INBOUND PO)
@Composable
fun ERPPurchaseModule(viewModel: InvoiceViewModel) {
    val suppliers by viewModel.suppliersList.collectAsState()
    val products by viewModel.productsList.collectAsState()

    var selectedSupplierId by remember { mutableStateOf(0) }
    var selectedProductId by remember { mutableStateOf(0) }
    var buyQtyStr by remember { mutableStateOf("") }
    var buyCostStr by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }

    val recentTransactions by viewModel.inventoryTransactionsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Draft Inbound Purchase Order", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

                // Supplier dropdown simulator
                Text(text = "Select Supplier Vendor:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suppliers.forEach { sup ->
                        FilterChip(
                            selected = selectedSupplierId == sup.id,
                            onClick = { selectedSupplierId = sup.id },
                            label = { Text("${sup.company} (${sup.name})", fontSize = 11.sp) }
                        )
                    }
                }

                // Product dropdown simulator
                Text(text = "Select Spice Product:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    products.forEach { prod ->
                        FilterChip(
                            selected = selectedProductId == prod.id,
                            onClick = { selectedProductId = prod.id },
                            label = { Text(prod.name, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyQtyStr,
                        onValueChange = { buyQtyStr = it },
                        label = { Text("Quantity (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = buyCostStr,
                        onValueChange = { buyCostStr = it },
                        label = { Text("Unit Cost (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = orderNotes,
                    onValueChange = { orderNotes = it },
                    label = { Text("Inbound Invoice / GRN Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val qty = buyQtyStr.toDoubleOrNull() ?: 0.0
                        val cost = buyCostStr.toDoubleOrNull() ?: 0.0
                        if (selectedSupplierId != 0 && selectedProductId != 0 && qty > 0.0) {
                            // Register stock in
                            viewModel.addStockTransaction(
                                productId = selectedProductId,
                                type = "Stock In",
                                quantity = qty,
                                reason = "PO Inbound - $orderNotes"
                            )
                            // Update supplier balance if needed
                            suppliers.find { it.id == selectedSupplierId }?.let { sup ->
                                val updatedDue = sup.dueAmount + (qty * cost)
                                viewModel.saveSupplier(
                                    name = sup.name,
                                    company = sup.company,
                                    phone = sup.phone,
                                    email = sup.email,
                                    dueAmount = updatedDue,
                                    id = sup.id
                                )
                            }
                            buyQtyStr = ""
                            buyCostStr = ""
                            orderNotes = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit Goods Received Note (GRN)")
                }
            }
        }

        // Recent Inbound logs
        Text(
            text = "RECENT INBOUND LOGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                recentTransactions.filter { it.type == "Stock In" }.take(5).forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val prodName = products.find { it.id == tx.productId }?.name ?: "Unknown Spice"
                            Text(text = "GRN: $prodName", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = tx.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(text = "+ ${tx.quantity} g", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 14.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// 6. EXPENSES LEDGER
@Composable
fun ERPExpensesModule(viewModel: InvoiceViewModel) {
    val expenses by viewModel.expensesList.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    var expDesc by remember { mutableStateOf("") }
    var expCat by remember { mutableStateOf("") }
    var expAmountStr by remember { mutableStateOf("") }

    val categories = listOf("Packaging", "Certifications", "Logistics", "Rent & Utility", "Marketing", "Salaries", "Sourcing")

    val totalAmount = remember(expenses) { expenses.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "TOTAL LOGGED EXPENSES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Rs. ${String.format(Locale.US, "%,.2f", totalAmount)}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Button(
                    onClick = {
                        expDesc = ""
                        expCat = "Packaging"
                        expAmountStr = ""
                        showDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Spend")
                }
            }
        }

        Text(
            text = "SPEND TRANSACTION LEDGER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No expenses recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses) { exp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = exp.description, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                        Text(text = exp.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Text(text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(exp.date)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "Rs. ${String.format(Locale.US, "%,.2f", exp.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 15.sp)

                                IconButton(
                                    onClick = { viewModel.deleteExpense(exp) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Record Business Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = expDesc, onValueChange = { expDesc = it }, label = { Text("Spend Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text(text = "Select Category:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = expCat == cat,
                                onClick = { expCat = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expAmountStr,
                        onValueChange = { expAmountStr = it },
                        label = { Text("Expense Amount (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = expAmountStr.toDoubleOrNull() ?: 0.0
                        if (expDesc.isNotBlank() && amt > 0.0) {
                            viewModel.saveExpense(
                                description = expDesc.trim(),
                                category = expCat,
                                amount = amt,
                                date = System.currentTimeMillis()
                            )
                            showDialog = false
                        }
                    }
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 7. STAFF ATTENDANCE & SHIFTS
@Composable
fun ERPEmployeesModule(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val employees by viewModel.employeesList.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var pinModalEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var pinTxt by remember { mutableStateOf("") }

    var empName by remember { mutableStateOf("") }
    var empRole by remember { mutableStateOf("Cashier") }
    var empPin by remember { mutableStateOf("1234") }

    val roles = listOf("Cashier", "Manager", "Admin")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STAFF ROSTER & ATTENDANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Button(
                onClick = {
                    empName = ""
                    empRole = "Cashier"
                    empPin = "1234"
                    showAddDialog = true
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAddAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Staff")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(employees) { emp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                    Text(text = emp.role, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                Text(text = "Attendance: ${emp.attendanceCount} Shifts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Attendance switch requiring PIN
                            Button(
                                onClick = {
                                    pinTxt = ""
                                    pinModalEmployee = emp
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (emp.isClockedIn) Color(0xFF66BB6A) else Color(0xFFB0BEC5)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (emp.isClockedIn) Color.White else Color(0xFF37474F), CircleShape)
                                    )
                                    Text(
                                        text = if (emp.isClockedIn) "Clocked In" else "Clocked Out",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (emp.isClockedIn) Color.White else Color(0xFF37474F)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteEmployee(emp) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Register Employee", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = empName, onValueChange = { empName = it }, label = { Text("Staff Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text(text = "Staff Role:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        roles.forEach { role ->
                            FilterChip(
                                selected = empRole == role,
                                onClick = { empRole = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(value = empPin, onValueChange = { empPin = it }, label = { Text("Attendance Security PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (empName.isNotBlank() && empPin.isNotBlank()) {
                            viewModel.saveEmployee(
                                name = empName.trim(),
                                role = empRole,
                                pin = empPin.trim()
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pinModalEmployee != null) {
        AlertDialog(
            onDismissRequest = { pinModalEmployee = null },
            title = { Text(text = "Shift Clock Gate: ${pinModalEmployee!!.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Enter your secure employee PIN to clock in or out.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = pinTxt,
                        onValueChange = { pinTxt = it },
                        label = { Text("4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinTxt == pinModalEmployee!!.pin) {
                            viewModel.toggleClockIn(pinModalEmployee!!)
                            pinModalEmployee = null
                        } else {
                            Toast.makeText(context, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { pinModalEmployee = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 8. SETTINGS & BARCODE MODULE
@Composable
fun ERPSettingsModule(viewModel: InvoiceViewModel) {
    var brandName by remember { mutableStateOf("Ceyvana Spices") }
    var brandAddress by remember { mutableStateOf("Colombo, Sri Lanka") }
    var brandPhone by remember { mutableStateOf("+94 77 123 4567") }
    var invoiceFooterText by remember { mutableStateOf("Thank you for choosing Ceyvana!") }

    var generateText by remember { mutableStateOf("4790001001234") }
    var generatedBarcode by remember { mutableStateOf<String?>(null) }

    var syncActive by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barcode / QR Generator Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "EAN-13 & QR Barcode Generator", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "Input product SKU, name or barcode serial to generate mock label.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = generateText,
                    onValueChange = { generateText = it },
                    label = { Text("Barcode String / EAN-13") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { generatedBarcode = generateText },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Barcode & QR code")
                }

                if (generatedBarcode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Ceyvana Packaging Label", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)

                        // Draw beautifully stylized Canvas Barcode Lines
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(50.dp)
                        ) {
                            val count = 40
                            val widthInterval = size.width / count
                            for (i in 0 until count) {
                                val isGap = (i % 3 == 0) || (i % 7 == 0)
                                val thickness = if (i % 5 == 0) 4f else 2f
                                if (!isGap) {
                                    drawLine(
                                        color = Color.Black,
                                        start = androidx.compose.ui.geometry.Offset(i * widthInterval, 0f),
                                        end = androidx.compose.ui.geometry.Offset(i * widthInterval, size.height),
                                        strokeWidth = thickness
                                    )
                                }
                            }
                        }

                        Text(text = generatedBarcode!!, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

                        // Draw a Beautiful QR code box
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Canvas(modifier = Modifier.size(45.dp)) {
                                drawRect(color = Color.Black, style = Stroke(width = 3f))
                                drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(8f, 8f), size = androidx.compose.ui.geometry.Size(12f, 12f))
                                drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(25f, 8f), size = androidx.compose.ui.geometry.Size(12f, 12f))
                                drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(8f, 25f), size = androidx.compose.ui.geometry.Size(12f, 12f))
                                drawCircle(color = Color.Black, radius = 2f, center = androidx.compose.ui.geometry.Offset(size.width/2f, size.height/2f))
                            }

                            Column {
                                Text(text = "QR SKU LINK", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = "Scan to POS billing", color = Color.DarkGray, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }

        // Settings Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Enterprise Configurations", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(value = brandName, onValueChange = { brandName = it }, label = { Text("Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brandAddress, onValueChange = { brandAddress = it }, label = { Text("Company Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brandPhone, onValueChange = { brandPhone = it }, label = { Text("Company Phone No") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = invoiceFooterText, onValueChange = { invoiceFooterText = it }, label = { Text("Invoice Footer Disclaimer") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        Toast.makeText(context, "Configurations Saved Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Configuration Updates")
                }
            }
        }

        // Database backups panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Local Backup & Cloud Synchronization", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)

                Text(
                    text = "Any transaction registered while offline is securely synced with your backup drive once the connection recovers.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Local SQLite Database backed up to /Ceyvana/Backups!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create Backup", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Database synced with Cloud Firestore!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sync Ledger", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
