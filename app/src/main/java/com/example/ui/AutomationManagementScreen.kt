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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import android.widget.Toast

import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationManagementScreen(
    viewModel: InvoiceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // States from view model
    val rules by viewModel.automationRules.collectAsState()
    val scheduledJobs by viewModel.scheduledJobs.collectAsState()
    val approvals by viewModel.workflowApprovals.collectAsState()
    val logs by viewModel.automationLogs.collectAsState()
    val config by viewModel.invoiceNumberConfig.collectAsState()
    val stockAlerts by viewModel.stockAlerts.collectAsState()
    val suggestedPOs by viewModel.suggestedPOs.collectAsState()
    val backupHistory by viewModel.backupHistory.collectAsState()
    val aiRecommendation by viewModel.aiRecommendation.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val stats by viewModel.automationStats.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val productsList by viewModel.productsList.collectAsState()
    
    // New AI Module state flows
    val aiLearningEnabled by viewModel.aiLearningEnabled.collectAsState()
    val aiChatMessages by viewModel.aiChatMessages.collectAsState()
    val fraudAlerts by viewModel.fraudAlerts.collectAsState()
    val generatedMarketingContent by viewModel.generatedMarketingContent.collectAsState()
    val extractedOcrFields by viewModel.extractedOcrFields.collectAsState()
    val voicePosStatus by viewModel.voicePosStatus.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Rule Engine", "Auto-Number", "Approvals", "Backups", "AI Insights")
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Trigger toast alerts for VM status messages
    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(statusMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Business Automation Module",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        Text(
                            text = "Ceyvana Ceylon Spices Centralized Workflows",
                            fontSize = 12.sp,
                            color = ForestSage
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to ERP Home",
                            tint = AccentGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestDark,
                    titleContentColor = AccentGold
                ),
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = AccentGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.syncOfflineData() }) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync Cloud ERP",
                                tint = AccentGold
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ForestDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Horizontal scrollable tabs matching the premium design guidelines
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = ForestMedium,
                contentColor = AccentGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentGold
                    )
                },
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) AccentGold else IvoryDark
                            )
                        }
                    )
                }
            }

            // Central view switcher based on active tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> AutomationDashboardTab(stats = stats, logs = logs, scheduledJobs = scheduledJobs)
                    1 -> RuleEngineTab(rules = rules, onToggleRule = { viewModel.toggleAutomationRule(it) }, onTriggerRule = { viewModel.triggerRuleInstantly(it) }, onAddNewRule = { name, trig, act, cat -> viewModel.addCustomAutomationRule(name, trig, act, cat) })
                    2 -> AutoNumberingAndInventoryTab(config = config, stockAlerts = stockAlerts, onUpdateConfig = { viewModel.updateInvoiceNumberConfig(it) }, onGenerate = { viewModel.generateNextInvoiceNumber(it) })
                    3 -> ApprovalsAndPurchaseOrdersTab(approvals = approvals, suggestedPOs = suggestedPOs, onResolveApproval = { id, status -> viewModel.resolveWorkflowApproval(id, status, "System Director") })
                    4 -> BackupsAndReportsTab(backupHistory = backupHistory, onRunBackup = { type, dest -> viewModel.runManualBackup(type, dest) }, onRunReport = { type, format, channels, recp -> viewModel.runAutomaticReport(type, format, channels, recp) })
                    5 -> AiEnterpriseSuiteTab(
                        viewModel = viewModel,
                        products = productsList,
                        aiRecommendation = aiRecommendation,
                        isAiLoading = isAiLoading,
                        aiLearningEnabled = aiLearningEnabled,
                        aiChatMessages = aiChatMessages,
                        fraudAlerts = fraudAlerts,
                        generatedMarketingContent = generatedMarketingContent,
                        extractedOcrFields = extractedOcrFields,
                        voicePosStatus = voicePosStatus
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 0: DASHBOARD & METRICS
// ==========================================
@Composable
fun AutomationDashboardTab(
    stats: Map<String, String>,
    logs: List<AutomationLog>,
    scheduledJobs: List<ScheduledJob>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Grid of statistics widgets
        item {
            Text(
                text = "Live Operational Performance Metrics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatWidgetCard(title = "Active Rules", value = stats["activeRules"] ?: "0", icon = Icons.Default.Rule, color = AccentGold, modifier = Modifier.weight(1f))
                    StatWidgetCard(title = "Scheduled Cron", value = stats["scheduledJobs"] ?: "0", icon = Icons.Default.Schedule, color = ForestLight, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatWidgetCard(title = "Success Runs", value = stats["successExecutions"] ?: "0", icon = Icons.Default.CheckCircle, color = AccentEmerald, modifier = Modifier.weight(1f))
                    StatWidgetCard(title = "Failed Runs", value = stats["failedExecutions"] ?: "0", icon = Icons.Default.Error, color = Color(0xFFEF5350), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatWidgetCard(title = "Notifications Sent", value = stats["notificationsSent"] ?: "0", icon = Icons.Default.Sms, color = Color(0xFF29B6F6), modifier = Modifier.weight(1f))
                    StatWidgetCard(title = "Reports Sent", value = stats["reportsGenerated"] ?: "0", icon = Icons.Default.Assignment, color = Color(0xFFAB47BC), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatWidgetCard(title = "Backups Done", value = stats["backupsCompleted"] ?: "0", icon = Icons.Default.Backup, color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
                    StatWidgetCard(title = "Stock Alerts", value = stats["stockAlertsCount"] ?: "0", icon = Icons.Default.Warning, color = Color(0xFFFF7043), modifier = Modifier.weight(1f))
                }
            }
        }

        // Active Cron jobs preview
        item {
            Text(
                text = "Cron Scheduler Registry",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scheduledJobs.forEach { job ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ForestMedium),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ForestPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (job.type) {
                                        "Backup" -> Icons.Default.Backup
                                        "Report" -> Icons.Default.Print
                                        "Stock Check" -> Icons.Default.FactCheck
                                        else -> Icons.Default.Campaign
                                    },
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextOnDarkIvory)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Code, null, tint = ForestSage, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cron: ${job.cronExpression}", fontSize = 11.sp, color = ForestSage)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Surface(
                                color = AccentGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = job.status,
                                    color = AccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-time Event Log output
        item {
            Text(
                text = "Central Audit Log & Rule Execution Trail",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(logs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ForestSage.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.ruleName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        
                        val isSuccess = log.status == "Success"
                        Surface(
                            color = (if (isSuccess) AccentEmerald else Color(0xFFEF5350)).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.status,
                                color = if (isSuccess) AccentEmerald else Color(0xFFEF5350),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.result,
                        fontSize = 12.sp,
                        color = TextOnDarkIvory
                    )
                    
                    if (log.errorDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Error: ${log.errorDetails}",
                            fontSize = 11.sp,
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trigger: ${log.triggerType}",
                            fontSize = 10.sp,
                            color = ForestSage
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm:ss (MMM dd)", Locale.US).format(Date(log.executionTime)),
                            fontSize = 10.sp,
                            color = ForestSage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatWidgetCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMedium),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ForestSage.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = ForestSage
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ForestDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// TAB 1: CUSTOM RULE ENGINE
// ==========================================
@Composable
fun RuleEngineTab(
    rules: List<AutomationRule>,
    onToggleRule: (String) -> Unit,
    onTriggerRule: (String) -> Unit,
    onAddNewRule: (String, String, String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    var newRuleName by remember { mutableStateOf("") }
    var newTrigger by remember { mutableStateOf("") }
    var newAction by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Inventory") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Workflow Automation Rules",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Text(
                        text = "Customize triggers and responsive system reactions",
                        fontSize = 11.sp,
                        color = ForestSage
                    )
                }
                
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_rule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(rules) { rule ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (rule.isActive) ForestMedium else ForestMedium.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (rule.isActive) AccentGold.copy(alpha = 0.3f) else ForestSage.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (rule.category) {
                                    "Inventory" -> Icons.Default.Inventory
                                    "Finance" -> Icons.Default.AccountBalanceWallet
                                    "Logistics" -> Icons.Default.LocalShipping
                                    "Management" -> Icons.Default.Insights
                                    else -> Icons.Default.ContactPhone
                                },
                                contentDescription = null,
                                tint = if (rule.isActive) AccentGold else ForestSage,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rule.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rule.isActive) TextOnDarkIvory else ForestSage
                            )
                        }
                        
                        Switch(
                            checked = rule.isActive,
                            onCheckedChange = { onToggleRule(rule.id) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ForestDark,
                                checkedTrackColor = AccentGold,
                                uncheckedThumbColor = ForestSage,
                                uncheckedTrackColor = ForestDark
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ForestDark.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row {
                            Text("IF: ", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(rule.trigger, color = TextOnDarkIvory, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text("THEN: ", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(rule.action, color = TextOnDarkIvory, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (rule.lastTriggered == 0L) "Never triggered" 
                            else "Last run: ${SimpleDateFormat("HH:mm (MMM dd)", Locale.US).format(Date(rule.lastTriggered))}",
                            fontSize = 10.sp,
                            color = ForestSage
                        )
                        
                        Button(
                            onClick = { onTriggerRule(rule.id) },
                            enabled = rule.isActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestPrimary,
                                contentColor = AccentGold,
                                disabledContainerColor = ForestPrimary.copy(alpha = 0.2f),
                                disabledContentColor = ForestSage
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trigger Now", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text("Register Custom Automation Rule", color = AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newRuleName,
                        onValueChange = { newRuleName = it },
                        label = { Text("Rule Name", color = IvoryDark) },
                        textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestPrimary,
                            unfocusedBorderColor = IvoryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Trigger Event (IF)", color = IvoryDark) },
                        textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestPrimary,
                            unfocusedBorderColor = IvoryDark
                        ),
                        placeholder = { Text("e.g., daily sales exceed Target", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAction,
                        onValueChange = { newAction = it },
                        label = { Text("Responsive Action (THEN)", color = IvoryDark) },
                        textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestPrimary,
                            unfocusedBorderColor = IvoryDark
                        ),
                        placeholder = { Text("e.g., email Owner and send WhatsApp", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text("Category Scope", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Inventory", "Finance", "Logistics", "CRM").forEach { cat ->
                                val isSel = newCategory == cat
                                FilterChip(
                                    selected = isSel,
                                    onClick = { newCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ForestPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRuleName.isNotEmpty() && newTrigger.isNotEmpty() && newAction.isNotEmpty()) {
                            onAddNewRule(newRuleName, newTrigger, newAction, newCategory)
                            showCreateDialog = false
                            newRuleName = ""
                            newTrigger = ""
                            newAction = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                ) {
                    Text("Register Rule", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================
// TAB 2: AUTO-NUMBERING & INVENTORY ALERTS
// ==========================================
@Composable
fun AutoNumberingAndInventoryTab(
    config: InvoiceNumberConfig,
    stockAlerts: List<StockAlertItem>,
    onUpdateConfig: (InvoiceNumberConfig) -> Unit,
    onGenerate: (String) -> String
) {
    var prefix by remember { mutableStateOf(config.prefix) }
    var useYear by remember { mutableStateOf(config.useYear) }
    var includeBranch by remember { mutableStateOf(config.includeBranch) }
    var branchPrefix by remember { mutableStateOf(config.branchPrefix) }
    var resetCycle by remember { mutableStateOf(config.resetCycle) }
    var numberingType by remember { mutableStateOf(config.numberingType) }
    
    var generatedNumberPreview by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Automatic numbering configuration panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Automatic Invoice Numbering Configurator",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = {
                            prefix = it
                            onUpdateConfig(config.copy(prefix = it, useYear = useYear, includeBranch = includeBranch, branchPrefix = branchPrefix, resetCycle = resetCycle, numberingType = numberingType))
                        },
                        label = { Text("Global Prefix Prefix", color = IvoryDark) },
                        textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IvoryTrue,
                            unfocusedContainerColor = IvoryTrue,
                            focusedBorderColor = ForestPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Year Code ([YYYY])", color = TextOnDarkIvory, fontSize = 13.sp)
                        Checkbox(
                            checked = useYear,
                            onCheckedChange = {
                                useYear = it
                                onUpdateConfig(config.copy(prefix = prefix, useYear = it, includeBranch = includeBranch, branchPrefix = branchPrefix, resetCycle = resetCycle, numberingType = numberingType))
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AccentGold)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Branch ID Prefix", color = TextOnDarkIvory, fontSize = 13.sp)
                        Checkbox(
                            checked = includeBranch,
                            onCheckedChange = {
                                includeBranch = it
                                onUpdateConfig(config.copy(prefix = prefix, useYear = useYear, includeBranch = it, branchPrefix = branchPrefix, resetCycle = resetCycle, numberingType = numberingType))
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AccentGold)
                        )
                    }

                    if (includeBranch) {
                        OutlinedTextField(
                            value = branchPrefix,
                            onValueChange = {
                                branchPrefix = it
                                onUpdateConfig(config.copy(prefix = prefix, useYear = useYear, includeBranch = true, branchPrefix = it, resetCycle = resetCycle, numberingType = numberingType))
                            },
                            label = { Text("Branch Code (e.g. COL-, GAL-)", color = IvoryDark) },
                            textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IvoryTrue,
                                unfocusedContainerColor = IvoryTrue,
                                focusedBorderColor = ForestPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Column {
                        Text("Reset Sequence Counter Cycle", fontSize = 12.sp, color = ForestSage)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Daily", "Monthly", "Yearly").forEach { cycle ->
                                FilterChip(
                                    selected = resetCycle == cycle,
                                    onClick = {
                                        resetCycle = cycle
                                        onUpdateConfig(config.copy(prefix = prefix, useYear = useYear, includeBranch = includeBranch, branchPrefix = branchPrefix, resetCycle = cycle, numberingType = numberingType))
                                    },
                                    label = { Text(cycle, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentGold,
                                        selectedLabelColor = ForestDark
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ForestSage.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Interactive Prefix Tester", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("Retail", "Wholesale", "Export").forEach { type ->
                            Button(
                                onClick = {
                                    numberingType = type
                                    onUpdateConfig(config.copy(prefix = prefix, useYear = useYear, includeBranch = includeBranch, branchPrefix = branchPrefix, resetCycle = resetCycle, numberingType = type))
                                    generatedNumberPreview = onGenerate(type)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary, contentColor = AccentGold),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Gen $type", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (generatedNumberPreview.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ForestDark, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Generated Sequential Code:", fontSize = 10.sp, color = ForestSage)
                                Text(
                                    generatedNumberPreview,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-time Low Stock Alert list
        item {
            Text(
                "Intelligent Stock Level & Expiry Alerts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (stockAlerts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestMedium),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "✅ Spices inventory levels healthy. No low stock alert triggered.",
                            fontSize = 13.sp,
                            color = AccentEmerald,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(stockAlerts) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestMedium),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when (alert.alertLevel) {
                            "Out of Stock" -> Color(0xFFEF5350)
                            "Critical Stock" -> Color(0xFFFF7043)
                            else -> Color(0xFFFFB74D)
                        }.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (alert.alertLevel) {
                                        "Out of Stock" -> Color(0xFFEF5350)
                                        "Critical Stock" -> Color(0xFFFF7043)
                                        else -> Color(0xFFFFB74D)
                                    }.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = when (alert.alertLevel) {
                                    "Out of Stock" -> Color(0xFFEF5350)
                                    "Critical Stock" -> Color(0xFFFF7043)
                                    else -> Color(0xFFFFB74D)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.productName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextOnDarkIvory)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Stock: ${alert.currentStock}g", fontSize = 11.sp, color = ForestSage)
                                Text("Min Reorder: ${alert.reorderLevel}g", fontSize = 11.sp, color = ForestSage)
                            }
                            if (alert.expiryDate.isNotEmpty()) {
                                Text("Expiry Date: ${alert.expiryDate}", fontSize = 10.sp, color = Color(0xFFEF5350))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = when (alert.alertLevel) {
                                "Out of Stock" -> Color(0xFFEF5350)
                                "Critical Stock" -> Color(0xFFFF7043)
                                else -> Color(0xFFFFB74D)
                            }.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = alert.alertLevel,
                                color = when (alert.alertLevel) {
                                    "Out of Stock" -> Color(0xFFEF5350)
                                    "Critical Stock" -> Color(0xFFFF7043)
                                    else -> Color(0xFFFFB74D)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: WORKFLOW APPROVALS & AUTO-PO SUGGESTIONS
// ==========================================
@Composable
fun ApprovalsAndPurchaseOrdersTab(
    approvals: List<WorkflowApproval>,
    suggestedPOs: List<SuggestedPurchaseOrder>,
    onResolveApproval: (String, String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Pending approval queue
        item {
            Text(
                "Corporate Workflow Approval Queue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
            Text(
                "Authorized oversight signatures for high-risk corporate ledger transactions",
                fontSize = 11.sp,
                color = ForestSage,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(approvals) { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    when (app.status) {
                        "Pending" -> AccentGold.copy(alpha = 0.3f)
                        "Approved" -> AccentEmerald.copy(alpha = 0.3f)
                        else -> Color(0xFFEF5350).copy(alpha = 0.3f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (app.type) {
                                    "Purchase Order" -> Icons.Default.ReceiptLong
                                    "Refund" -> Icons.Default.SettingsBackupRestore
                                    "Expense" -> Icons.Default.MoneyOff
                                    "Large Discount" -> Icons.Default.Discount
                                    else -> Icons.Default.FactCheck
                                },
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.type,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextOnDarkIvory
                            )
                        }
                        
                        Surface(
                            color = when (app.status) {
                                "Pending" -> AccentGold
                                "Approved" -> AccentEmerald
                                else -> Color(0xFFEF5350)
                            }.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = app.status,
                                color = when (app.status) {
                                    "Pending" -> AccentGold
                                    "Approved" -> AccentEmerald
                                    else -> Color(0xFFEF5350)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(app.description, fontSize = 13.sp, color = TextOnDarkIvory)
                    
                    if (app.amount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Value: Rs. ${String.format(Locale.US, "%,.2f", app.amount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reason: ${app.justification}", fontSize = 11.sp, color = ForestSage)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ForestSage.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("By: ${app.requestedBy}", fontSize = 10.sp, color = ForestSage)
                            Text("Requires: ${app.approvalLevelRequired}", fontSize = 10.sp, color = ForestSage, fontWeight = FontWeight.Bold)
                        }

                        if (app.status == "Pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onResolveApproval(app.id, "Rejected") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.2f), contentColor = Color(0xFFEF5350)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onResolveApproval(app.id, "Approved") },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald.copy(alpha = 0.2f), contentColor = AccentEmerald),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Resolved by ${app.approvedBy ?: "Director"}",
                                fontSize = 10.sp,
                                color = ForestSage
                            )
                        }
                    }
                }
            }
        }

        // Automatic reorder purchase recommendations
        item {
            Text(
                "Automatic Inventory Replenishment (Auto-PO Drafts)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "Suggested purchase orders generated automatically by inventory reorder thresholds",
                fontSize = 11.sp,
                color = ForestSage,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(suggestedPOs) { po ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ForestSage.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Draft PO ID: ${po.id}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        
                        Surface(
                            color = ForestPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Auto Drafted",
                                color = AccentGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    po.productNames.forEachIndexed { idx, name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = 13.sp, color = TextOnDarkIvory, modifier = Modifier.weight(1f))
                            Text("Qty: ${po.quantities[idx]}kg", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextOnDarkIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ForestSage.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Preferred Supplier: ${po.preferredSupplier}", fontSize = 11.sp, color = ForestSage)
                            Text("Estimated Cost: Rs. ${String.format(Locale.US, "%,.2f", po.estimatedCost)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                        
                        Button(
                            onClick = { /* Simulated action: post actual PO to backend system */ },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Transmit PO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: BACKUPS & SYSTEM REPORT SCHEDULER
// ==========================================
@Composable
fun BackupsAndReportsTab(
    backupHistory: List<String>,
    onRunBackup: (String, String) -> Unit,
    onRunReport: (String, String, List<String>, String) -> Unit
) {
    var backupType by remember { mutableStateOf("Database Schema & Room Entities") }
    var backupDest by remember { mutableStateOf("Google Drive Cloud Storage") }
    
    var reportType by remember { mutableStateOf("Daily Sales Summary Report") }
    var reportFormat by remember { mutableStateOf("PDF Document Format") }
    var sendWhatsApp by remember { mutableStateOf(true) }
    var sendEmail by remember { mutableStateOf(true) }
    var recipientAddress by remember { mutableStateOf("ceylonstonex@gmail.com, ceyvana-directors@google.com") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Backup scheduling controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Automatic Secure Backup Engine",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Scope of Backup Data", fontSize = 12.sp, color = ForestSage)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("Database Schema & Room Entities", "Product Catalogs & High-res Images", "Sales Ledger & Historical Invoices").forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { backupType = type }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = backupType == type,
                                onClick = { backupType = type },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(type, fontSize = 12.sp, color = TextOnDarkIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Target Cloud Destination", fontSize = 12.sp, color = ForestSage)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("Google Drive Cloud Storage", "Microsoft OneDrive Integration", "Secure Office Local NAS Server").forEach { dest ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { backupDest = dest }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = backupDest == dest,
                                onClick = { backupDest = dest },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dest, fontSize = 12.sp, color = TextOnDarkIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onRunBackup(backupType, backupDest) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Initiate Secure AES-256 Backup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Backup History
        item {
            Text("Encrypted Backup Archives History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                backupHistory.forEach { history ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ForestMedium.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ForestSage.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(history, fontSize = 11.sp, color = TextOnDarkIvory, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Automatic report scheduler
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Periodic Document Compiler & Broadcaster",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Target Report", fontSize = 12.sp, color = ForestSage)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("Daily Sales Summary Report", "Profit & Loss Statement (Trial Balance)", "Low Stock & Reorder Alert Log").forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportType = type }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = reportType == type,
                                onClick = { reportType = type },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(type, fontSize = 12.sp, color = TextOnDarkIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Distribution Format", fontSize = 12.sp, color = ForestSage)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("PDF Document Format", "Microsoft Excel Spreadsheet", "CSV Raw Data Archive").forEach { format ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportFormat = format }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = reportFormat == format,
                                onClick = { reportFormat = format },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(format, fontSize = 12.sp, color = TextOnDarkIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Broadcasting Channels", fontSize = 12.sp, color = ForestSage)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send via WhatsApp Business API", fontSize = 12.sp, color = TextOnDarkIvory)
                        Checkbox(checked = sendWhatsApp, onCheckedChange = { sendWhatsApp = it }, colors = CheckboxDefaults.colors(checkedColor = AccentGold))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send via Corporate SMTP Email", fontSize = 12.sp, color = TextOnDarkIvory)
                        Checkbox(checked = sendEmail, onCheckedChange = { sendEmail = it }, colors = CheckboxDefaults.colors(checkedColor = AccentGold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recipientAddress,
                        onValueChange = { recipientAddress = it },
                        label = { Text("Recipients List (Comma-separated)", color = IvoryDark) },
                        textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IvoryTrue,
                            unfocusedContainerColor = IvoryTrue,
                            focusedBorderColor = ForestPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val channels = mutableListOf<String>()
                            if (sendWhatsApp) channels.add("WhatsApp API")
                            if (sendEmail) channels.add("SMTP Email")
                            onRunReport(reportType, reportFormat, channels, recipientAddress)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary, contentColor = AccentGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcast Scheduled Report Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 5: ENTERPRISE AI SUITE
// ==========================================
@Composable
fun AiEnterpriseSuiteTab(
    viewModel: InvoiceViewModel,
    products: List<ProductEntity>,
    aiRecommendation: String,
    isAiLoading: Boolean,
    aiLearningEnabled: Boolean,
    aiChatMessages: List<AiChatMessage>,
    fraudAlerts: List<FraudAlert>,
    generatedMarketingContent: String,
    extractedOcrFields: Map<String, String>?,
    voicePosStatus: String
) {
    var activeSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf(
        "AI Chat Assistant",
        "AI Dashboards",
        "AI Marketing",
        "Voice POS",
        "OCR Docs",
        "Risk Monitor"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal pill row of sub-tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTabs.forEachIndexed { idx, label ->
                val selected = activeSubTab == idx
                AssistChip(
                    onClick = { activeSubTab = idx },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) AccentGold else ForestMedium,
                        labelColor = if (selected) ForestDark else TextOnDarkIvory
                    ),
                    border = BorderStroke(1.dp, if (selected) AccentGold else IvoryDark.copy(alpha = 0.2f))
                )
            }
        }

        // Sub-view rendering
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeSubTab) {
                0 -> AiChatSubTab(
                    messages = aiChatMessages,
                    isLoading = isAiLoading,
                    learningEnabled = aiLearningEnabled,
                    onSubmitQuery = { viewModel.submitAiQuery(it) },
                    onClearHistory = { viewModel.clearAiChat() },
                    onToggleLearning = { viewModel.setAiLearningEnabled(it) }
                )
                1 -> AiDashboardsSubTab(
                    products = products,
                    aiRecommendation = aiRecommendation,
                    isLoading = isAiLoading,
                    onRunForecast = { viewModel.runAiForecastAndStockOptimization(products) }
                )
                2 -> AiMarketingSubTab(
                    products = products,
                    generatedContent = generatedMarketingContent,
                    isLoading = isAiLoading,
                    onGenerate = { prod, chan, lang -> viewModel.generateSpicesMarketingContent(prod, chan, lang) }
                )
                3 -> AiVoicePosSubTab(
                    voiceStatus = voicePosStatus,
                    onVoiceCommand = { viewModel.processVoicePosCommand(it) },
                    onClearStatus = { viewModel.clearVoicePosStatus() }
                )
                4 -> AiOcrSubTab(
                    extractedFields = extractedOcrFields,
                    isLoading = isAiLoading,
                    onScanDoc = { viewModel.simulateOcrDocumentProcessing(it) },
                    onClear = { viewModel.clearOcrFields() },
                    onSave = { viewModel.saveOcrInvoiceToDatabase() }
                )
                5 -> AiRiskSubTab(
                    alerts = fraudAlerts,
                    onDismiss = { viewModel.dismissFraudAlert(it) }
                )
            }
        }
    }
}

// ------------------------------------------
// SUB-TAB 0: CHAT & NATURAL LANGUAGE Q&A
// ------------------------------------------
@Composable
fun AiChatSubTab(
    messages: List<AiChatMessage>,
    isLoading: Boolean,
    learningEnabled: Boolean,
    onSubmitQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    onToggleLearning: (Boolean) -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickQueries = listOf(
        "Show today's profit.",
        "How many invoices are unpaid?",
        "Which products generated highest revenue?",
        "Which customers have overdue payments?",
        "Show low-stock items.",
        "How much cash is available today?",
        "Compare this month's sales with last month.",
        "Which products expire within 30 days?",
        "Which supplier offers the best purchase price?"
    )

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Area
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ForestDark, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            items(messages) { msg ->
                val isAi = msg.sender == "ai"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAi) ForestMedium else ForestPrimary
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isAi) 4.dp else 16.dp,
                            bottomEnd = if (isAi) 16.dp else 4.dp
                        ),
                        border = BorderStroke(1.dp, if (isAi) AccentGold.copy(alpha = 0.15f) else Color.Transparent),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAi) Icons.Default.AutoAwesome else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isAi) AccentGold else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAi) "Ceyvana AI Assistant" else "System Administrator",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAi) AccentGold else Color.White
                                    )
                                }
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(msg.timestamp)),
                                    fontSize = 9.sp,
                                    color = IvoryDark
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Text Output
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = TextOnDarkIvory,
                                lineHeight = 18.sp
                            )

                            // Optional Table Output
                            if (msg.tableHeaders != null && msg.tableRows != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ForestDark, RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    // Headers
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        msg.tableHeaders.forEach { header ->
                                            Text(
                                                header,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentGold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Divider(color = ForestMedium, thickness = 1.dp)
                                    // Rows
                                    msg.tableRows.forEach { row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            row.forEach { cell ->
                                                Text(
                                                    cell,
                                                    fontSize = 10.sp,
                                                    color = TextOnDarkIvory,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Optional Chart Output
                            if (msg.chartData != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = msg.chartTitle ?: "AI Data Visualizer",
                                    fontSize = 11.sp,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                SimpleBarChart(data = msg.chartData)
                            }

                            // Optional Explainable AI Panel
                            if (isAi && msg.confidenceScore > 0.1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = ForestDark.copy(alpha = 0.5f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "🔍 Explainable AI Diagnostics",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                                Text("Confidence Score: ${String.format(Locale.US, "%.1f%%", msg.confidenceScore * 100)}", fontSize = 9.sp, color = ForestSage)
                                Text("Data Sources: ${msg.dataSources}", fontSize = 9.sp, color = ForestSage)
                                Text("Reasoning: ${msg.reasoning}", fontSize = 9.sp, color = ForestSage)
                                Text("Estimated Impact: ${msg.businessImpact}", fontSize = 9.sp, color = ForestSage)
                            }

                            // Recommended Action Button
                            if (msg.actionLabel != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onSubmitQuery(msg.actionLabel) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(msg.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini Core compiling spices ledger diagnostics...", fontSize = 11.sp, color = ForestSage)
                    }
                }
            }
        }

        // Quick suggestions panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text("Suggested NL Business Queries:", fontSize = 10.sp, color = ForestSage, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickQueries.forEach { label ->
                    InputChip(
                        selected = false,
                        onClick = {
                            queryText = label
                            onSubmitQuery(label)
                            queryText = ""
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = ForestMedium,
                            labelColor = TextOnDarkIvory
                        ),
                        border = BorderStroke(1.dp, IvoryDark.copy(alpha = 0.2f))
                    )
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .size(44.dp)
                    .background(ForestMedium, CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Red.copy(alpha = 0.8f))
            }

            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = { Text("Ask Ceyvana AI about profit, stock, sales...", color = IvoryDark.copy(alpha = 0.5f), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextOnLightForest,
                    unfocusedTextColor = TextOnLightForest,
                    focusedContainerColor = IvoryTrue,
                    unfocusedContainerColor = IvoryTrue,
                    focusedBorderColor = ForestPrimary
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (queryText.isNotBlank()) {
                                onSubmitQuery(queryText)
                                queryText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = ForestPrimary)
                    }
                }
            )

            // Settings Learning Toggle
            IconButton(
                onClick = { onToggleLearning(!learningEnabled) },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (learningEnabled) ForestPrimary else ForestMedium, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Learning Engine Toggle",
                    tint = if (learningEnabled) AccentGold else IvoryDark
                )
            }
        }
    }
}

// ------------------------------------------
// SUB-TAB 1: PREDICTIVE ANALYTICS DASHBOARDS
// ------------------------------------------
@Composable
fun AiDashboardsSubTab(
    products: List<ProductEntity>,
    aiRecommendation: String,
    isLoading: Boolean,
    onRunForecast: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Business Metric Health Gauges Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Predictive Corporate Health Indexes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BusinessHealthGauge(score = 92, label = "Business Health")
                        BusinessHealthGauge(score = 78, label = "Inventory Health")
                        BusinessHealthGauge(score = 85, label = "Cash Flow Health")
                    }
                }
            }
        }

        // 7-Day Forecast Line Chart
        item {
            ForecastTrendChart()
        }

        // High precision Forecast execution panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "TensorFlow & Gemini Demand Optimization Engine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Analyzes sales velocity cycles, crop shelf-life expiry dates, and weather patterns in Matale and Kandy.",
                        fontSize = 11.sp,
                        color = ForestSage,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = AccentGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Compiling massive ledger forecasts...", fontSize = 11.sp, color = ForestSage)
                    } else {
                        Button(
                            onClick = onRunForecast,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compute Predictive Demand Forecast", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Display results of compiled forecasts
        if (aiRecommendation.isNotEmpty() && !isLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestMedium.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, tint = AccentGold, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Demand Forecast & Safety Stock Optimization Results",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiRecommendation,
                            fontSize = 12.sp,
                            color = TextOnDarkIvory,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// SUB-TAB 2: AI MULTILINGUAL MARKETING COPYWRITER
// ------------------------------------------
@Composable
fun AiMarketingSubTab(
    products: List<ProductEntity>,
    generatedContent: String,
    isLoading: Boolean,
    onGenerate: (String, String, String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()?.name ?: "Organic Cinnamon Premium") }
    var selectedChannel by remember { mutableStateOf("Facebook & Instagram") }
    var selectedLang by remember { mutableStateOf("English") }

    val context = LocalContext.current
    val channels = listOf("Facebook & Instagram", "TikTok & Threads", "WhatsApp Business", "LinkedIn Professional", "SEO Product Blog")
    val languages = listOf("English", "Sinhala", "Tamil")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("AI Social Media Content Architect", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Draft high-conversion multi-language captions, hashtags, and launched blogs instantly using Gemini.", fontSize = 11.sp, color = ForestSage)
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    // Product Selector Dropdown (Simplified list picker)
                    Text("Target Spice Product", fontSize = 11.sp, color = ForestSage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val names = products.map { it.name }.ifEmpty { listOf("Organic Cinnamon", "Kandy Green Cardamom", "Kandy Cloves") }
                        names.take(5).forEach { name ->
                            val isSel = selectedProduct == name
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedProduct = name },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = ForestDark,
                                    containerColor = ForestDark,
                                    labelColor = TextOnDarkIvory
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Distribution Channel", fontSize = 11.sp, color = ForestSage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        channels.forEach { channel ->
                            val isSel = selectedChannel == channel
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedChannel = channel },
                                label = { Text(channel, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = ForestDark,
                                    containerColor = ForestDark,
                                    labelColor = TextOnDarkIvory
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Target Copy Language", fontSize = 11.sp, color = ForestSage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSel = selectedLang == lang
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedLang = lang },
                                label = { Text(lang, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = ForestDark,
                                    containerColor = ForestDark,
                                    labelColor = TextOnDarkIvory
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onGenerate(selectedProduct, selectedChannel, selectedLang) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = ForestDark, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Marketing Content", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (generatedContent.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestMedium.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, tint = AccentGold, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generated Marketing Asset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            }

                            // Copy button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Ceyvana Spices Copy", generatedContent)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied content copy to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Content", tint = AccentGold, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = generatedContent,
                            fontSize = 12.sp,
                            color = TextOnDarkIvory,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// SUB-TAB 3: VOICE-CONTROLLED POS SIMULATOR
// ------------------------------------------
@Composable
fun AiVoicePosSubTab(
    voiceStatus: String,
    onVoiceCommand: (String) -> Unit,
    onClearStatus: () -> Unit
) {
    var spokenText by remember { mutableStateOf("") }
    val sampleVoiceCommands = listOf(
        "Add two packs of turmeric powder.",
        "Apply ten percent discount.",
        "Search customer John.",
        "Generate invoice.",
        "Print receipt.",
        "Show today's sales."
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = ForestMedium),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ForestDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Activation Hub",
                    tint = AccentGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text("Voice-Controlled POS Operations", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGold)
            Text(
                "Allows checkout clerks and cashier staff to dictate sales actions in English, Sinhala, or Tamil. Critical processes (refunds/discounts) enforce confirmation gates.",
                fontSize = 11.sp,
                color = ForestSage,
                textAlign = TextAlign.Center
            )

            // Dynamic Status Output Display
            if (voiceStatus.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("POS Command Dispatcher Logs:", fontSize = 10.sp, color = ForestSage)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(voiceStatus, fontSize = 13.sp, color = AccentGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Select Simulator chips
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Simulate Spoken Commands:", fontSize = 11.sp, color = ForestSage, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(sampleVoiceCommands) { cmd ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ForestDark),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ForestSage.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable {
                                spokenText = cmd
                                onVoiceCommand(cmd)
                            }
                        ) {
                            Box(modifier = Modifier.padding(10.dp).fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentGold, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(cmd, fontSize = 11.sp, color = TextOnDarkIvory, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = spokenText,
                    onValueChange = { spokenText = it },
                    placeholder = { Text("Or enter a custom voice dictation text here...", color = IvoryDark, fontSize = 12.sp) },
                    textStyle = LocalTextStyle.current.copy(color = TextOnLightForest),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IvoryTrue,
                        unfocusedContainerColor = IvoryTrue,
                        focusedBorderColor = ForestPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (spokenText.isNotBlank()) {
                            onVoiceCommand(spokenText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark)
                ) {
                    Text("Execute")
                }
            }

            TextButton(onClick = onClearStatus) {
                Text("Clear Log Output", color = Color.Red.copy(alpha = 0.8f))
            }
        }
    }
}

// ------------------------------------------
// SUB-TAB 4: AI OCR & DOCUMENT PROCESSING
// ------------------------------------------
@Composable
fun AiOcrSubTab(
    extractedFields: Map<String, String>?,
    isLoading: Boolean,
    onScanDoc: (String) -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val documents = listOf("Supplier Invoice", "Delivery Note", "Bank Statement")
    var activeDocType by remember { mutableStateOf("Supplier Invoice") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("AI-Powered Optical Character Recognition (OCR)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Instantly extracts details from physical purchase receipts, shipping delivery sheets and banks statements, and populates forms.", fontSize = 11.sp, color = ForestSage)
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Select Document Type to Scan", fontSize = 11.sp, color = ForestSage)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        documents.forEach { doc ->
                            val isSel = activeDocType == doc
                            FilterChip(
                                selected = isSel,
                                onClick = { activeDocType = doc },
                                label = { Text(doc, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = ForestDark,
                                    containerColor = ForestDark,
                                    labelColor = TextOnDarkIvory
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onScanDoc(activeDocType) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = ForestDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = ForestDark, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Process Document Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Extracted Fields Form Review (Human-In-The-Loop)
        if (extractedFields != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestMedium.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ML Kit OCR Review Form", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            Text("98.5% Accuracy", fontSize = 10.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        }
                        Text("Please verify extracted fields before saving to Spices ledger.", fontSize = 11.sp, color = ForestSage)
                        Spacer(modifier = Modifier.height(10.dp))

                        extractedFields.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(key, fontSize = 11.sp, color = IvoryDark, fontWeight = FontWeight.SemiBold)
                                Text(value, fontSize = 11.sp, color = TextOnDarkIvory, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClear,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                            ) {
                                Text("Discard Scan", color = Color.Red)
                            }

                            Button(
                                onClick = onSave,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary, contentColor = AccentGold)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save to ERP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// SUB-TAB 5: RISK & FRAUD ANOMALY MONITOR
// ------------------------------------------
@Composable
fun AiRiskSubTab(
    alerts: List<FraudAlert>,
    onDismiss: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestMedium),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("AI Security & Fraud Anomaly Radar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scans POS refund volumes, stock adjustments, vendor pricing matrices, and journal ledgers automatically to capture anomalous trends.", fontSize = 11.sp, color = ForestSage)
                }
            }
        }

        if (alerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, tint = Color.Green, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Suspicious Risk Anomalies Found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextOnDarkIvory)
                        Text("Systems fully secure. Audit logging is active.", fontSize = 11.sp, color = ForestSage)
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                val badgeColor = when (alert.riskLevel.lowercase(Locale.ROOT)) {
                    "critical" -> Color.Red
                    "high" -> Color(0xFFFF5722)
                    "medium" -> AccentGold
                    else -> Color.Green
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestMedium),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(alert.riskLevel.uppercase(Locale.ROOT), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(alert.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextOnDarkIvory)
                            }
                            IconButton(onClick = { onDismiss(alert.id) }) {
                                Icon(Icons.Default.Check, contentDescription = "Resolve", tint = ForestSage)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(alert.description, fontSize = 11.sp, color = IvoryDark, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Source Module: ${alert.module}", fontSize = 9.sp, color = ForestSage, fontWeight = FontWeight.Bold)
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date(alert.timestamp)),
                                fontSize = 9.sp,
                                color = ForestSage
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// GENERAL COMPOSABLE DESIGN WIDGETS
// ------------------------------------------
@Composable
fun SimpleBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    color: Color = AccentGold
) {
    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ForestMedium.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        data.forEach { (label, value) ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontSize = 11.sp, color = IvoryDark, maxLines = 1)
                    Text("Rs. ${String.format(Locale.US, "%,.0f", value)}", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(ForestDark, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (maxVal > 0) (value / maxVal).toFloat().coerceIn(0f, 1f) else 0f)
                            .fillMaxHeight()
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun BusinessHealthGauge(score: Int, label: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(100.dp)
    ) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxSize(),
            color = if (score > 80) Color(0xFF4CAF50) else if (score > 50) AccentGold else Color(0xFFF44336),
            strokeWidth = 8.dp,
            trackColor = ForestDark
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextOnDarkIvory)
            Text(label, fontSize = 9.sp, color = ForestSage)
        }
    }
}

@Composable
fun ForecastTrendChart(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ForestMedium.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("7-Day Sales Forecast Trend (LKR)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val points = listOf(140000.0, 165000.0, 150000.0, 190000.0, 185000.0, 220000.0, 245000.0)
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val maxVal = points.maxOrNull() ?: 1.0

            points.forEachIndexed { idx, price ->
                val barHeight = (price / maxVal).toFloat().coerceIn(0.1f, 1.0f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(barHeight)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(AccentGold, AccentGold.copy(alpha = 0.3f))
                                ),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(days[idx], fontSize = 9.sp, color = ForestSage)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(AccentGold, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Predictive Median", fontSize = 9.sp, color = ForestSage)
            }
            Text("Accuracy: 94.2% (95% CI)", fontSize = 9.sp, color = AccentGold, fontWeight = FontWeight.Bold)
        }
    }
}
