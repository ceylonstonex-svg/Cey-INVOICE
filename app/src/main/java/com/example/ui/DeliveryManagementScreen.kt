package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeliveryManagementScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel Flows
    val drivers by viewModel.driversList.collectAsState()
    val vehicles by viewModel.vehiclesList.collectAsState()
    val zones by viewModel.deliveryZones.collectAsState()
    val deliveries by viewModel.deliveriesList.collectAsState()
    val fuelLogs by viewModel.fuelLogs.collectAsState()
    val isOffline by viewModel.deliveryOfflineMode.collectAsState()
    val activeRole by viewModel.driverActiveRole.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val gpsProgress by viewModel.gpsSimulationProgress.collectAsState()
    val trackingId by viewModel.activeTrackingDeliveryId.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()

    // Screen navigation tabs (within Delivery Module)
    var activeTab by remember { mutableStateOf("Dashboard") } // Dashboard, Scheduling, Drivers, FuelLogs

    // Dialog state controllers
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var showPodDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var selectedDeliveryForAction by remember { mutableStateOf<DeliveryRecord?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF070E0B), // Deep Forest background
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF0B1410))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF144D30), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Column {
                            Text(
                                text = "CEYVANA Logistics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                            Text(
                                text = "Premium Ceylon Spices Dispatch",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Online/Offline & Sync Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOffline) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF821C1C))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable { viewModel.toggleDeliveryOfflineMode() }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Offline Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("OFFLINE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // Visual sync button for offline cache syncing
                            IconButton(
                                onClick = { viewModel.syncOfflineData() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0F2E20), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Data",
                                    tint = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF144D30))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable { viewModel.toggleDeliveryOfflineMode() }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Online Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("ONLINE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Role Selector Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF050A08))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val roles = listOf("Manager", "Driver")
                    roles.forEach { role ->
                        val isSelected = activeRole == role
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ForestPrimary else Color.Transparent)
                                .clickable { viewModel.setDriverActiveRole(role) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$role View",
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (activeRole == "Manager") {
                NavigationBar(
                    containerColor = Color(0xFF0B1410),
                    tonalElevation = 8.dp
                ) {
                    val navItems = listOf(
                        Triple("Dashboard", Icons.Default.Dashboard, "Dashboard"),
                        Triple("Scheduling", Icons.Default.Schedule, "Dispatch"),
                        Triple("Drivers", Icons.Default.People, "Drivers/Vehicles"),
                        Triple("FuelLogs", Icons.Default.LocalGasStation, "Fuel & Analytics")
                    )

                    navItems.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGold,
                                selectedTextColor = AccentGold,
                                indicatorColor = Color(0xFF144D30),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Role Router
            if (activeRole == "Driver") {
                DriverInterface(
                    deliveries = deliveries,
                    drivers = drivers,
                    vehicles = vehicles,
                    viewModel = viewModel,
                    onOpenPod = { delivery ->
                        selectedDeliveryForAction = delivery
                        showPodDialog = true
                    },
                    onOpenReturn = { delivery ->
                        selectedDeliveryForAction = delivery
                        showReturnDialog = true
                    }
                )
            } else {
                // Manager Module router
                when (activeTab) {
                    "Dashboard" -> ManagerDashboard(
                        deliveries = deliveries,
                        drivers = drivers,
                        vehicles = vehicles,
                        fuelLogs = fuelLogs,
                        gpsProgress = gpsProgress,
                        trackingId = trackingId,
                        viewModel = viewModel
                    )
                    "Scheduling" -> SchedulingPanel(
                        deliveries = deliveries,
                        invoices = invoices,
                        zones = zones,
                        drivers = drivers,
                        vehicles = vehicles,
                        onScheduleClick = { showScheduleDialog = true },
                        onAssignClick = { delivery ->
                            selectedDeliveryForAction = delivery
                            showAssignDialog = true
                        },
                        onStatusChange = { deliveryId, nextStatus ->
                            viewModel.updateDeliveryStatus(deliveryId, nextStatus)
                        }
                    )
                    "Drivers" -> DriversVehiclesPanel(
                        drivers = drivers,
                        vehicles = vehicles
                    )
                    "FuelLogs" -> FuelAnalyticsPanel(
                        fuelLogs = fuelLogs,
                        vehicles = vehicles,
                        onAddLogClick = { showFuelDialog = true },
                        deliveries = deliveries
                    )
                }
            }

            // Sync/Loading Overlay overlay
            if (isSyncing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1B14)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = AccentGold)
                            Text(
                                text = "Syncing with Cloud Spanner...",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS & SHEET OVERLAYS
    // =========================================================================

    // 1. Schedule Delivery Dialog (Real Sales Module Integration)
    if (showScheduleDialog) {
        var selectedInvoiceNum by remember { mutableStateOf("") }
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custAddr by remember { mutableStateOf("") }
        var selectedZone by remember { mutableStateOf(zones.firstOrNull()?.name ?: "") }
        var priority by remember { mutableStateOf("Standard") }
        var weight by remember { mutableStateOf("5.0") }
        var charge by remember { mutableStateOf("500.0") }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            containerColor = Color(0xFF0C1B14),
            title = { Text("Schedule Spices Dispatch", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("Connect Sales Invoice / Order", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Select from actual system invoices
                        if (invoices.isEmpty()) {
                            Text("No recent invoices in system. Simulated Invoice selected.", color = Color.Gray, fontSize = 11.sp)
                            selectedInvoiceNum = "MOCK-INV-101"
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF050A08))
                                    .padding(12.dp)
                                    .clickable {
                                        // Quick auto-picker of the latest invoice
                                        val latest = invoices.first()
                                        selectedInvoiceNum = latest.invoice.invoiceNumber
                                        custName = latest.invoice.customerName
                                        custPhone = latest.invoice.customerPhone
                                        custAddr = "Galle Road, Colombo"
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedInvoiceNum.isEmpty()) "Tap to Import Recent Order" else "Imported: $selectedInvoiceNum",
                                    color = if (selectedInvoiceNum.isEmpty()) AccentGold else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.CloudDownload, contentDescription = "Import", tint = AccentGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = custName,
                            onValueChange = { custName = it },
                            label = { Text("Customer Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = AccentGold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = custPhone,
                            onValueChange = { custPhone = it },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = AccentGold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = custAddr,
                            onValueChange = { custAddr = it },
                            label = { Text("Delivery Address") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = AccentGold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Delivery Zone & Charge", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF050A08))
                                    .padding(12.dp)
                            ) {
                                Text(selectedZone, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = charge,
                                onValueChange = { charge = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Charge (Rs.)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Weight (Kg)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Priority Switcher
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Priority", color = Color.White, fontSize = 10.sp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF050A08))
                                        .clickable {
                                            priority = if (priority == "Standard") "Express" else "Standard"
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(priority, color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (custName.isEmpty() || custAddr.isEmpty()) {
                            Toast.makeText(context, "Please complete fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.scheduleDelivery(
                            invoiceNumber = selectedInvoiceNum.ifEmpty { "INV-${System.currentTimeMillis() % 10000}" },
                            customerName = custName,
                            customerPhone = custPhone,
                            customerAddress = custAddr,
                            zoneName = selectedZone,
                            priority = priority,
                            weightKg = weight.toDoubleOrNull() ?: 5.0,
                            scheduledTime = System.currentTimeMillis() + 4 * 60 * 60 * 1000,
                            deliveryCharge = charge.toDoubleOrNull() ?: 500.0
                        )
                        showScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                ) {
                    Text("Schedule", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // 2. Assign Driver & Vehicle Override Dialog
    if (showAssignDialog && selectedDeliveryForAction != null) {
        val delivery = selectedDeliveryForAction!!
        val availableDrivers = drivers.filter { it.status == "Available" }
        val availableVehicles = vehicles.filter { it.status == "Available" }

        var selectedDriverId by remember { mutableStateOf(availableDrivers.firstOrNull()?.id ?: "") }
        var selectedVehicleReg by remember { mutableStateOf(availableVehicles.firstOrNull()?.registrationNumber ?: "") }

        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            containerColor = Color(0xFF0C1B14),
            title = { Text("Assign Delivery Fleet", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Driver & Vehicle to handle delivery for ${delivery.customerName}.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                    // Driver Selection
                    Column {
                        Text("Available Driver", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (availableDrivers.isEmpty()) {
                            Text("No available drivers found! Assigning anyway (override active).", color = Color.Red, fontSize = 11.sp)
                            selectedDriverId = drivers.firstOrNull()?.id ?: ""
                        } else {
                            availableDrivers.forEach { driver ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedDriverId == driver.id) ForestPrimary else Color(0xFF050A08))
                                        .clickable { selectedDriverId = driver.id }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(driver.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("⭐ ${driver.rating}", color = AccentGold, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // Vehicle Selection
                    Column {
                        Text("Available Vehicle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (availableVehicles.isEmpty()) {
                            Text("No available vehicles found!", color = Color.Red, fontSize = 11.sp)
                            selectedVehicleReg = vehicles.firstOrNull()?.registrationNumber ?: ""
                        } else {
                            availableVehicles.forEach { vehicle ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedVehicleReg == vehicle.registrationNumber) ForestPrimary else Color(0xFF050A08))
                                        .clickable { selectedVehicleReg = vehicle.registrationNumber }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${vehicle.type} (${vehicle.registrationNumber})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Cap: ${vehicle.capacityKg}Kg", color = AccentGold, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDriverId.isEmpty() || selectedVehicleReg.isEmpty()) {
                            Toast.makeText(context, "Driver/Vehicle required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.assignDriverAndVehicle(delivery.id, selectedDriverId, selectedVehicleReg)
                        showAssignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                ) {
                    Text("Confirm Dispatch", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // 3. Fuel Logs Purchasing Entry Dialog (Linked with ERP Financial Accounts)
    if (showFuelDialog) {
        var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()?.registrationNumber ?: "") }
        var litres by remember { mutableStateOf("30.0") }
        var pricePerLitre by remember { mutableStateOf("325.0") }
        var mileage by remember { mutableStateOf("45000") }

        AlertDialog(
            onDismissRequest = { showFuelDialog = false },
            containerColor = Color(0xFF0C1B14),
            title = { Text("Log Fuel Purchase & Auto Post ERP", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Records fuel expense directly to COA ledger account (5050 - Transport & Logistics).", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                    // Vehicle registration
                    OutlinedTextField(
                        value = selectedVehicle,
                        onValueChange = { selectedVehicle = it },
                        label = { Text("Vehicle Reg Number") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Litres
                    OutlinedTextField(
                        value = litres,
                        onValueChange = { litres = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Litres Purchased") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Rate
                    OutlinedTextField(
                        value = pricePerLitre,
                        onValueChange = { pricePerLitre = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Price per Litre (Rs.)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Current mileage
                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Current Mileage (Km)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val l = litres.toDoubleOrNull() ?: 0.0
                        val cost = pricePerLitre.toDoubleOrNull() ?: 0.0
                        val m = mileage.toDoubleOrNull() ?: 0.0
                        if (selectedVehicle.isEmpty() || l <= 0.0 || cost <= 0.0) {
                            Toast.makeText(context, "Invalid input data", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.logFuelPurchase(selectedVehicle, l, cost, m)
                        showFuelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                ) {
                    Text("Post Ledger Logs", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFuelDialog = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // 4. Complete Proof of Delivery Dialog (Signature Pad Canvas, Photo Placeholder, Verified OTP)
    if (showPodDialog && selectedDeliveryForAction != null) {
        val delivery = selectedDeliveryForAction!!
        var drawPoints = remember { mutableStateListOf<Pair<Float, Float>>() }
        var mockOtp by remember { mutableStateOf("842103") } // Presetted OTP
        var enteredOtp by remember { mutableStateOf("") }
        var driverNotes by remember { mutableStateOf("") }
        var driverPhotoPlaceholder by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPodDialog = false },
            containerColor = Color(0xFF0C1B14),
            modifier = Modifier.fillMaxWidth(0.95f),
            title = { Text("Verify & Capture Proof of Delivery", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF050A08)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Customer: ${delivery.customerName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Address: ${delivery.customerAddress}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("Secure Code required: $mockOtp", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = { enteredOtp = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Secure OTP Verification Code") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Touch Signature Pad
                    item {
                        Text("Customer Digital Signature (Touch Screen Sign)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.5.dp, AccentGold, RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        drawPoints.add(Pair(change.position.x, change.position.y))
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (drawPoints.isNotEmpty()) {
                                    val path = Path().apply {
                                        moveTo(drawPoints.first().first, drawPoints.first().second)
                                        for (i in 1 until drawPoints.size) {
                                            lineTo(drawPoints[i].first, drawPoints[i].second)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }

                            if (drawPoints.isEmpty()) {
                                Text(
                                    text = "Sign with finger here",
                                    color = Color.Gray.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                IconButton(
                                    onClick = { drawPoints.clear() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Red)
                                }
                            }
                        }
                    }

                    // Driver Photo Attachment
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF050A08))
                                    .clickable { driverPhotoPlaceholder = !driverPhotoPlaceholder }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (driverPhotoPlaceholder) "Photo Captured ✓" else "Capture Delivery Photo",
                                        fontSize = 11.sp,
                                        color = if (driverPhotoPlaceholder) Color.Green else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (driverPhotoPlaceholder) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }

                    // Drivers Notes
                    item {
                        OutlinedTextField(
                            value = driverNotes,
                            onValueChange = { driverNotes = it },
                            label = { Text("Driver Delivery Notes") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredOtp != mockOtp) {
                            Toast.makeText(context, "Verification Failed: Incorrect OTP Secure Code!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (drawPoints.isEmpty()) {
                            Toast.makeText(context, "Customer signature is required for POD compliance", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.captureProofOfDelivery(
                            deliveryId = delivery.id,
                            signature = drawPoints.toList(),
                            photoPath = if (driverPhotoPlaceholder) "secure_cloud_pod_photo_uri" else null,
                            otpCode = enteredOtp,
                            notes = driverNotes
                        )
                        showPodDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                ) {
                    Text("Complete Handover", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPodDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // 5. Failed Delivery & Return Management Dialog
    if (showReturnDialog && selectedDeliveryForAction != null) {
        val delivery = selectedDeliveryForAction!!
        val reasons = listOf("Customer unavailable", "Wrong address", "Product damaged", "Customer refused", "Payment failure")
        var selectedReason by remember { mutableStateOf("Customer unavailable") }
        var returnNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            containerColor = Color(0xFF0C1B14),
            title = { Text("Process Return & Auto Restock", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Processing this return will automatically restock spices back to database products inventory.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                    Column {
                        reasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedReason == reason) Color(0xFF821C1C) else Color(0xFF050A08))
                                    .clickable { selectedReason = reason }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(reason, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (selectedReason == reason) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    OutlinedTextField(
                        value = returnNotes,
                        onValueChange = { returnNotes = it },
                        label = { Text("Additional notes") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDeliveryStatus(delivery.id, DeliveryStatus.RETURNED, "Return Reason: $selectedReason. Notes: $returnNotes")
                        showReturnDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Process Restock Return", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

// =============================================================================
// SUB-PANELS & COMPONENTS
// =============================================================================

@Composable
fun ManagerDashboard(
    deliveries: List<DeliveryRecord>,
    drivers: List<DeliveryDriver>,
    vehicles: List<DeliveryVehicle>,
    fuelLogs: List<FuelLog>,
    gpsProgress: Float,
    trackingId: String?,
    viewModel: InvoiceViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dashboard metrics helper
    val todayDeliveries = deliveries.size
    val completedCount = deliveries.count { it.status == DeliveryStatus.DELIVERED }
    val pendingCount = deliveries.count { it.status == DeliveryStatus.PENDING || it.status == DeliveryStatus.SCHEDULED }
    val transitCount = deliveries.count { it.status == DeliveryStatus.IN_TRANSIT || it.status == DeliveryStatus.ARRIVING }
    val failedCount = deliveries.count { it.status == DeliveryStatus.FAILED || it.status == DeliveryStatus.RETURNED }
    val activeDrivers = drivers.count { it.status == "On Delivery" }
    
    // Dynamic fuel stats
    val totalFuelSpent = fuelLogs.sumOf { it.totalCost }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Delivery Management Stats Title
        item {
            Text(
                text = "Dispatch & Control Centre",
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Dashboard Stats Matrix
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = "Scheduled",
                    value = todayDeliveries.toString(),
                    icon = Icons.Default.PendingActions,
                    color = Color(0xFF144D30),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "In Transit",
                    value = transitCount.toString(),
                    icon = Icons.Default.LocalShipping,
                    color = Color(0xFFD4AF37),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Completed",
                    value = completedCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Returns / Fail",
                    value = failedCount.toString(),
                    icon = Icons.Default.AssignmentReturn,
                    color = Color(0xFF821C1C),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary Metrics row
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCardMini(
                    label = "Active Fleet",
                    value = "${drivers.count { it.status != "Off Duty" }} Drivers",
                    modifier = Modifier.weight(1f)
                )
                StatCardMini(
                    label = "Total Fuel Spend",
                    value = "Rs. ${String.format(Locale.US, "%,.2f", totalFuelSpent)}",
                    modifier = Modifier.weight(1f)
                )
                StatCardMini(
                    label = "CSAT Delivery Rate",
                    value = "98.4% ⭐",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Delivery Map Simulation Card (Canvas custom maps)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_gps_map_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Text("Live Delivery Map (Simulation Engine)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }

                        if (trackingId != null) {
                            Text(
                                text = "TRK: ${trackingId.take(6).uppercase()}",
                                fontSize = 10.sp,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Draw Live Map Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF050A08))
                            .border(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw Central Hub Location
                            drawCircle(
                                color = Color(0xFF144D30),
                                radius = 12f,
                                center = Offset(size.width * 0.2f, size.height * 0.7f)
                            )

                            // Draw Customer Location
                            drawCircle(
                                color = Color(0xFF821C1C),
                                radius = 12f,
                                center = Offset(size.width * 0.8f, size.height * 0.3f)
                            )

                            // Draw simulated Route path
                            val path = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.7f)
                                quadraticTo(
                                    size.width * 0.4f, size.height * 0.3f,
                                    size.width * 0.5f, size.height * 0.6f
                                )
                                lineTo(size.width * 0.8f, size.height * 0.3f)
                            }
                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.15f),
                                style = Stroke(width = 4f)
                            )

                            // Draw Live vehicle marker position along path if simulating
                            if (trackingId != null) {
                                val currentX = (size.width * 0.2f) + (size.width * 0.6f) * gpsProgress
                                val currentY = (size.height * 0.7f) - (size.height * 0.4f) * gpsProgress
                                
                                drawCircle(
                                    color = AccentGold,
                                    radius = 16f,
                                    center = Offset(currentX, currentY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 6f,
                                    center = Offset(currentX, currentY)
                                )
                            }
                        }

                        // Overlay labels
                        Text(
                            text = "Colombo Hub (Pettah Depot)",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )

                        Text(
                            text = "Customer Address (Galle Face)",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )

                        if (trackingId == null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ForestPrimary)
                                    .clickable {
                                        val match = deliveries.firstOrNull { it.status == DeliveryStatus.ASSIGNED }
                                        if (match != null) {
                                            viewModel.startGpsSimulation(match.id)
                                        } else {
                                            Toast.makeText(context, "No Assigned Delivery to simulate tracking!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Simulate GPS Tracking", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (trackingId != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ETA: ${((1.0f - gpsProgress) * 30).toInt()} Mins Left",
                                fontSize = 11.sp,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = gpsProgress,
                                color = AccentGold,
                                trackColor = Color.DarkGray,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Audit Logistics Logs List (Connected to ERP System Logs)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Logistics Dispatch Audit Trails", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val items = listOf(
                        "15:32 - DRV-101 Assigned Cargo Delivery for INV-2026-001",
                        "14:15 - New bulk spice delivery order created from POS screen checkout",
                        "11:05 - Driver Saman Kumara marked off-duty with safe vehicle parking check"
                    )

                    items.forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(4.dp).background(AccentGold, CircleShape))
                            Text(log, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchedulingPanel(
    deliveries: List<DeliveryRecord>,
    invoices: List<InvoiceWithLineItems>,
    zones: List<DeliveryZone>,
    drivers: List<DeliveryDriver>,
    vehicles: List<DeliveryVehicle>,
    onScheduleClick: () -> Unit,
    onAssignClick: (DeliveryRecord) -> Unit,
    onStatusChange: (String, DeliveryStatus) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Scheduled", "Assigned", "Delivered")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Deliveries Scheduling Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            Button(
                onClick = onScheduleClick,
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontal filter bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentGold else Color(0xFF0B1410))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Deliveries list
        val filteredList = remember(deliveries, selectedFilter) {
            if (selectedFilter == "All") deliveries else {
                deliveries.filter { it.status.label.contains(selectedFilter, ignoreCase = true) }
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No deliveries match this status.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(filteredList) { record ->
                    DeliveryRecordItem(
                        record = record,
                        onAssign = { onAssignClick(record) },
                        onStatusChange = { next -> onStatusChange(record.id, next) }
                    )
                }
            }
        }
    }
}

@Composable
fun DriversVehiclesPanel(
    drivers: List<DeliveryDriver>,
    vehicles: List<DeliveryVehicle>
) {
    var selectedSection by remember { mutableStateOf("Drivers") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0B1410))
                .padding(4.dp)
        ) {
            val tabs = listOf("Drivers", "Vehicles")
            tabs.forEach { tab ->
                val isSelected = selectedSection == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ForestPrimary else Color.Transparent)
                        .clickable { selectedSection = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (selectedSection == "Drivers") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(drivers) { driver ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF144D30), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(driver.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text(driver.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("ID: ${driver.employeeId}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (driver.status == "Available") Color(0xFF1B5E20) else Color(0xFF821C1C))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(driver.status, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                                    Text(driver.phone, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                }
                                Text("⭐ ${driver.rating} rating", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vehicles) { vehicle ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text(vehicle.registrationNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${vehicle.type} • ${vehicle.fuelType}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (vehicle.status == "Available") Color(0xFF1B5E20) else Color(0xFF821C1C))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(vehicle.status, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Capacity: ${vehicle.capacityKg}Kg", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text("Odo: ${vehicle.currentMileage} Km", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FuelAnalyticsPanel(
    fuelLogs: List<FuelLog>,
    vehicles: List<DeliveryVehicle>,
    onAddLogClick: () -> Unit,
    deliveries: List<DeliveryRecord>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fuel Ledger & Logistics Accounting", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            IconButton(
                onClick = onAddLogClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(ForestPrimary, CircleShape)
            ) {
                Icon(Icons.Default.LocalGasStation, contentDescription = "Add Log", tint = Color.White)
            }
        }

        // Export Actions Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Generate Logistics Performance Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Compiles delivery records, CSAT Ratings, returns, driver performances, and fuel efficiencies.", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val formats = listOf("PDF", "Excel", "CSV")
                    formats.forEach { format ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF050A08))
                                .border(1.dp, AccentGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    isExporting = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1500)
                                        isExporting = false
                                        Toast.makeText(context, "Logistics Report exported to /Ceyvana/Reports in $format format!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(format, color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text("Logistics Refill Receipts", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        if (fuelLogs.isEmpty()) {
            Text("No fuel log reports found.", color = Color.Gray, fontSize = 12.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fuelLogs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF050A08)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = AccentGold)
                                Column {
                                    Text(log.vehicleReg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${log.litres}L @ Rs.${log.costPerLitre}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                }
                            }

                            Text(
                                text = "Rs. ${String.format(Locale.US, "%,.2f", log.totalCost)}",
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Driver dedicated simple UI
@Composable
fun DriverInterface(
    deliveries: List<DeliveryRecord>,
    drivers: List<DeliveryDriver>,
    vehicles: List<DeliveryVehicle>,
    viewModel: InvoiceViewModel,
    onOpenPod: (DeliveryRecord) -> Unit,
    onOpenReturn: (DeliveryRecord) -> Unit
) {
    val myDriverProfile = drivers.firstOrNull() // Seed driver
    val myAssignedDeliveries = deliveries.filter { it.assignedDriverId == myDriverProfile?.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome Back,", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text(myDriverProfile?.name ?: "Driver Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Assigned to: ${myDriverProfile?.assignedVehicleReg ?: "None"}", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF144D30))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("⭐ ${myDriverProfile?.rating}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Text("My Deliveries Today", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (myAssignedDeliveries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active assignments for you today.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(myAssignedDeliveries) { record ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("INV: ${record.invoiceNumber}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF144D30))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(record.status.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f))

                            Text("Customer: ${record.customerName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Address: ${record.customerAddress}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.updateDeliveryStatus(record.id, DeliveryStatus.IN_TRANSIT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Start Transit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenPod(record) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("Complete Handover", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenReturn(record) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF821C1C)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Failed/Return", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryRecordItem(
    record: DeliveryRecord,
    onAssign: () -> Unit,
    onStatusChange: (DeliveryStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INV: ${record.invoiceNumber}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Zone: ${record.deliveryZone}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (record.status) {
                                DeliveryStatus.DELIVERED -> Color(0xFF1B5E20)
                                DeliveryStatus.RETURNED, DeliveryStatus.FAILED -> Color(0xFF821C1C)
                                else -> Color(0xFF144D30)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(record.status.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Divider(color = Color.White.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Customer: ${record.customerName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Contact: ${record.customerPhone}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text("Address: ${record.customerAddress}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Weight: ${record.weightKg}Kg", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("Charge: Rs. ${record.deliveryCharge}", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (record.status == DeliveryStatus.SCHEDULED || record.status == DeliveryStatus.PENDING) {
                    Button(
                        onClick = onAssign,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Assign Transport", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (record.status == DeliveryStatus.ASSIGNED) {
                    Button(
                        onClick = { onStatusChange(DeliveryStatus.IN_TRANSIT) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dispatch (In Transit)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (record.status == DeliveryStatus.DELIVERED) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                        Text("POD Captured Securely", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// KPI widget design helper
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1410)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(title, fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatCardMini(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050A08))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
        }
    }
}
