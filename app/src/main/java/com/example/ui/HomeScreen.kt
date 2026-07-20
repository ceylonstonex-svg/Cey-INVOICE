package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ForestLight
import com.example.ui.theme.ForestMedium
import com.example.ui.theme.ForestPrimary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: InvoiceViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val invoices by viewModel.invoicesList.collectAsState()
    val products by viewModel.productsList.collectAsState()
    val context = LocalContext.current
    var showCalculator by remember { mutableStateOf(false) }
    var showWhatsAppBot by remember { mutableStateOf(false) }

    // Live statistics
    val totalRevenue = remember(invoices) {
        invoices.sumOf { it.grandTotal }
    }
    val invoiceCount = invoices.size
    val productCount = products.size

    // Staggered loading states
    var visibleIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..6) {
            delay(120)
            visibleIndex = i
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .testTag("home_screen_container")
    ) {
        // 1. Beautiful animated drifting spice particles/leaves in background
        DriftingLeavesBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 2. Animated Header Brand with Golden Emblem
            AnimatedVisibility(
                visible = visibleIndex >= 1,
                enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { -40 })
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ceyvana_logo),
                        contentDescription = "Ceyvana Logo",
                        modifier = Modifier
                            .height(85.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 3. Hero Card with beautiful custom generated image banner & scale entrance
            AnimatedVisibility(
                visible = visibleIndex >= 2,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + slideInVertically(initialOffsetY = { 80 })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_spices_hero),
                            contentDescription = "Sri Lankan Spices Hero",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(AccentGold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GLOBAL EXPORTS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ceylon's Finest Quality Billing",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Nature's authentic spices, calculated with extreme precision.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 4. Animated live accounting counting stats grid
            AnimatedVisibility(
                visible = visibleIndex >= 3,
                enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { 60 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PLATFORM OVERVIEW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Invoiced",
                            icon = Icons.Default.MonetizationOn,
                            iconColor = AccentGold,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            AnimatedPriceText(targetValue = totalRevenue, currencySymbol = "Rs.")
                        }

                        StatCard(
                            title = "Invoices",
                            icon = Icons.Default.Description,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedCountText(targetValue = invoiceCount)
                        }

                        StatCard(
                            title = "Products",
                            icon = Icons.Default.Spa,
                            iconColor = ForestLight,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedCountText(targetValue = productCount)
                        }
                    }
                }
            }

            // 5. Quick actions asymmetric grid with spring interactive clicks
            AnimatedVisibility(
                visible = visibleIndex >= 4,
                enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { 60 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QUICK ACTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                title = "New Invoice",
                                description = "Draft & calculate instantly",
                                icon = Icons.Outlined.ReceiptLong,
                                tintColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.createNewInvoice()
                                    // Switches to Invoices tab where details can be edited
                                    onNavigateToTab(1)
                                }
                            )
                            QuickActionCard(
                                title = "Spice Catalog",
                                description = "Manage product prices",
                                icon = Icons.Outlined.Inventory,
                                tintColor = AccentGold,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(4) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                title = "Sales Reports",
                                description = "Profit margins analysis",
                                icon = Icons.Default.Insights,
                                tintColor = ForestLight,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(3) }
                            )
                            QuickActionCard(
                                title = "Spice Calculator",
                                description = "Pricing, margins & math",
                                icon = Icons.Default.Calculate,
                                tintColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                                onClick = { showCalculator = true }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                title = "WhatsApp BOT",
                                description = "Automated assistant chatbot",
                                icon = Icons.Default.Forum,
                                tintColor = Color(0xFF00E676),
                                modifier = Modifier.weight(1f),
                                onClick = { showWhatsAppBot = true }
                            )
                            QuickActionCard(
                                title = "Sync Cloud",
                                description = "Backup & sync ledger",
                                icon = Icons.Default.CloudSync,
                                tintColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(2) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionCard(
                                title = "සජීවී විකුණුම් (POS System)",
                                description = "Instant spice packages checkout & bill printing",
                                icon = Icons.Default.Storefront,
                                tintColor = AccentGold,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(5) }
                            )
                        }
                    }
                }
            }

            // Interactive Ceyvana Zen Aroma Visualizer Animation Card
            AnimatedVisibility(
                visible = visibleIndex >= 5,
                enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { 60 })
            ) {
                CeyvanaZenAromaVisualizer()
            }

            // 6. Sri Lankan Spice Route Wisdom / Inspiring Quotes Card
            AnimatedVisibility(
                visible = visibleIndex >= 6,
                enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { 60 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Wisdom",
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Ceylon Spice Origin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sri Lankan spices (especially True Cinnamon and Pure Cardamom) represent a heritage of unmatched fragrance, organic farming, and international excellence since ancient maritime routes.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCalculator) {
        SpiceCalculatorSheet(onDismiss = { showCalculator = false })
    }

    if (showWhatsAppBot) {
        WhatsAppBotSheet(onDismiss = { showWhatsAppBot = false }, viewModel = viewModel)
    }
}

