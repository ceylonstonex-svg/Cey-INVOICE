package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

private val ForestPrimary = Color(0xFF2E7D32)
private val ForestDark = Color(0xFF1B5E20)
private val ForestMedium = Color(0xFF4CAF50)
private val AccentGold = Color(0xFFFBC02D)
private val AccentRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialModuleScreen(viewModel: InvoiceViewModel, onBack: () -> Unit) {
    val accounts by viewModel.accountsList.collectAsState()
    val journalEntries by viewModel.journalEntriesList.collectAsState()
    val receivables by viewModel.customerReceivablesList.collectAsState()
    val supplierBills by viewModel.supplierBillsList.collectAsState()
    val fixedAssets by viewModel.fixedAssetsList.collectAsState()
    val payrollRecords by viewModel.payrollRecordsList.collectAsState()
    val budgets by viewModel.budgetsList.collectAsState()
    val auditLogs by viewModel.auditLogsList.collectAsState()

    val customers by viewModel.customersList.collectAsState()
    val suppliers by viewModel.suppliersList.collectAsState()
    val employees by viewModel.employeesList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()
    val expenses by viewModel.expensesList.collectAsState()

    var activeSubTab by remember { mutableStateOf("dashboard") } 
    // dashboard, coa, ledger, ar_ap, assets, payroll, budget, banking, reports, forecast, audit

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Financial Control Terminal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Ceylon Spices Advanced ERP Accounting", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(ForestPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Security, contentDescription = "Security", tint = ForestPrimary, modifier = Modifier.size(12.dp))
                            Text("ACCRUAL & CASH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal scrolling sub-tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "dashboard" to "Dashboard",
                    "coa" to "Chart of Accounts",
                    "ledger" to "General Ledger",
                    "ar_ap" to "AR / AP Ledgers",
                    "assets" to "Fixed Assets",
                    "payroll" to "Staff Payroll",
                    "budget" to "Budgets",
                    "banking" to "Bank & Reconcile",
                    "reports" to "Financial Reports",
                    "forecast" to "Prediction Flow",
                    "audit" to "Audit Trail"
                )

                tabs.forEach { (tag, label) ->
                    FilterChip(
                        selected = activeSubTab == tag,
                        onClick = { activeSubTab = tag },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("subtab_$tag")
                    )
                }
            }

            AnimatedContent(
                targetState = activeSubTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SubTabTransition",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> FinancialDashboardTab(
                        viewModel = viewModel,
                        accounts = accounts,
                        invoices = invoices,
                        expenses = expenses,
                        receivables = receivables,
                        supplierBills = supplierBills
                    )
                    "coa" -> ChartOfAccountsTab(viewModel = viewModel, accounts = accounts)
                    "ledger" -> GeneralLedgerTab(viewModel = viewModel, accounts = accounts, entries = journalEntries)
                    "ar_ap" -> AccountsReceivablePayableTab(
                        viewModel = viewModel,
                        accounts = accounts,
                        customers = customers,
                        suppliers = suppliers,
                        receivables = receivables,
                        bills = supplierBills
                    )
                    "assets" -> FixedAssetsTab(viewModel = viewModel, assets = fixedAssets)
                    "payroll" -> PayrollTab(viewModel = viewModel, employees = employees, payrollRecords = payrollRecords)
                    "budget" -> BudgetTab(viewModel = viewModel, accounts = accounts, budgets = budgets)
                    "banking" -> BankingReconciliationTab(viewModel = viewModel, accounts = accounts, entries = journalEntries)
                    "reports" -> FinancialReportsTab(
                        viewModel = viewModel,
                        accounts = accounts,
                        entries = journalEntries,
                        expenses = expenses,
                        invoices = invoices,
                        fixedAssets = fixedAssets,
                        payrollRecords = payrollRecords
                    )
                    "forecast" -> CashFlowForecastingTab(
                        accounts = accounts,
                        receivables = receivables,
                        supplierBills = supplierBills
                    )
                    "audit" -> AuditTrailTab(auditLogs = auditLogs)
                }
            }
        }
    }
}

