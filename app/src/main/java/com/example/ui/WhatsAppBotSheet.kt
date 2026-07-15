package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.InvoiceWithLineItems
import com.example.data.ProductEntity
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ForestLight
import com.example.ui.theme.ForestMedium
import com.example.ui.theme.ForestPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isMe: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "read" // "sent", "delivered", "read"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppBotSheet(
    onDismiss: () -> Unit,
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val products by viewModel.productsList.collectAsState()
    val invoices by viewModel.invoicesList.collectAsState()

    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "🌿 Welcome to *Ceyvana Spices Official Chatbot*!\n\nI am your automated virtual assistant. How can I help you manage your premium Ceylon spices trade today?",
                    isMe = false
                )
            )
        )
    }

    var textInput by remember { mutableStateOf("") }
    var isBotTyping by remember { mutableStateOf(false) }
    var isGeminiAgentMode by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    // Scroll to latest message on change
    LaunchedEffect(chatMessages.size, isBotTyping) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Smart reply generator
    fun handleIncomingUserMessage(messageText: String) {
        if (messageText.isBlank()) return

        // 1. Add user message
        chatMessages = chatMessages + ChatMessage(text = messageText, isMe = true)
        textInput = ""

        // 2. Simulate bot typing & thinking
        isBotTyping = true

        coroutineScope.launch {
            delay(1200) // Realistic reading / typing lag

            val cleanedText = messageText.trim().lowercase(Locale.US)
            val botResponse: String

            if (cleanedText.contains("price") || cleanedText.contains("catalog") || cleanedText.contains("spice") || cleanedText.contains("cinnamon")) {
                botResponse = generateProductsPriceListMessage(products)
            } else if (cleanedText.contains("invoice") || cleanedText.contains("bill") || cleanedText.contains("ledger") || cleanedText.contains("balance")) {
                botResponse = generateInvoicesLedgerMessage(invoices)
            } else if (cleanedText.contains("shipping") || cleanedText.contains("cargo") || cleanedText.contains("export") || cleanedText.contains("port")) {
                botResponse = "📦 *Ceyvana Logistics & Cargo Export Info*\n\n" +
                        "• *Port of Origin*: Colombo Port (SND), Sri Lanka.\n" +
                        "• *Certifications*: USDA Organic, Ceylon Spice Board Authentic Lion Logo, ISO 22000.\n" +
                        "• *Lead Time*: 7-14 business days from payment clearance.\n" +
                        "• *Tracking*: Full shipment bills of lading generated dynamically as PDF reports.\n\n" +
                        "🌿 _Tip: Use the 'Spice Trade' calculator in the home screen to optimize margin pricing!_"
            } else {
                // Try Gemini API if key is set & active
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (isGeminiAgentMode && apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && !apiKey.startsWith("YOUR_")) {
                    botResponse = try {
                        callGeminiApiForBot(messageText, products, invoices)
                    } catch (e: Exception) {
                        generateLocalFallbackResponse(messageText)
                    }
                } else {
                    botResponse = generateLocalFallbackResponse(messageText)
                }
            }

            // 3. Append bot response
            chatMessages = chatMessages + ChatMessage(text = botResponse, isMe = false)
            isBotTyping = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF0F1A15), // Deep Dark forest background matching Ceyvana
        dragHandle = { BottomSheetDefaults.DragHandle(color = AccentGold.copy(alpha = 0.5f)) },
        modifier = modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // WhatsApp Custom Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07120E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_ceyvana_logo),
                            contentDescription = "Ceyvana Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }

                    // Name & Online Status
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ceyvana Spice Bot",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Brand",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (isBotTyping) "typing..." else "Verified Business Account • Online",
                            fontSize = 10.sp,
                            color = if (isBotTyping) Color(0xFF00E676) else Color.LightGray.copy(alpha = 0.8f),
                            fontWeight = if (isBotTyping) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // AI Engine Mode Indicator Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val hasApiKey = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    IconButton(
                        onClick = { isGeminiAgentMode = !isGeminiAgentMode },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isGeminiAgentMode) ForestPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isGeminiAgentMode) Icons.Default.AutoAwesome else Icons.Default.Block,
                            contentDescription = "Toggle Gemini AI",
                            tint = if (isGeminiAgentMode) AccentGold else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Chat Messages Stream area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF08100C)) // Even darker slate canvas
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Encryption Notice Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F27).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Messages are end-to-end simulated & fully secure.",
                                        fontSize = 10.sp,
                                        color = Color.LightGray.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Messages List
                    items(chatMessages) { message ->
                        ChatBubble(message = message)
                    }

                    // Typing Indicator
                    if (isBotTyping) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2C24)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Ceyvana Bot is typing",
                                            fontSize = 11.sp,
                                            color = Color.LightGray.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        // Flashing Dot Animation
                                        val infiniteTransition = rememberInfiniteTransition(label = "dots")
                                        val dotAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.2f,
                                            targetValue = 1.0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "dot_alpha"
                                        )
                                        Text(
                                            text = "...",
                                            fontSize = 14.sp,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.alpha(dotAlpha)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Quick Reply suggestions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07120E))
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickReplyChip(
                        label = "🌿 Current Pricing",
                        onClick = { handleIncomingUserMessage("Show me current spice prices") }
                    )
                    QuickReplyChip(
                        label = "📄 Active Invoices",
                        onClick = { handleIncomingUserMessage("What is my latest invoice status?") }
                    )
                    QuickReplyChip(
                        label = "📦 Cargo Export Log",
                        onClick = { handleIncomingUserMessage("Tell me about shipping port logistics") }
                    )
                    QuickReplyChip(
                        label = "✨ Spice Board Info",
                        onClick = { handleIncomingUserMessage("What certifications does Ceyvana hold?") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text Box
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "Message Ceyvana WhatsApp...",
                                color = Color.LightGray.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                handleIncomingUserMessage(textInput)
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF13221C),
                            unfocusedContainerColor = Color(0xFF13221C),
                            focusedBorderColor = AccentGold.copy(alpha = 0.8f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("whatsapp_input_field"),
                        trailingIcon = {
                            IconButton(onClick = { /* Simulated File Attachment */ }) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach file",
                                    tint = AccentGold.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )

                    // Send Floating Circle Button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                handleIncomingUserMessage(textInput)
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (textInput.isNotBlank()) Color(0xFF00E676) else Color(0xFF1E2C24)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .testTag("whatsapp_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (textInput.isNotBlank()) Color.Black else Color.LightGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    val timeStr = remember(message.timestamp) { formatter.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMe) 16.dp else 2.dp,
                bottomEnd = if (message.isMe) 2.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isMe) Color(0xFF104A30) else Color(0xFF1B2A23)
            ),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .border(
                    width = 0.5.dp,
                    color = if (message.isMe) AccentGold.copy(alpha = 0.3f) else ForestMedium.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isMe) 16.dp else 2.dp,
                        bottomEnd = if (message.isMe) 2.dp else 16.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Parse rudimentary bold text formatting like *text* inside bubble
                FormattedMessageText(
                    text = message.text,
                    textColor = if (message.isMe) Color.White else Color.LightGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time & Tick Row
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )
                    if (message.isMe) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMessageText(text: String, textColor: Color) {
    // Splits text by asterisks to apply Bold on alternating substrings
    val parts = remember(text) { text.split("*") }

    Column {
        androidx.compose.ui.text.buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                val isBold = index % 2 != 0
                if (isBold) {
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold, color = AccentGold)) {
                        append(part)
                    }
                } else {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = textColor)) {
                        append(part)
                    }
                }
            }
        }.let { annotatedString ->
            Text(
                text = annotatedString,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun QuickReplyChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF13221C))
            .border(1.dp, ForestMedium.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// Local smart response generators
fun generateProductsPriceListMessage(products: List<ProductEntity>): String {
    val sb = StringBuilder()
    sb.append("🌿 *Ceyvana Spice Board Pricing* 🌿\n\n")
    sb.append("We offer certified export-grade premium Ceylon spices at the following rates:\n\n")
    if (products.isEmpty()) {
        sb.append("• *Premium Cinnamon*: Rs. 15.00 / g\n")
        sb.append("• *Premium Cardamom*: Rs. 35.00 / g\n")
        sb.append("• *Black Pepper*: Rs. 8.00 / g\n")
    } else {
        products.take(6).forEach { product ->
            sb.append("• *Ceyvana ${product.name}*: Rs. ${String.format(Locale.US, "%,.2f", product.pricePerGram)} per gram\n")
        }
    }
    sb.append("\nWholesale discounts are auto-calculated at checkout in the Ceyvana App.")
    return sb.toString()
}

fun generateInvoicesLedgerMessage(invoices: List<InvoiceWithLineItems>): String {
    val sb = StringBuilder()
    sb.append("📄 *Ceyvana Active Invoice Ledger* 📄\n\n")
    if (invoices.isEmpty()) {
        sb.append("You do not have any active invoices registered. Tap 'New Invoice' in the app to draft one immediately!")
    } else {
        sb.append("We retrieved the *${invoices.size} most recent invoices* from your local device database:\n\n")
        invoices.take(3).forEach { invoiceItem ->
            val invoice = invoiceItem.invoice
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val dateStr = formatter.format(Date(invoice.invoiceDate))
            sb.append("• *Invoice #${invoice.invoiceNumber}*\n")
            sb.append("  Client: *${invoice.customerName}*\n")
            sb.append("  Amount: Rs. *${String.format(Locale.US, "%,.2f", invoiceItem.grandTotal)}*\n")
            sb.append("  Date: $dateStr\n\n")
        }
        sb.append("Open the Ceyvana app invoices screen to view item logs, sync with Google Drive, or generate official high-contrast PDF receipts.")
    }
    return sb.toString()
}

fun generateLocalFallbackResponse(query: String): String {
    return "🌿 *Ceyvana Spices Help Desk* 🌿\n\n" +
            "Thank you for your message! Our virtual representative is offline, but I can assist with automated tools:\n\n" +
            "• Type *'prices'* to get our latest live catalog pricing.\n" +
            "• Type *'invoice'* to review your draft invoice balances.\n" +
            "• Type *'shipping'* to look up Colombo cargo port schedules.\n\n" +
            "For full-featured operations, please use the navigation tabs below inside the app!"
}

// Call Gemini API using Direct REST API as defined in the skill!
suspend fun callGeminiApiForBot(
    query: String,
    products: List<ProductEntity>,
    invoices: List<InvoiceWithLineItems>
): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    // Construct the context prompt with local products and invoice info so that the bot gives accurate, smart responses!
    val productContext = products.joinToString { "${it.name} at Rs.${it.pricePerGram}/g" }
    val invoiceContext = if (invoices.isEmpty()) "No active invoices" else invoices.joinToString { "Invoice #${it.invoice.invoiceNumber} for client ${it.invoice.customerName} totaling Rs.${it.grandTotal}" }

    val systemInstruction = "You are the official Ceyvana WhatsApp AI assistant. Ceyvana is a premium Ceylon spices brand specializing in USDA organic certified spices like Cinnamon, Cardamom, and Pepper. " +
            "We have these products in our catalog: $productContext. " +
            "Our active invoices are: $invoiceContext. " +
            "Keep your response strictly under 3-4 sentences. Use WhatsApp bold formatting like *text* to highlight important terms. Always remain polite, professional, and helpful."

    val jsonRequest = JSONObject().apply {
        put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", query)
                    })
                })
            })
        })
        put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", systemInstruction)
                })
            })
        })
    }

    val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url(endpoint)
        .post(requestBody)
        .build()

    val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    try {
        val response = client.newCall(request).execute()
        val responseBodyString = response.body?.string() ?: ""
        if (response.isSuccessful && responseBodyString.isNotEmpty()) {
            val jsonResponse = JSONObject(responseBodyString)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val firstPart = parts.getJSONObject(0)
            firstPart.getString("text")
        } else {
            generateLocalFallbackResponse(query)
        }
    } catch (e: Exception) {
        generateLocalFallbackResponse(query)
    }
}