@Composable
fun DriftingLeavesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "drifting_leaves")

    // Define 4 animated float streams for x, y offsets and rotation
    val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_1"
    )

    val drift2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width > 0 && height > 0) {
            // Draw Leaf 1
            val angle1Rad = Math.toRadians(drift1.toDouble())
            val x1 = (width * 0.15f + Math.sin(angle1Rad) * 40f).toFloat()
            val y1 = (height * 0.25f + Math.cos(angle1Rad) * 60f).toFloat()
            drawLeafParticle(x = x1, y = y1, rotation = drift1, sizeDp = 22f)

            // Draw Leaf 2
            val angle2Rad = Math.toRadians(drift2.toDouble())
            val x2 = (width * 0.85f + Math.cos(angle2Rad) * 50f).toFloat()
            val y2 = (height * 0.65f + Math.sin(angle2Rad) * 70f).toFloat()
            drawLeafParticle(x = x2, y = y2, rotation = -drift2, sizeDp = 18f)

            // Draw Leaf 3
            val x3 = (width * 0.7f + Math.sin(angle1Rad * 0.7) * 30f).toFloat()
            val y3 = (height * 0.15f + Math.cos(angle1Rad * 0.5) * 40f).toFloat()
            drawLeafParticle(x = x3, y = y3, rotation = drift1 * 1.5f, sizeDp = 14f)

            // Draw Leaf 4
            val x4 = (width * 0.3f + Math.cos(angle2Rad * 1.2) * 60f).toFloat()
            val y4 = (height * 0.8f + Math.sin(angle2Rad * 0.8) * 50f).toFloat()
            drawLeafParticle(x = x4, y = y4, rotation = drift2 * 0.7f, sizeDp = 25f)
        }
    }
}

// Draw a beautiful organic stylized leaf vector on Canvas
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLeafParticle(
    x: Float,
    y: Float,
    rotation: Float,
    sizeDp: Float
) {
    val sizePx = sizeDp * density
    val path = Path().apply {
        moveTo(0f, -sizePx / 2)
        cubicTo(sizePx / 3, -sizePx / 3, sizePx / 2, -sizePx / 8, sizePx / 4, sizePx / 2)
        lineTo(0f, sizePx / 3)
        cubicTo(-sizePx / 4, sizePx / 2, -sizePx / 2, -sizePx / 8, -sizePx / 3, -sizePx / 3)
        close()
    }

    drawContext.canvas.save()
    drawContext.canvas.translate(x, y)
    drawContext.canvas.rotate(rotation)
    drawPath(
        path = path,
        color = AccentGold.copy(alpha = 0.12f),
        style = Stroke(width = 1.5f * density)
    )
    drawContext.canvas.restore()
}

@Composable
fun StatCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title.uppercase(Locale.US),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
            content()
        }
    }
}

@Composable
fun AnimatedPriceText(targetValue: Double, currencySymbol: String) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "price_anim"
    )
    val formatted = remember(animatedValue) {
        String.format(Locale.US, "%,.2f", animatedValue.toDouble())
    }
    Text(
        text = "$currencySymbol $formatted",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start
    )
}

@Composable
fun AnimatedCountText(targetValue: Int) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "count_anim"
    )
    Text(
        text = animatedValue.toString(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start
    )
}

@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "action_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tintColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