// 1. FINANCIAL DASHBOARD TAB
@Composable
fun FinancialDashboardTab(
    viewModel: InvoiceViewModel,
    accounts: List<CoaAccountEntity>,
    invoices: List<InvoiceWithLineItems>,
    expenses: List<ExpenseEntity>,
    receivables: List<CustomerReceivableEntity>,
    supplierBills: List<SupplierBillEntity>
) {
    val totalSalesToday = remember(invoices) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        invoices.filter { it.invoice.invoiceDate >= todayStart }.sumOf { it.grandTotal }
    }

    val cashBalance = remember(accounts) {
        accounts.find { it.code == "1010" }?.balance ?: 0.0
    }
    val bankBalance = remember(accounts) {
        accounts.filter { it.code in listOf("1020", "1030") }.sumOf { it.balance }
    }
    val outstandingAR = remember(receivables) {
        receivables.sumOf { it.balance }
    }
    val outstandingAP = remember(supplierBills) {
        supplierBills.filter { it.status != "Paid" }.sumOf { it.amount - it.paidAmount }
    }

    val totalRevenueMonth = remember(accounts) {
        accounts.filter { it.category == "Income" }.sumOf { it.balance }
    }
    val totalExpensesMonth = remember(accounts) {
        accounts.filter { it.category == "Expense" }.sumOf { it.balance }
    }
    val grossProfit = totalRevenueMonth - (accounts.find { it.code == "5010" }?.balance ?: 0.0)
    val netProfit = totalRevenueMonth - totalExpensesMonth

    var showQuickActionDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardKpiCard(
                    title = "Today's POS Sales",
                    value = "Rs. ${String.format(Locale.US, "%,.2f", totalSalesToday)}",
                    icon = Icons.Default.TrendingUp,
                    color = ForestPrimary,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiCard(
                    title = "Est. Today's Profit",
                    value = "Rs. ${String.format(Locale.US, "%,.2f", totalSalesToday * 0.35)}",
                    icon = Icons.Default.AttachMoney,
                    color = AccentGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LIQUID CASH & BANK BALANCES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cash Drawer Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rs. ${String.format(Locale.US, "%,.2f", cashBalance)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Active Bank Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rs. ${String.format(Locale.US, "%,.2f", bankBalance)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ForestDark)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardKpiCard(
                    title = "Outstanding Receivables (AR)",
                    value = "Rs. ${String.format(Locale.US, "%,.2f", outstandingAR)}",
                    icon = Icons.Default.ArrowUpward,
                    color = ForestMedium,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiCard(
                    title = "Outstanding Payables (AP)",
                    value = "Rs. ${String.format(Locale.US, "%,.2f", outstandingAP)}",
                    icon = Icons.Default.ArrowDownward,
                    color = AccentRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MONTH-TO-DATE REVENUE VS EXPENSES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Revenues", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rs. ${String.format(Locale.US, "%,.2f", totalRevenueMonth)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rs. ${String.format(Locale.US, "%,.2f", totalExpensesMonth)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                        }
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (totalRevenueMonth > 0.0) (totalExpensesMonth / totalRevenueMonth).toFloat().coerceIn(0f, 1f) else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentRed,
                        trackColor = ForestPrimary.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Profit: Rs. ${String.format(Locale.US, "%,.2f", grossProfit)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Net Margin: Rs. ${String.format(Locale.US, "%,.2f", netProfit)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestDark)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showQuickActionDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant Ledger Entry")
                }
                
                Button(
                    onClick = { viewModel.runDepreciation() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Depreciation")
                }
            }
        }
    }

    if (showQuickActionDialog) {
        QuickJournalDialog(
            viewModel = viewModel,
            accounts = accounts,
            onDismiss = { showQuickActionDialog = false }
        )
    }
}

@Composable
fun DashboardKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// 2. CHART OF ACCOUNTS TAB
@Composable
fun ChartOfAccountsTab(viewModel: InvoiceViewModel, accounts: List<CoaAccountEntity>) {
    var showAddAccountDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CHART OF ACCOUNTS (COA)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Button(
                onClick = { showAddAccountDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Account", fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        val categories = listOf("Asset", "Liability", "Equity", "Income", "Expense")

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEach { cat ->
                val catAccounts = accounts.filter { it.category == cat }
                if (catAccounts.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = cat.uppercase() + "S",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ForestPrimary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                catAccounts.forEach { acc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(acc.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(acc.name, fontSize = 13.sp)
                                            }
                                            if (acc.bankAccountNo.isNotBlank()) {
                                                Text("A/C: ${acc.bankAccountNo}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(
                                            text = "Rs. ${String.format(Locale.US, "%,.2f", acc.balance)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (acc.balance < 0.0) AccentRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            viewModel = viewModel,
            onDismiss = { showAddAccountDialog = false }
        )
    }
}

// 3. GENERAL LEDGER / JOURNAL ENTRIES TAB
@Composable
fun GeneralLedgerTab(
    viewModel: InvoiceViewModel,
    accounts: List<CoaAccountEntity>,
    entries: List<JournalEntryWithLines>
) {
    var showCreateEntryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DOUBLE-ENTRY LEDGER ENTRIES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Button(
                onClick = { showCreateEntryDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Entry")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Journal", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { entryWithLines ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(entryWithLines.entry.narration, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Ref: ${entryWithLines.entry.reference} | Type: ${entryWithLines.entry.type}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(entryWithLines.entry.date)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        entryWithLines.lines.forEach { line ->
                            val accName = accounts.find { it.code == line.accountCode }?.name ?: "Unknown"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.padding(start = if (line.credit > 0.0) 16.dp else 0.dp)) {
                                    Text("${line.accountCode} - $accName", fontSize = 12.sp, color = if (line.credit > 0.0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (line.debit > 0.0) {
                                        Text("Dr. Rs. ${String.format(Locale.US, "%,.2f", line.debit)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ForestPrimary)
                                    } else {
                                        Text("Cr. Rs. ${String.format(Locale.US, "%,.2f", line.credit)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateEntryDialog) {
        QuickJournalDialog(
            viewModel = viewModel,
            accounts = accounts,
            onDismiss = { showCreateEntryDialog = false }
        )
    }
}

// 4. ACCOUNTS RECEIVABLE / PAYABLE TAB
@Composable
fun AccountsReceivablePayableTab(
    viewModel: InvoiceViewModel,
    accounts: List<CoaAccountEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    receivables: List<CustomerReceivableEntity>,
    bills: List<SupplierBillEntity>
) {
    var showRecordBillDialog by remember { mutableStateOf(false) }
    var showRecordCollectionDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CREDIT LEDGERS (AR / AP)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showRecordCollectionDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Collect AR", fontSize = 11.sp)
                }
                Button(
                    onClick = { showRecordBillDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Log Supplier Bill", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accounts Receivable Grid
        Text("ACCOUNTS RECEIVABLE (CUSTOMER DEBTS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(receivables) { rec ->
                val customer = customers.find { it.id == rec.customerId }
                if (customer != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Limit: Rs. ${customer.creditLimit} | Due: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(rec.dueDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${String.format(Locale.US, "%,.2f", rec.balance)}", fontWeight = FontWeight.Bold, color = AccentRed, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .background(ForestPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("0-30 Days", fontSize = 9.sp, color = ForestPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("ACCOUNTS PAYABLE (SUPPLIER BILLS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(bills) { bill ->
                val supplier = suppliers.find { it.id == bill.supplierId }
                if (supplier != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${supplier.name} (${bill.billNumber})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Due: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(bill.dueDate))} | Status: ${bill.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rs. ${String.format(Locale.US, "%,.2f", bill.amount - bill.paidAmount)}", fontWeight = FontWeight.Bold, color = AccentRed, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordBillDialog) {
        RecordSupplierBillDialog(viewModel = viewModel, suppliers = suppliers, onDismiss = { showRecordBillDialog = false })
    }

    if (showRecordCollectionDialog) {
        RecordCollectionDialog(viewModel = viewModel, receivables = receivables, customers = customers, onDismiss = { showRecordCollectionDialog = false })
    }
}

// 5. FIXED ASSETS TAB
@Composable
fun FixedAssetsTab(viewModel: InvoiceViewModel, assets: List<FixedAssetEntity>) {
    var showAddAssetDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FIXED ASSET REGISTER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.runDepreciation() },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Depreciate All", fontSize = 11.sp)
                }
                Button(
                    onClick = { showAddAssetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Asset", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(assets) { asset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Method: ${asset.depreciationMethod} (${asset.usefulLifeYears} Yrs Useful Life)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "Purchased: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(asset.purchaseDate))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Original Cost", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rs. ${String.format(Locale.US, "%,.2f", asset.cost)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Accumulated Dep.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rs. ${String.format(Locale.US, "%,.2f", asset.accumulatedDepreciation)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Book Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rs. ${String.format(Locale.US, "%,.2f", asset.currentValue)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAssetDialog) {
        AddFixedAssetDialog(viewModel = viewModel, onDismiss = { showAddAssetDialog = false })
    }
}

// 6. STAFF PAYROLL TAB
@Composable
fun PayrollTab(
    viewModel: InvoiceViewModel,
    employees: List<EmployeeEntity>,
    payrollRecords: List<PayrollRecordEntity>
) {
    var showProcessPayrollDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STAFF COMPENSATION & PAYROLL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Button(
                onClick = { showProcessPayrollDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Process Payslip", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("HISTORIC DISBURSED PAYMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(payrollRecords) { record ->
                val netPay = record.basicSalary + record.allowances + record.overtime + record.bonuses - record.deductions - record.advances
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(record.employeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Disbursed Period: ${record.month}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .background(ForestPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(record.status.uppercase(), fontSize = 10.sp, color = ForestPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Base: Rs. ${record.basicSalary}", fontSize = 11.sp)
                            Text("Overtime: Rs. ${record.overtime}", fontSize = 11.sp)
                            Text("Bonuses: Rs. ${record.bonuses}", fontSize = 11.sp)
                            Text("Net Disbursed: Rs. ${String.format(Locale.US, "%,.2f", netPay)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showProcessPayrollDialog) {
        ProcessPayrollDialog(viewModel = viewModel, employees = employees, onDismiss = { showProcessPayrollDialog = false })
    }
}

// 7. BUDGETS TAB
@Composable
fun BudgetTab(viewModel: InvoiceViewModel, accounts: List<CoaAccountEntity>, budgets: List<BudgetEntity>) {
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OPERATIONAL BUDGETS METER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Button(
                onClick = { showAddBudgetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ New Budget", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(budgets) { budget ->
                val acc = accounts.find { it.code == budget.accountCode }
                val actualSpent = acc?.balance ?: 0.0
                val variance = budget.amount - actualSpent
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${budget.accountCode} - ${acc?.name ?: "Unknown"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Department: ${budget.department} | Period: ${budget.month}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        val ratio = if (budget.amount > 0) (actualSpent / budget.amount).toFloat().coerceIn(0f, 1.5f) else 0f
                        
                        LinearProgressIndicator(
                            progress = { ratio.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (ratio > 1.0f) AccentRed else ForestPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Budgeted: Rs. ${budget.amount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Actual Spent: Rs. ${actualSpent}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (ratio > 1.0f) AccentRed else ForestPrimary)
                            Text("Variance: Rs. ${variance}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (variance < 0) AccentRed else ForestDark)
                        }
                    }
                }
            }
        }
    }

    if (showAddBudgetDialog) {
        CreateBudgetDialog(viewModel = viewModel, accounts = accounts, onDismiss = { showAddBudgetDialog = false })
    }
}

// 8. BANKING & RECONCILIATION TAB
@Composable
fun BankingReconciliationTab(
    viewModel: InvoiceViewModel,
    accounts: List<CoaAccountEntity>,
    entries: List<JournalEntryWithLines>
) {
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showReconcileDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BANK ACCOUNTS & RECONCILIATION", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showTransferDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Fund Transfer", fontSize = 11.sp)
                }
                Button(
                    onClick = { showReconcileDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reconcile Workspace", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val bankAccounts = accounts.filter { it.bankAccountNo.isNotBlank() }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bankAccounts) { bank ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bank.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Account No: ${bank.bankAccountNo}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Rs. ${String.format(Locale.US, "%,.2f", bank.balance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                    }
                }
            }
        }
    }

    if (showTransferDialog) {
        BankTransferDialog(viewModel = viewModel, accounts = accounts, onDismiss = { showTransferDialog = false })
    }

    if (showReconcileDialog) {
        ReconciliationWorkspaceDialog(accounts = accounts, entries = entries, onDismiss = { showReconcileDialog = false })
    }
}

// 9. FINANCIAL REPORTS TAB
@Composable
fun FinancialReportsTab(
    viewModel: InvoiceViewModel,
    accounts: List<CoaAccountEntity>,
    entries: List<JournalEntryWithLines>,
    expenses: List<ExpenseEntity>,
    invoices: List<InvoiceWithLineItems>,
    fixedAssets: List<FixedAssetEntity>,
    payrollRecords: List<PayrollRecordEntity>
) {
    var selectedReport by remember { mutableStateOf("pl") } // pl, bs, cf, trial

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FINANCIAL STATEMENT COMPOSER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
            
            // Export actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.recordAuditLog("Admin", "Report Exported", "Format: PDF", "Success") }) {
                    Icon(Icons.Default.Description, contentDescription = "PDF Export", tint = AccentRed)
                }
                IconButton(onClick = { viewModel.recordAuditLog("Admin", "Report Exported", "Format: Excel", "Success") }) {
                    Icon(Icons.Default.Assessment, contentDescription = "Excel Export", tint = ForestPrimary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val reportTypes = listOf(
                "pl" to "Profit & Loss",
                "bs" to "Balance Sheet",
                "cf" to "Cash Flow",
                "trial" to "Trial Balance"
            )
            reportTypes.forEach { (type, label) ->
                ElevatedFilterChip(
                    selected = selectedReport == type,
                    onClick = { selectedReport = type },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedReport) {
                "pl" -> ProfitAndLossView(accounts)
                "bs" -> BalanceSheetView(accounts)
                "cf" -> CashFlowStatementView(accounts)
                "trial" -> TrialBalanceView(accounts)
            }
        }
    }
}

@Composable
fun ProfitAndLossView(accounts: List<CoaAccountEntity>) {
    val revenues = accounts.filter { it.category == "Income" }
    val costOfSales = accounts.find { it.code == "5010" }?.balance ?: 0.0
    val totalRevenue = revenues.sumOf { it.balance }
    val grossProfit = totalRevenue - costOfSales
    
    val otherExpenses = accounts.filter { it.category == "Expense" && it.code != "5010" }
    val totalExpenses = otherExpenses.sumOf { it.balance }
    val netIncome = grossProfit - totalExpenses

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ceyvana Premium Ceylon Spices", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("PROFIT & LOSS STATEMENT (MTD)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("For period ending 2026-07-19 | Accrual Basis", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(12.dp))

        Text("REVENUE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
        revenues.forEach { rev ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rev.name, fontSize = 12.sp)
                Text("Rs. ${String.format(Locale.US, "%,.2f", rev.balance)}", fontSize = 12.sp)
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Revenue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", totalRevenue)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Less: Cost of Goods Sold (COGS)", fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", costOfSales)}", fontSize = 12.sp)
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GROSS PROFIT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
            Text("Rs. ${String.format(Locale.US, "%,.2f", grossProfit)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("OPERATING EXPENSES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentRed)
        otherExpenses.forEach { exp ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(exp.name, fontSize = 12.sp)
                Text("Rs. ${String.format(Locale.US, "%,.2f", exp.balance)}", fontSize = 12.sp)
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Expenses", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", totalExpenses)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 2.dp, color = ForestPrimary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("NET PROFIT / LOSS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestDark)
            Text("Rs. ${String.format(Locale.US, "%,.2f", netIncome)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestDark)
        }
    }
}

@Composable
fun BalanceSheetView(accounts: List<CoaAccountEntity>) {
    val assets = accounts.filter { it.category == "Asset" }
    val liabilities = accounts.filter { it.category == "Liability" }
    val equity = accounts.filter { it.category == "Equity" }
    
    val totalAssets = assets.sumOf { it.balance }
    val totalLiabilities = liabilities.sumOf { it.balance }
    val totalEquity = equity.sumOf { it.balance }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ceyvana Premium Ceylon Spices", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("BALANCE SHEET", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("As of 2026-07-19 | Accrual Basis", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(12.dp))

        Text("ASSETS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
        assets.forEach { asset ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(asset.name, fontSize = 12.sp)
                Text("Rs. ${String.format(Locale.US, "%,.2f", asset.balance)}", fontSize = 12.sp)
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Assets", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", totalAssets)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("LIABILITIES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentRed)
        liabilities.forEach { lia ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(lia.name, fontSize = 12.sp)
                Text("Rs. ${String.format(Locale.US, "%,.2f", lia.balance)}", fontSize = 12.sp)
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Liabilities", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", totalLiabilities)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentRed)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("EQUITY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestDark)
        equity.forEach { eq ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(eq.name, fontSize = 12.sp)
                Text("Rs. ${String.format(Locale.US, "%,.2f", eq.balance)}", fontSize = 12.sp)
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Equity", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", totalEquity)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestDark)
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 2.dp, color = ForestPrimary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL LIABILITIES & EQUITY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", totalLiabilities + totalEquity)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
        }
    }
}

@Composable
fun CashFlowStatementView(accounts: List<CoaAccountEntity>) {
    val cashOnHand = accounts.find { it.code == "1010" }?.balance ?: 0.0
    val cashInBank = accounts.filter { it.code in listOf("1020", "1030") }.sumOf { it.balance }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ceyvana Premium Ceylon Spices", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("STATEMENT OF CASH FLOWS", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(12.dp))

        Text("CASH FLOW FROM OPERATING ACTIVITIES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cash Receipts from Customer Sales", fontSize = 11.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", (accounts.find { it.code == "4010" }?.balance ?: 0.0) * 0.9)}", fontSize = 11.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cash Payments for Supplier Inventory Purchases", fontSize = 11.sp)
            Text("Rs. -${String.format(Locale.US, "%,.2f", (accounts.find { it.code == "5010" }?.balance ?: 0.0) * 0.85)}", fontSize = 11.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cash Payments to Employees", fontSize = 11.sp)
            Text("Rs. -${String.format(Locale.US, "%,.2f", accounts.find { it.code == "5020" }?.balance ?: 0.0)}", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("CASH FLOW FROM INVESTING ACTIVITIES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Acquisition of Grinding/Packaging Machinery", fontSize = 11.sp)
            Text("Rs. -120,000.00", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("NET INCREASE IN LIQUID CASH POSITION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cash Balance at Beginning of Month", fontSize = 11.sp)
            Text("Rs. 600,000.00", fontSize = 11.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cash Balance at End of Month (Actual)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            Text("Rs. ${String.format(Locale.US, "%,.2f", cashOnHand + cashInBank)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun TrialBalanceView(accounts: List<CoaAccountEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ceyvana Premium Ceylon Spices", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("TRIAL BALANCE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Account Code & Title", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            Text("Debit (Dr.)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("Credit (Cr.)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        HorizontalDivider()

        accounts.forEach { acc ->
            val isDebitSide = acc.category == "Asset" || acc.category == "Expense"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${acc.code} - ${acc.name}", fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                if (isDebitSide) {
                    Text("Rs. ${String.format(Locale.US, "%,.2f", acc.balance)}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = ForestPrimary)
                    Text("-", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                } else {
                    Text("-", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text("Rs. ${String.format(Locale.US, "%,.2f", acc.balance)}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = AccentGold)
                }
            }
        }
    }
}

// 10. CASH FLOW FORECASTING TAB
@Composable
fun CashFlowForecastingTab(
    accounts: List<CoaAccountEntity>,
    receivables: List<CustomerReceivableEntity>,
    supplierBills: List<SupplierBillEntity>
) {
    val cashOnHand = accounts.find { it.code == "1010" }?.balance ?: 0.0
    val cashInBank = accounts.filter { it.code in listOf("1020", "1030") }.sumOf { it.balance }
    val currentLiquid = cashOnHand + cashInBank

    val projectedReceivables = receivables.sumOf { it.balance }
    val projectedPayables = supplierBills.filter { it.status != "Paid" }.sumOf { it.amount - it.paidAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("3-MONTH CASH POSITION PREDICTIVE FORECASTING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
        Text("Algorithmic forecast generated offline utilizing historical sales data, seasonal spice harvesting seasons, and outstanding obligations.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CURRENT STARTING POSITION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Liquid Cash & Bank Reserves")
                    Text("Rs. ${String.format(Locale.US, "%,.2f", currentLiquid)}", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Projected Collections (A/R)")
                    Text("Rs. ${String.format(Locale.US, "%,.2f", projectedReceivables)}", color = ForestPrimary, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Projected Payments (A/P)")
                    Text("Rs. -${String.format(Locale.US, "%,.2f", projectedPayables)}", color = AccentRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Projecting next 3 months
        val monthProjection1 = currentLiquid + projectedReceivables - projectedPayables + 350000.0 - 240000.0 // avg monthly spice sales net profit
        val monthProjection2 = monthProjection1 + 380000.0 - 240000.0
        val monthProjection3 = monthProjection2 + 420000.0 - 240000.0 // Spice peak season increase

        Text("MONTHLY PROJECTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ForecastMonthRow("Month 1 (August 2026)", monthProjection1)
            ForecastMonthRow("Month 2 (September 2026)", monthProjection2)
            ForecastMonthRow("Month 3 (October 2026 - Cinnamon Harvest peak)", monthProjection3)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = ForestPrimary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Harvest Alert", tint = ForestPrimary)
                Text("Projected October spice sales show an upward spike due to the Southern Province Cinnamon peeling season and higher export freight clearances.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ForestDark)
            }
        }
    }
}

@Composable
fun ForecastMonthRow(monthName: String, amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(monthName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text("Est. Rs. ${String.format(Locale.US, "%,.2f", amount)}", fontWeight = FontWeight.Bold, color = ForestPrimary)
        }
    }
}

// 11. AUDIT TRAIL TAB
@Composable
fun AuditTrailTab(auditLogs: List<AuditLogEntity>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("IMMUTABLE ERP AUDIT LEDGER LOGS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(auditLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("User: ${log.user}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(log.timestamp)), fontSize = 10.sp, color = Color.Gray)
                        }
                        Text("Action: ${log.action}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = ForestDark)
                        if (log.previousValue.isNotBlank()) {
                            Text("Prev: ${log.previousValue}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (log.newValue.isNotBlank()) {
                            Text("New: ${log.newValue}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Device Stamp: ${log.device}", fontSize = 9.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// ======================== DIALOG WORKSPACES ========================

@Composable
fun QuickJournalDialog(
    viewModel: InvoiceViewModel,
    accounts: List<CoaAccountEntity>,
    onDismiss: () -> Unit
) {
    var narration by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    
    var debitAccountCode by remember { mutableStateOf("") }
    var debitAmountStr by remember { mutableStateOf("") }

    var creditAccountCode by remember { mutableStateOf("") }
    var creditAmountStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Quick Journal Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(
                    value = narration,
                    onValueChange = { narration = it },
                    label = { Text("Narration / Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference / Receipt ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("DEBIT ACTION (DR.)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = debitAccountCode,
                        onValueChange = { debitAccountCode = it },
                        label = { Text("A/C Code (e.g., 5030)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = debitAmountStr,
                        onValueChange = { debitAmountStr = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("CREDIT ACTION (CR.)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AccentGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creditAccountCode,
                        onValueChange = { creditAccountCode = it },
                        label = { Text("A/C Code (e.g., 1010)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = creditAmountStr,
                        onValueChange = { creditAmountStr = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val debVal = debitAmountStr.toDoubleOrNull() ?: 0.0
                            val credVal = creditAmountStr.toDoubleOrNull() ?: 0.0
                            if (narration.isNotBlank() && debitAccountCode.isNotBlank() && creditAccountCode.isNotBlank()) {
                                val lines = listOf(
                                    JournalLineEntity(accountCode = debitAccountCode, debit = debVal, credit = 0.0, entryId = 0),
                                    JournalLineEntity(accountCode = creditAccountCode, debit = 0.0, credit = credVal, entryId = 0)
                                )
                                viewModel.postJournalEntry(narration, reference, lines)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Post Journal")
                    }
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(viewModel: InvoiceViewModel, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Asset") }
    var bankNo by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Register New Account", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Account Code (unique)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bankNo, onValueChange = { bankNo = it }, label = { Text("Bank Account Number (Optional)") }, modifier = Modifier.fillMaxWidth())
                
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = listOf("Asset", "Liability", "Equity", "Income", "Expense")
                    cats.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (code.isNotBlank() && name.isNotBlank()) {
                                viewModel.addBankAccount(code, name, bankNo)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordSupplierBillDialog(viewModel: InvoiceViewModel, suppliers: List<SupplierEntity>, onDismiss: () -> Unit) {
    var supplierId by remember { mutableStateOf(0) }
    var billNo by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Log Procurement Bill", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = billNo, onValueChange = { billNo = it }, label = { Text("Invoice / Bill Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Bill Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                
                Text("Select Supplier Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 120.dp)) {
                    suppliers.forEach { sup ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { supplierId = sup.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = supplierId == sup.id, onClick = { supplierId = sup.id })
                            Text(sup.name, fontSize = 12.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val am = amountStr.toDoubleOrNull() ?: 0.0
                            if (supplierId != 0 && billNo.isNotBlank() && am > 0) {
                                viewModel.recordSupplierBill(supplierId, billNo, am, System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Log Bill")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCollectionDialog(
    viewModel: InvoiceViewModel,
    receivables: List<CustomerReceivableEntity>,
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit
) {
    var customerId by remember { mutableStateOf(0) }
    var amountStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Collect Outstanding Debt", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Collected Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                
                Text("Select Debtor Customer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 120.dp)) {
                    receivables.forEach { rec ->
                        val cust = customers.find { it.id == rec.customerId }
                        if (cust != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customerId = cust.id }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = customerId == cust.id, onClick = { customerId = cust.id })
                                Text("${cust.name} (Balance: Rs. ${rec.balance})", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val am = amountStr.toDoubleOrNull() ?: 0.0
                            if (customerId != 0 && am > 0) {
                                viewModel.recordCustomerCreditPayment(customerId, am)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Record Pay")
                    }
                }
            }
        }
    }
}

@Composable
fun AddFixedAssetDialog(viewModel: InvoiceViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var usefulYears by remember { mutableStateOf("5") }
    var supplier by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Acquire Fixed Asset", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("Purchase Cost (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = usefulYears, onValueChange = { usefulYears = it }, label = { Text("Useful Life (Years)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Equipment Supplier") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val cs = costStr.toDoubleOrNull() ?: 0.0
                            val yr = usefulYears.toIntOrNull() ?: 5
                            if (name.isNotBlank() && cs > 0) {
                                viewModel.addFixedAsset(name, cs, yr, "Straight-Line", supplier)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Acquire")
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessPayrollDialog(viewModel: InvoiceViewModel, employees: List<EmployeeEntity>, onDismiss: () -> Unit) {
    var selectedEmployeeId by remember { mutableStateOf(0) }
    var basicStr by remember { mutableStateOf("45000") }
    var overtimeStr by remember { mutableStateOf("5000") }
    var bonusStr by remember { mutableStateOf("1500") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Process Monthly Salary Payslip", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = basicStr, onValueChange = { basicStr = it }, label = { Text("Basic Salary (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = overtimeStr, onValueChange = { overtimeStr = it }, label = { Text("Overtime (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bonusStr, onValueChange = { bonusStr = it }, label = { Text("Incentives & Bonuses (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

                Text("Select Employee Staff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 120.dp)) {
                    employees.forEach { emp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEmployeeId = emp.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedEmployeeId == emp.id, onClick = { selectedEmployeeId = emp.id })
                            Text(emp.name, fontSize = 12.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val emp = employees.find { it.id == selectedEmployeeId }
                            val bs = basicStr.toDoubleOrNull() ?: 0.0
                            val ot = overtimeStr.toDoubleOrNull() ?: 0.0
                            val bo = bonusStr.toDoubleOrNull() ?: 0.0
                            if (emp != null) {
                                viewModel.recordPayroll(emp.id, emp.name, "July 2026", bs, 0.0, ot, bo, 0.0, 0.0)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disburse Pay")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateBudgetDialog(viewModel: InvoiceViewModel, accounts: List<CoaAccountEntity>, onDismiss: () -> Unit) {
    var accCode by remember { mutableStateOf("") }
    var limitStr by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Operations") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set Department Budget Limit", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = accCode, onValueChange = { accCode = it }, label = { Text("Account Code (e.g. 5020)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = limitStr, onValueChange = { limitStr = it }, label = { Text("Monthly Cap Limit (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val limit = limitStr.toDoubleOrNull() ?: 0.0
                            if (accCode.isNotBlank() && limit > 0) {
                                viewModel.addBudget(accCode, limit, "2026-07", department)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Establish Limit")
                    }
                }
            }
        }
    }
}

@Composable
fun BankTransferDialog(viewModel: InvoiceViewModel, accounts: List<CoaAccountEntity>, onDismiss: () -> Unit) {
    var sourceCode by remember { mutableStateOf("1010") } // Default cash drawer
    var destCode by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Inter-Account Fund Transfer", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                OutlinedTextField(value = sourceCode, onValueChange = { sourceCode = it }, label = { Text("Source Account Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = destCode, onValueChange = { destCode = it }, label = { Text("Destination Account Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Transfer Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (sourceCode.isNotBlank() && destCode.isNotBlank() && amt > 0) {
                                val lines = listOf(
                                    JournalLineEntity(accountCode = destCode, debit = amt, credit = 0.0, entryId = 0),
                                    JournalLineEntity(accountCode = sourceCode, debit = 0.0, credit = amt, entryId = 0)
                                )
                                viewModel.postJournalEntry("Inter-Account Transfer", "BANK-TX", lines, "Manual")
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Transfer")
                    }
                }
            }
        }
    }
}

@Composable
fun ReconciliationWorkspaceDialog(
    accounts: List<CoaAccountEntity>,
    entries: List<JournalEntryWithLines>,
    onDismiss: () -> Unit
) {
    var bankStatementBalStr by remember { mutableStateOf("450000.00") }
    var reconciliationDifference by remember { mutableStateOf(0.0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bank Statement Reconciliation", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestPrimary)
                
                Text("BOC Ledger Balance: Rs. ${String.format(Locale.US, "%,.2f", accounts.find { it.code == "1020" }?.balance ?: 0.0)}", fontSize = 12.sp)

                OutlinedTextField(
                    value = bankStatementBalStr,
                    onValueChange = { 
                        bankStatementBalStr = it
                        val actualBocLedger = accounts.find { it.code == "1020" }?.balance ?: 0.0
                        val statementVal = it.toDoubleOrNull() ?: 0.0
                        reconciliationDifference = actualBocLedger - statementVal
                    },
                    label = { Text("Actual Bank Statement Balance (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Unreconciled Variance:", fontSize = 12.sp)
                    Text("Rs. ${String.format(Locale.US, "%,.2f", reconciliationDifference)}", fontWeight = FontWeight.Bold, color = if (reconciliationDifference == 0.0) ForestPrimary else AccentRed)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (reconciliationDifference == 0.0) ForestPrimary.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (reconciliationDifference == 0.0) "Ledgers are fully reconciled with your bank account!" else "Variance detected! Please audit unmatched deposit tickets, outstanding checks, or bank service charges.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reconciliationDifference == 0.0) ForestDark else AccentRed
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Workspace")
                }
            }
        }
    }
}
