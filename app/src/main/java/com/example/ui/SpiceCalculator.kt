package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ForestLight
import com.example.ui.theme.ForestMedium
import com.example.ui.theme.ForestPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiceCalculatorSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Standard, 1 = Bulk Trade

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AccentGold.copy(alpha = 0.5f)) },
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header with Animated Ceyvana Star
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculator icon",
                        tint = AccentGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "CEYVANA CALCULATOR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Dual-mode arithmetic & spice logistics calculator",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (activeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = AccentGold
                        )
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Quick Math", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Spice Trade", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Animated Screen Switching
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "calculator_tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> StandardCalculatorView()
                    1 -> SpiceTradeCalculatorView()
                }
            }
        }
    }
}

@Composable
fun StandardCalculatorView() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var isResultEvaluated by remember { mutableStateOf(false) }

    // Live real-time result preview as the user types
    val liveResult = remember(expression) {
        if (expression.isBlank()) {
            ""
        } else {
            try {
                var cleanExpr = expression
                while (cleanExpr.isNotEmpty() && "+-×÷".contains(cleanExpr.last())) {
                    cleanExpr = cleanExpr.dropLast(1)
                }
                if (cleanExpr.isBlank()) {
                    ""
                } else {
                    val computed = evaluateSimpleExpression(cleanExpr)
                    if (computed == "Error" || computed == "Div by 0") "" else computed
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun onKeyPress(key: String) {
        if (isResultEvaluated) {
            when (key) {
                "C" -> {
                    expression = ""
                    result = "0"
                    isResultEvaluated = false
                }
                "⌫" -> {
                    expression = ""
                    isResultEvaluated = false
                }
                "=" -> {
                    // Do nothing
                }
                "+", "-", "×", "÷" -> {
                    expression = result + key
                    isResultEvaluated = false
                }
                "%" -> {
                    expression = result + key
                    isResultEvaluated = false
                }
                "." -> {
                    expression = "0."
                    isResultEvaluated = false
                }
                else -> { // Numbers 0-9
                    expression = key
                    isResultEvaluated = false
                }
            }
            return
        }

        when (key) {
            "C" -> {
                expression = ""
                result = "0"
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            }
            "=" -> {
                if (expression.isNotBlank()) {
                    try {
                        val computed = evaluateSimpleExpression(expression)
                        history = (history + "$expression = $computed").takeLast(3)
                        result = computed
                        expression = computed
                        isResultEvaluated = true
                    } catch (e: Exception) {
                        result = "Error"
                    }
                }
            }
            "+", "-", "×", "÷" -> {
                if (expression.isNotEmpty()) {
                    val lastChar = expression.last()
                    if ("+-×÷%".contains(lastChar)) {
                        expression = expression.dropLast(1) + key
                    } else {
                        expression += key
                    }
                } else if (key == "-") {
                    expression += key
                }
            }
            "%" -> {
                if (expression.isNotEmpty() && !"+-×÷%".contains(expression.last())) {
                    expression += key
                }
            }
            "." -> {
                val tokens = expression.split(Regex("[+\\-×÷%]"))
                val lastToken = tokens.lastOrNull() ?: ""
                if (!lastToken.contains(".")) {
                    if (lastToken.isEmpty()) {
                        expression += "0."
                    } else {
                        expression += "."
                    }
                }
            }
            else -> { // Numbers 0-9
                if (expression == "0") {
                    expression = key
                } else {
                    expression += key
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Display Screen Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // History List
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    history.forEach { item ->
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Formula Expression Input
                    Text(
                        text = expression.ifEmpty { "Enter equation" },
                        fontSize = 18.sp,
                        color = if (expression.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Large Result Output (shows live real-time preview if typing, otherwise shows final result)
                    Text(
                        text = if (liveResult.isNotEmpty() && expression != liveResult) liveResult else result,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Numeric & Operators Keypad
        val keys = listOf(
            listOf("C", "÷", "⌫", "-"),
            listOf("7", "8", "9", "+"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "="),
            listOf("0", ".", "%", "")
        )

        Column(
            modifier = Modifier.weight(3f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        if (key.isNotEmpty()) {
                            CalculatorButton(
                                text = key,
                                modifier = Modifier
                                    .weight(if (key == "=") 1f else 1f)
                                    .fillMaxHeight(),
                                onClick = { onKeyPress(key) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOperator = "+-×÷=".contains(text)
    val isClear = text == "C" || text == "⌫"

    val containerColor = when {
        text == "=" -> AccentGold
        isOperator -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        isClear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        text == "=" -> MaterialTheme.colorScheme.onPrimary
        isClear -> MaterialTheme.colorScheme.error
        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "calc_button_press"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (!isOperator && !isClear) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .testTag("calc_key_$text"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun SpiceTradeCalculatorView() {
    var selectedSpiceIndex by remember { mutableStateOf(0) }
    val spicesList = listOf(
        SpiceProduct("Ceylon Cinnamon", 2200.0, "kg"),
        SpiceProduct("Ceylon Cardamom", 5400.0, "kg"),
        SpiceProduct("Ceylon Cloves", 1850.0, "kg"),
        SpiceProduct("Ceylon Nutmeg", 1450.0, "kg"),
        SpiceProduct("Ceylon Black Pepper", 1100.0, "kg")
    )

    var weightInput by remember { mutableStateOf("10") }
    var selectedUnitIndex by remember { mutableStateOf(0) } // 0 = kg, 1 = g
    var customPriceInput by remember { mutableStateOf("") }
    var marginScenario by remember { mutableStateOf(30f) } // target profit margin percentage

    val activeSpice = spicesList[selectedSpiceIndex]
    val activePrice = customPriceInput.toDoubleOrNull() ?: activeSpice.basePricePerKg

    val weightInKg = remember(weightInput, selectedUnitIndex) {
        val rawVal = weightInput.toDoubleOrNull() ?: 0.0
        if (selectedUnitIndex == 1) rawVal / 1000.0 else rawVal
    }

    // Calculations
    val costAmount = remember(weightInKg, activePrice) {
        weightInKg * activePrice
    }
    val suggestedWholesalePrice = remember(costAmount, marginScenario) {
        if (marginScenario >= 100f) costAmount else costAmount / (1.0 - (marginScenario / 100.0))
    }
    val targetProfit = remember(suggestedWholesalePrice, costAmount) {
        suggestedWholesalePrice - costAmount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spice Selection Box
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "1. SELECT SPICE VARIETY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spicesList.forEachIndexed { idx, spice ->
                    val isSelected = selectedSpiceIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) AccentGold else MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                            .clickable { selectedSpiceIndex = idx }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = spice.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ForestMedium else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Weight Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "2. ENTER BULK WEIGHT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "UNIT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp))
                            .background(if (selectedUnitIndex == 0) AccentGold else Color.Transparent)
                            .clickable { selectedUnitIndex = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("kg", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selectedUnitIndex == 0) ForestMedium else MaterialTheme.colorScheme.onSurface)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp))
                            .background(if (selectedUnitIndex == 1) AccentGold else Color.Transparent)
                            .clickable { selectedUnitIndex = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("g", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selectedUnitIndex == 1) ForestMedium else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Custom Cost Input Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "3. BASE SPICE COST PER KG (Rs.)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            OutlinedTextField(
                value = customPriceInput,
                onValueChange = { customPriceInput = it },
                placeholder = { Text("Using base: Rs. ${activeSpice.basePricePerKg}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Margin Target Slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4. TARGET PROFIT MARGIN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${marginScenario.toInt()}% Profit Margin",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
            }
            Slider(
                value = marginScenario,
                onValueChange = { marginScenario = it },
                valueRange = 5f..80f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        // Real-Time Calculation Results Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = ForestPrimary
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BULK ANALYSIS RESULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestLight,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(AccentGold, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CEYVANA PRESETS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestMedium
                        )
                    }
                }

                Divider(color = ForestMedium.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Base Cost Amount", fontSize = 11.sp, color = ForestLight.copy(alpha = 0.7f))
                        Text(String.format(Locale.US, "Rs. %,.2f", costAmount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gross Profit (${marginScenario.toInt()}%)", fontSize = 11.sp, color = ForestLight.copy(alpha = 0.7f))
                        Text(String.format(Locale.US, "+ Rs. %,.2f", targetProfit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SUGGESTED EXPORT PRICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                            Text("Include this price in Ceylon Spices Invoice", fontSize = 9.sp, color = ForestLight.copy(alpha = 0.6f))
                        }
                        Text(
                            text = String.format(Locale.US, "Rs. %,.2f", suggestedWholesalePrice),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Highly accurate mathematical expression parser with standard operator precedence
fun evaluateSimpleExpression(expr: String): String {
    val cleaned = expr.replace("×", "*").replace("÷", "/")
    if (cleaned.isBlank()) return "0"

    return try {
        class Parser(val str: String) {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    } else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val numStr = str.substring(startPos, this.pos)
                    x = numStr.toDoubleOrNull() ?: throw RuntimeException("Invalid number: $numStr")
                } else {
                    throw RuntimeException("Unexpected character: " + ch.toChar())
                }

                while (eat('%'.code)) {
                    x /= 100.0
                }

                return x
            }
        }

        val result = Parser(cleaned).parse()
        if (result.isInfinite() || result.isNaN()) {
            "Error"
        } else {
            if (result % 1 == 0.0) {
                result.toLong().toString()
            } else {
                val formatted = String.format(Locale.US, "%.8f", result)
                var idx = formatted.length - 1
                while (idx >= 0 && formatted[idx] == '0') {
                    idx--
                }
                if (idx >= 0 && formatted[idx] == '.') {
                    idx--
                }
                formatted.substring(0, idx + 1)
            }
        }
    } catch (e: ArithmeticException) {
        "Div by 0"
    } catch (e: Exception) {
        "Error"
    }
}

data class SpiceProduct(
    val name: String,
    val basePricePerKg: Double,
    val unit: String
)
