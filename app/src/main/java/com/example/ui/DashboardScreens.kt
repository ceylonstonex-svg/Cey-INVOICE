package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvoiceWithLineItems
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// High-precision analytics container classes
data class SpicesStats(
    val totalRevenue: Double,
    val totalPacks: Int,
    val totalGrams: Int,
    val totalKilograms: Double,
    val profitAt30: Double,
    val profitAt35: Double,
    val profitAt40: Double,
    val profitAt45: Double,
    val costAt30: Double,
    val costAt35: Double,
    val costAt40: Double,
    val costAt45: Double
)

@Composable
fun SalesDashboardScreen(viewModel: InvoiceViewModel) {
    val invoices by viewModel.invoicesList.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val todaySales = remember(invoices) { calculateTodaySales(invoices) }
    val weeklySales = remember(invoices) { calculateWeeklySales(invoices) }
    val monthlySales = remember(invoices) { calculateMonthlySales(invoices) }
    val totalSales = remember(invoices) { invoices.sumOf { it.grandTotal } }

    val daysData = remember(invoices) { get7DaysSalesData(invoices) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sales Performance",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Live analytics from local billing ledger",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_ceyvana_logo),
                    contentDescription = "Ceyvana Logo",
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(3.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }

        // Metrics 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Today's Sales",
                        value = todaySales,
                        icon = Icons.Default.Today,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Weekly Sales",
                        value = weeklySales,
                        icon = Icons.Default.DateRange,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Monthly Sales",
                        value = monthlySales,
                        icon = Icons.Default.CalendarMonth,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "All-Time Revenue",
                        value = totalSales,
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = ForestLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Google Drive Backup & Sync Card
        item {
            GoogleDriveSyncCard(viewModel = viewModel, googleAccount = googleAccount, context = context)
        }

        // Custom High-Precision Chart Visualizer
        item {
            TechnicalSalesChart(daysData = daysData)
        }

        // Business Summary Highlights
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Insights",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "LATEST BUSINESS INSIGHTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeInvoices = invoices.size
                    val avgInvoice = if (activeInvoices > 0) totalSales / activeInvoices else 0.0

                    InsightRow(
                        label = "Average Transaction Value",
                        value = "Rs. " + String.format(Locale.US, "%,.2f", avgInvoice),
                        desc = "Mean revenue generated per issued invoice"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    val maxDay = daysData.maxByOrNull { it.second }
                    InsightRow(
                        label = "Peak Sales Day (Last 7 Days)",
                        value = maxDay?.let { "${it.first} (Rs. " + String.format(Locale.US, "%,.0f", it.second) + ")" } ?: "N/A",
                        desc = "Day with the highest billing throughput"
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleDriveSyncCard(
    viewModel: InvoiceViewModel,
    googleAccount: com.google.android.gms.auth.api.signin.GoogleSignInAccount?,
    context: android.content.Context
) {
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                viewModel.updateGoogleAccount(account)
            } catch (e: Exception) {
                android.util.Log.e("GoogleDrive", "Sign in failed", e)
                android.widget.Toast.makeText(context, "Link failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Cloud Backup",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "GOOGLE DRIVE BACKUP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Backup or restore your billing invoices and product catalog pricing to your Google Drive account.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (googleAccount == null) {
                Button(
                    onClick = {
                        val intent = viewModel.googleDriveService.getSignInIntent()
                        signInLauncher.launch(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Google Drive", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Linked",
                            tint = ForestLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Linked Account",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = googleAccount.email ?: "Ceyvanainfo@gmail.com",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.backupToGoogleDrive(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.restoreFromGoogleDrive(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.googleDriveService.signOut { viewModel.updateGoogleAccount(null) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlink Account", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SpicesReportsScreen(invoices: List<InvoiceWithLineItems>) {
    val stats = remember(invoices) { calculateSpicesStats(invoices) }
    var selectedMarginScenario by remember { mutableStateOf(35) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Reports header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ceylon Spices Reports",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Dynamic profit auditing for handpicked Ceylon spices",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_ceyvana_logo),
                    contentDescription = "Ceyvana Logo",
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(3.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }

        // Ceylon spices overall performance summary card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CEYVANA SPICES REVENUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = " Ceylon Premium ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Rs. " + String.format(Locale.US, "%,.2f", stats.totalRevenue),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Packs Sold", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${stats.totalPacks} units", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Grams Sold", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${stats.totalGrams} g", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Kilograms Sold", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "%.2f kg", stats.totalKilograms), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Automatic Calculation of Profit Header
        item {
            Column {
                Text(
                    text = "Profit Scenario Calculator",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Automatic handpicked profit estimations by target margin",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Matrix Grid of Scenario Profits
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfitScenarioCard(
                        marginPercent = 30,
                        profit = stats.profitAt30,
                        cost = stats.costAt30,
                        isSelected = selectedMarginScenario == 30,
                        onClick = { selectedMarginScenario = 30 },
                        modifier = Modifier.weight(1f)
                    )
                    ProfitScenarioCard(
                        marginPercent = 35,
                        profit = stats.profitAt35,
                        cost = stats.costAt35,
                        isSelected = selectedMarginScenario == 35,
                        onClick = { selectedMarginScenario = 35 },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfitScenarioCard(
                        marginPercent = 40,
                        profit = stats.profitAt40,
                        cost = stats.costAt40,
                        isSelected = selectedMarginScenario == 40,
                        onClick = { selectedMarginScenario = 40 },
                        modifier = Modifier.weight(1f)
                    )
                    ProfitScenarioCard(
                        marginPercent = 45,
                        profit = stats.profitAt45,
                        cost = stats.costAt45,
                        isSelected = selectedMarginScenario == 45,
                        onClick = { selectedMarginScenario = 45 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Detailed selected scenario explanation
        item {
            val selectedProfit = when (selectedMarginScenario) {
                30 -> stats.profitAt30
                35 -> stats.profitAt35
                40 -> stats.profitAt40
                45 -> stats.profitAt45
                else -> stats.profitAt35
            }
            val selectedCost = when (selectedMarginScenario) {
                30 -> stats.costAt30
                35 -> stats.costAt35
                40 -> stats.costAt40
                45 -> stats.costAt45
                else -> stats.costAt35
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUDITED STATEMENT ($selectedMarginScenario% TARGET)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Spice Sales:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs. " + String.format(Locale.US, "%,.2f", stats.totalRevenue), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Audited Net Cost (Pre-Profit):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs. " + String.format(Locale.US, "%,.2f", selectedCost), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Handpicked Net Profit:", fontSize = 13.sp, color = ForestLight)
                        Text("Rs. " + String.format(Locale.US, "%,.2f", selectedProfit), fontSize = 14.sp, fontWeight = FontWeight.Black, color = ForestLight)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Estimates reflect strict separation of Ceyvana product line items from regular delivery and secondary service layers.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = androidx.compose.ui.text.TextStyle(lineHeight = 13.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfitScenarioCard(
    marginPercent: Int,
    profit: Double,
    cost: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$marginPercent% Margin",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rs. " + String.format(Locale.US, "%,.0f", profit),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else ForestLight
            )
            Text(
                text = "Profit Earned",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: Double,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Rs. " + String.format(Locale.US, "%,.2f", value),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InsightRow(
    label: String,
    value: String,
    desc: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TechnicalSalesChart(daysData: List<Pair<String, Double>>) {
    val maxVal = daysData.maxOfOrNull { it.second } ?: 1.0
    val displayMax = if (maxVal == 0.0) 1000.0 else maxVal * 1.15

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "7-DAY SALES VELOCITY (LKR)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val gridColor = MaterialTheme.colorScheme.surfaceVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val barCount = daysData.size
                val spacing = 16.dp.toPx()
                val totalSpacing = spacing * (barCount + 1)
                val barWidth = (width - totalSpacing) / barCount

                // Draw horizontal dotted grid lines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines)
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw bars and dynamic gradients
                daysData.forEachIndexed { index, (_, amount) ->
                    val x = spacing + index * (barWidth + spacing)
                    val barHeight = (amount / displayMax).toFloat() * height
                    val y = height - barHeight

                    if (amount > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        // Empty/zero state indicator
                        drawRoundRect(
                            color = gridColor,
                            topLeft = androidx.compose.ui.geometry.Offset(x, height - 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(barWidth, 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysData.forEach { (label, amount) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (amount >= 1000) {
                            String.format(Locale.US, "%.1fk", amount / 1000.0)
                        } else {
                            amount.toInt().toString()
                        },
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Stats aggregation helpers
fun calculateTodaySales(invoices: List<InvoiceWithLineItems>): Double {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis
    return invoices.filter { it.invoice.invoiceDate >= startOfToday }.sumOf { it.grandTotal }
}

fun calculateWeeklySales(invoices: List<InvoiceWithLineItems>): Double {
    val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
    return invoices.filter { it.invoice.invoiceDate >= sevenDaysAgo }.sumOf { it.grandTotal }
}

fun calculateMonthlySales(invoices: List<InvoiceWithLineItems>): Double {
    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    return invoices.filter { it.invoice.invoiceDate >= thirtyDaysAgo }.sumOf { it.grandTotal }
}

fun get7DaysSalesData(invoices: List<InvoiceWithLineItems>): List<Pair<String, Double>> {
    val days = mutableListOf<Pair<String, Double>>()
    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        
        val startCal = cal.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        
        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val daySales = invoices
            .filter { it.invoice.invoiceDate >= startCal.timeInMillis && it.invoice.invoiceDate <= endCal.timeInMillis }
            .sumOf { it.grandTotal }

        days.add(Pair(sdf.format(cal.time), daySales))
    }
    return days
}

fun calculateSpicesStats(invoices: List<InvoiceWithLineItems>): SpicesStats {
    var revenue = 0.0
    var packs = 0
    var grams = 0
    var kilograms = 0.0

    invoices.forEach { invoiceWithItems ->
        invoiceWithItems.lineItems.forEach { item ->
            if (item.description.contains("Ceyvana", ignoreCase = true)) {
                val itemRevenue = item.unitPrice * item.quantity
                revenue += itemRevenue

                val unitLower = item.unit.lowercase().trim()
                if (unitLower.contains("gram")) {
                    grams += item.quantity
                } else if (unitLower.contains("kilogram") || unitLower.contains("kg")) {
                    kilograms += item.quantity.toDouble()
                } else {
                    packs += item.quantity
                }
            }
        }
    }

    val profitAt30 = revenue * 0.30 / 1.30
    val costAt30 = revenue / 1.30

    val profitAt35 = revenue * 0.35 / 1.35
    val costAt35 = revenue / 1.35

    val profitAt40 = revenue * 0.40 / 1.40
    val costAt40 = revenue / 1.40

    val profitAt45 = revenue * 0.45 / 1.45
    val costAt45 = revenue / 1.45

    return SpicesStats(
        totalRevenue = revenue,
        totalPacks = packs,
        totalGrams = grams,
        totalKilograms = kilograms,
        profitAt30 = profitAt30,
        profitAt35 = profitAt35,
        profitAt40 = profitAt40,
        profitAt45 = profitAt45,
        costAt30 = costAt30,
        costAt35 = costAt35,
        costAt40 = costAt40,
        costAt45 = costAt45
    )
}
