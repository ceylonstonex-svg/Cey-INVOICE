package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.example.sharePdfFile
import com.example.sharePdfViaWhatsApp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InvoiceEntity
import com.example.data.InvoiceWithLineItems
import com.example.data.LineItemEntity
import com.example.data.ProductEntity
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ForestLight
import com.example.ui.theme.ForestMedium
import com.example.ui.theme.ForestPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Local UI data classes for POS Cart Items
data class PosCartItem(
    val product: ProductEntity?,
    val customName: String = "",
    val pricePerGram: Double,
    val selectedWeightGrams: Double, // Weight in grams (e.g. 100g, 250g, 1000g)
    val quantity: Int = 1,
    val unit: String = "g"
) {
    val itemName: String
        get() = product?.name ?: customName

    val unitPrice: Double
        get() = pricePerGram * selectedWeightGrams

    val totalPrice: Double
        get() = unitPrice * quantity
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(viewModel: InvoiceViewModel) {
    val products by viewModel.productsList.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Cart and order details
    val cartItems = remember { mutableStateListOf<PosCartItem>() }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var deliveryChargeInput by remember { mutableStateOf("") }
    var taxRate by remember { mutableStateOf(0.08) } // 8% Default VAT/Tax

    // Product search/filter
    var searchQuery by remember { mutableStateOf("") }

    // Dialog & Checkout Success State
    var showProductWeightDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var checkoutSuccessInvoice by remember { mutableStateOf<InvoiceWithLineItems?>(null) }

    // Dynamic metrics
    val subTotal = remember(cartItems) {
        cartItems.sumOf { it.totalPrice }
    }
    val discount = remember(discountInput) {
        discountInput.toDoubleOrNull() ?: 0.0
    }
    val deliveryCharge = remember(deliveryChargeInput) {
        deliveryChargeInput.toDoubleOrNull() ?: 0.0
    }
    val taxAmount = remember(subTotal, taxRate) {
        Math.round(subTotal * taxRate * 100.0) / 100.0
    }
    val grandTotal = remember(subTotal, discount, deliveryCharge, taxAmount) {
        Math.round((subTotal - discount + deliveryCharge + taxAmount) * 100.0) / 100.0
    }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Grid layout (Product Grid left/top, Cart pane right/bottom)
        // Adaptive Layout: Side-by-side for medium/large, column for small screens
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 680.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Catalog selection
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PosHeaderSection(
                            onAddCustomItem = { showCustomItemDialog = true },
                            onAddNewProduct = { showAddProductDialog = true }
                        )

                        PosSearchAndFilters(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )

                        PosProductGrid(
                            products = filteredProducts,
                            onProductSelected = { showProductWeightDialog = it }
                        )
                    }

                    // Right Column: Cart detail & Checkout
                    Card(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CartHeaderSection(cartSize = cartItems.size) {
                                cartItems.clear()
                                customerName = ""
                                customerPhone = ""
                                discountInput = ""
                                deliveryChargeInput = ""
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Cart list
                            Box(modifier = Modifier.weight(1f)) {
                                if (cartItems.isEmpty()) {
                                    EmptyCartView()
                                } else {
                                    CartItemsList(
                                        cartItems = cartItems,
                                        onIncreaseQty = { index ->
                                            cartItems[index] = cartItems[index].copy(quantity = cartItems[index].quantity + 1)
                                        },
                                        onDecreaseQty = { index ->
                                            if (cartItems[index].quantity > 1) {
                                                cartItems[index] = cartItems[index].copy(quantity = cartItems[index].quantity - 1)
                                            } else {
                                                cartItems.removeAt(index)
                                            }
                                        },
                                        onRemoveItem = { index ->
                                            cartItems.removeAt(index)
                                        }
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Customer Info
                            CustomerInputFields(
                                name = customerName,
                                onNameChange = { customerName = it },
                                phone = customerPhone,
                                onPhoneChange = { customerPhone = it },
                                viewModel = viewModel
                            )

                            // Cost summary & checkout
                            CheckoutBillSummary(
                                subTotal = subTotal,
                                discountInput = discountInput,
                                onDiscountChange = { discountInput = it },
                                deliveryInput = deliveryChargeInput,
                                onDeliveryChange = { deliveryChargeInput = it },
                                taxAmount = taxAmount,
                                grandTotal = grandTotal,
                                cartNotEmpty = cartItems.isNotEmpty(),
                                onCheckout = {
                                    scope.launch {
                                        val successInvoice = checkoutOrder(
                                            viewModel = viewModel,
                                            cartItems = cartItems,
                                            customerName = customerName,
                                            customerPhone = customerPhone,
                                            subTotal = subTotal,
                                            discount = discount,
                                            delivery = deliveryCharge,
                                            taxRate = taxRate,
                                            context = context
                                        )
                                        if (successInvoice != null) {
                                            checkoutSuccessInvoice = successInvoice
                                            // Reset POS cart state
                                            cartItems.clear()
                                            customerName = ""
                                            customerPhone = ""
                                            discountInput = ""
                                            deliveryChargeInput = ""
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Compact Layout for Phones (Tabs/Column switcher)
                var activePane by remember { mutableStateOf(0) } // 0 = Catalog, 1 = Cart & Bill

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Pane Tab Indicator
                    TabRow(
                        selectedTabIndex = activePane,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = activePane == 0,
                            onClick = { activePane = 0 },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("තෝරන්න (Catalog)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = activePane == 1,
                            onClick = { activePane = 1 },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (cartItems.isNotEmpty()) {
                                                Badge(containerColor = AccentGold) {
                                                    Text(cartItems.size.toString(), color = ForestMedium)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ගෙවීම් (Cart)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    if (activePane == 0) {
                        // Product Catalog View
                        PosHeaderSection(
                            onAddCustomItem = { showCustomItemDialog = true },
                            onAddNewProduct = { showAddProductDialog = true }
                        )

                        PosSearchAndFilters(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            PosProductGrid(
                                products = filteredProducts,
                                onProductSelected = { showProductWeightDialog = it }
                            )
                        }

                        // Bottom sticky bar for easy cart jumping
                        if (cartItems.isNotEmpty()) {
                            Button(
                                onClick = { activePane = 1 },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("භාණ්ඩ ${cartItems.sumOf { it.quantity }} ක් එකතු කර ඇත", fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Rs. ${String.format(Locale.US, "%,.2f", grandTotal)}", fontWeight = FontWeight.ExtraBold, color = AccentGold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        // Cart View for checkout
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CartHeaderSection(cartSize = cartItems.size) {
                                    cartItems.clear()
                                    customerName = ""
                                    customerPhone = ""
                                    discountInput = ""
                                    deliveryChargeInput = ""
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (cartItems.isEmpty()) {
                                        EmptyCartView()
                                    } else {
                                        CartItemsList(
                                            cartItems = cartItems,
                                            onIncreaseQty = { index ->
                                                cartItems[index] = cartItems[index].copy(quantity = cartItems[index].quantity + 1)
                                            },
                                            onDecreaseQty = { index ->
                                                if (cartItems[index].quantity > 1) {
                                                    cartItems[index] = cartItems[index].copy(quantity = cartItems[index].quantity - 1)
                                                } else {
                                                    cartItems.removeAt(index)
                                                }
                                            },
                                            onRemoveItem = { index ->
                                                cartItems.removeAt(index)
                                            }
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                CustomerInputFields(
                                    name = customerName,
                                    onNameChange = { customerName = it },
                                    phone = customerPhone,
                                    onPhoneChange = { customerPhone = it }, viewModel = viewModel
                                )

                                CheckoutBillSummary(
                                    subTotal = subTotal,
                                    discountInput = discountInput,
                                    onDiscountChange = { discountInput = it },
                                    deliveryInput = deliveryChargeInput,
                                    onDeliveryChange = { deliveryChargeInput = it },
                                    taxAmount = taxAmount,
                                    grandTotal = grandTotal,
                                    cartNotEmpty = cartItems.isNotEmpty(),
                                    onCheckout = {
                                        scope.launch {
                                            val successInvoice = checkoutOrder(
                                                viewModel = viewModel,
                                                cartItems = cartItems,
                                                customerName = customerName,
                                                customerPhone = customerPhone,
                                                subTotal = subTotal,
                                                discount = discount,
                                                delivery = deliveryCharge,
                                                taxRate = taxRate,
                                                context = context
                                            )
                                            if (successInvoice != null) {
                                                checkoutSuccessInvoice = successInvoice
                                                // Reset POS state
                                                cartItems.clear()
                                                customerName = ""
                                                customerPhone = ""
                                                discountInput = ""
                                                deliveryChargeInput = ""
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Product packaging size picker dialog
        showProductWeightDialog?.let { product ->
            ProductPackagingDialog(
                product = product,
                onDismiss = { showProductWeightDialog = null },
                onConfirm = { itemsList ->
                    for ((weightGrams, qty) in itemsList) {
                        val existingIdx = cartItems.indexOfFirst { it.product?.id == product.id && it.selectedWeightGrams == weightGrams }
                        if (existingIdx != -1) {
                            cartItems[existingIdx] = cartItems[existingIdx].copy(quantity = cartItems[existingIdx].quantity + qty)
                        } else {
                            cartItems.add(
                                PosCartItem(
                                    product = product,
                                    pricePerGram = product.pricePerGram,
                                    selectedWeightGrams = weightGrams,
                                    quantity = qty
                                )
                            )
                        }
                    }
                    showProductWeightDialog = null
                    Toast.makeText(context, "${product.name} Added to Cart", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Add custom line item dialog
        if (showCustomItemDialog) {
            CustomLineItemDialog(
                onDismiss = { showCustomItemDialog = false },
                onConfirm = { name, price, qty ->
                    cartItems.add(
                        PosCartItem(
                            product = null,
                            customName = name,
                            pricePerGram = price,
                            selectedWeightGrams = 1.0, // Ad-hoc price is simple multiplier
                            quantity = qty,
                            unit = "item"
                        )
                    )
                    showCustomItemDialog = false
                    Toast.makeText(context, "Added ad-hoc: $name", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Add new product catalog dialog
        if (showAddProductDialog) {
            AddProductDialog(
                onDismiss = { showAddProductDialog = false },
                onConfirm = { name, price ->
                    viewModel.saveProduct(name = name, pricePerGram = price)
                    showAddProductDialog = false
                }
            )
        }

        // Checkout Success Screen Dialog with Receipts & Sharing options
        checkoutSuccessInvoice?.let { invoiceWithLineItems ->
            CheckoutSuccessDialog(
                invoiceWithItems = invoiceWithLineItems,
                onDismiss = { checkoutSuccessInvoice = null },
                pdfService = viewModel.pdfService,
                context = context
            )
        }
    }
}

// POS Header Title section
@Composable
fun PosHeaderSection(onAddCustomItem: () -> Unit, onAddNewProduct: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = "Point of Sale (POS)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Ceyvana සජීවී විකුණුම් පද්ධතිය",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onAddNewProduct,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                border = BorderStroke(1.dp, AccentGold)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("නව නිෂ්පාදන (Add)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onAddCustomItem,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestMedium),
                border = BorderStroke(1.dp, ForestMedium)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("අලුත් අයිතම (Custom)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Search & Catalog Filter Bar
@Composable
fun PosSearchAndFilters(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("කුළුබඩු සොයන්න (Search Spices...)", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestMedium) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGold,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
    )
}

// Grid of products available in the SQLite catalog
@Composable
fun PosProductGrid(
    products: List<ProductEntity>,
    onProductSelected: (ProductEntity) -> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))
                Text("නිෂ්පාදන කිසිවක් නැත (No products)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductSelected(product) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ForestLight.copy(alpha = 0.15f))
                                .align(Alignment.Start),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = ForestMedium,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Rs. ${product.pricePerGram}/g",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGold
                        )
                    }
                }
            }
        }
    }
}

// Cart Header Row
@Composable
fun CartHeaderSection(cartSize: Int, onClearCart: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ForestMedium)
            Text(
                text = "භාණ්ඩ එකතුව (${cartSize})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (cartSize > 0) {
            IconButton(onClick = onClearCart) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Empty Cart visual state
@Composable
fun EmptyCartView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCartCheckout,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "භාණ්ඩ එකතුව හිස්ව ඇත",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "විකුණුම් ආරම්භ කිරීමට නිෂ්පාදනයක් තෝරන්න.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// List of selected cart items
@Composable
fun CartItemsList(
    cartItems: List<PosCartItem>,
    onIncreaseQty: (Int) -> Unit,
    onDecreaseQty: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cartItems.size) { index ->
            val item = cartItems[index]
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.itemName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (item.product != null) "${item.selectedWeightGrams.toInt()}g" else "Custom Unit",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rs. ${String.format(Locale.US, "%,.2f", item.totalPrice)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestMedium
                            )
                        }
                    }

                    // Quantity buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { onDecreaseQty(index) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = item.quantity.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { onIncreaseQty(index) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }

                    IconButton(
                        onClick = { onRemoveItem(index) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Customer details inputs
@Composable
fun CustomerInputFields(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    viewModel: InvoiceViewModel? = null
) {
    var showCrmSelector by remember { mutableStateOf(false) }
    val customers by if (viewModel != null) viewModel.customersList.collectAsState() else remember { mutableStateOf(emptyList()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("පාරිභෝගික තොරතුරු (Customer Details)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestMedium)
            
            if (viewModel != null && customers.isNotEmpty()) {
                TextButton(
                    onClick = { showCrmSelector = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = ForestPrimary)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CRM Selector", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("නම (Customer Name)", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestMedium,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                placeholder = { Text("දුරකථනය (Phone)", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestMedium,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
            )
        }
    }

    if (showCrmSelector) {
        AlertDialog(
            onDismissRequest = { showCrmSelector = false },
            title = { Text("Select Customer from CRM", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customers) { cust ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNameChange(cust.name)
                                        onPhoneChange(cust.phone)
                                        showCrmSelector = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "📞 ${cust.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = AccentGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${cust.loyaltyPoints} PTS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCrmSelector = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Checkout and Cost summary breakdown
@Composable
fun CheckoutBillSummary(
    subTotal: Double,
    discountInput: String,
    onDiscountChange: (String) -> Unit,
    deliveryInput: String,
    onDeliveryChange: (String) -> Unit,
    taxAmount: Double,
    grandTotal: Double,
    cartNotEmpty: Boolean,
    onCheckout: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Quick Discount and Service / Delivery inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = discountInput,
                onValueChange = onDiscountChange,
                placeholder = { Text("වට්ටම් (Discount) Rs.", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestMedium,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
            )
            OutlinedTextField(
                value = deliveryInput,
                onValueChange = onDeliveryChange,
                placeholder = { Text("සේවා ගාස්තු (Service) Rs.", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestMedium,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
            )
        }

        // Subtotal, VAT and Grand Total rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rs. ${String.format(Locale.US, "%,.2f", subTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (taxAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tax / VAT (8%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Rs. ${String.format(Locale.US, "%,.2f", taxAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GRAND TOTAL", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ForestPrimary)
                Text(
                    text = "Rs. ${String.format(Locale.US, "%,.2f", grandTotal)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentGold
                )
            }
        }

        Button(
            onClick = onCheckout,
            enabled = cartNotEmpty,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("pos_checkout_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                Text("ගෙවීම් සම්පූර්ණ කරන්න (CHECKOUT)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Dialog to select packaging weight & custom counts of products
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductPackagingDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<Double, Int>>) -> Unit
) {
    // Keep track of quantities for each weight variant
    val quantities = remember { mutableStateMapOf<Double, Int>() }
    
    // Default weights
    val presetWeights = listOf(25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
    
    // Custom weight state
    var customWeightStr by remember { mutableStateOf("") }
    var customWeightQty by remember { mutableStateOf(0) }
    
    val customWeight = customWeightStr.toDoubleOrNull() ?: 0.0
    val isCustomWeightValid = customWeight > 0.0

    // Compute grand totals
    val totalItemsCount = presetWeights.sumOf { quantities[it] ?: 0 } + (if (isCustomWeightValid) customWeightQty else 0)
    
    val totalAmount = presetWeights.sumOf { wt ->
        val qty = quantities[wt] ?: 0
        qty * wt * product.pricePerGram
    } + (if (isCustomWeightValid) customWeightQty * customWeight * product.pricePerGram else 0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = ForestMedium,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = product.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ග්‍රෑමයක මිල (Price/g): Rs. ${String.format(Locale.US, "%.2f", product.pricePerGram)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ප්‍රභේදය (Grams)",
                        modifier = Modifier.weight(1.2f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "මිල (Price)",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "ප්‍රමාණය (Qty)",
                        modifier = Modifier.weight(1.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Table Rows for Preset Weights
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(presetWeights) { wt ->
                        val qty = quantities[wt] ?: 0
                        val label = if (wt >= 1000.0) "${(wt/1000.0).toInt()}kg" else "${wt.toInt()}g"
                        val price = wt * product.pricePerGram

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (qty > 0) ForestPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    color = if (qty > 0) ForestPrimary.copy(alpha = 0.05f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Grams
                            Row(
                                modifier = Modifier.weight(1.2f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (qty > 0) AccentGold else MaterialTheme.colorScheme.outlineVariant, shape = CircleShape)
                                )
                                Text(
                                    text = label,
                                    fontWeight = if (qty > 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Price
                            Text(
                                text = "Rs. ${String.format(Locale.US, "%,.2f", price)}",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = if (qty > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (qty > 0) ForestMedium else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            )

                            // Quantity Selector
                            Row(
                                modifier = Modifier.weight(1.5f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (qty > 0) {
                                            quantities[wt] = qty - 1
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (qty > 0) ForestPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = qty.toString(),
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (qty > 0) ForestPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        quantities[wt] = qty + 1
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = ForestPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom weight row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (customWeightQty > 0) AccentGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    color = if (customWeightQty > 0) AccentGold.copy(alpha = 0.05f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Weight input field
                            Column(modifier = Modifier.weight(1.2f)) {
                                OutlinedTextField(
                                    value = customWeightStr,
                                    onValueChange = { customWeightStr = it },
                                    placeholder = { Text("වෙනත් (Custom)", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                            }

                            // Computed custom price
                            val customUnitPrice = customWeight * product.pricePerGram
                            Text(
                                text = if (isCustomWeightValid) "Rs. ${String.format(Locale.US, "%,.2f", customUnitPrice)}" else "Rs. 0.00",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = if (customWeightQty > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (customWeightQty > 0) ForestMedium else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            )

                            // Custom quantity selector
                            Row(
                                modifier = Modifier.weight(1.5f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (customWeightQty > 0) {
                                            customWeightQty--
                                        }
                                    },
                                    enabled = isCustomWeightValid,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Custom",
                                        tint = if (customWeightQty > 0) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = customWeightQty.toString(),
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customWeightQty > 0) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        customWeightQty++
                                    },
                                    enabled = isCustomWeightValid,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase Custom",
                                        tint = if (isCustomWeightValid) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cumulative Summary Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ForestPrimary.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, ForestPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("මුළු අයිතම ගණන (Total Selected):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalItemsCount pack(s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("මුළු මුදල (Total Price):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Rs. ${String.format(Locale.US, "%,.2f", totalAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentGold
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("අවලංගු කරන්න (Cancel)", color = ForestMedium)
                    }
                    Button(
                        onClick = {
                            // Extract selected presets and custom weight
                            val selectedItems = mutableListOf<Pair<Double, Int>>()
                            presetWeights.forEach { wt ->
                                val q = quantities[wt] ?: 0
                                if (q > 0) {
                                    selectedItems.add(wt to q)
                                }
                            }
                            if (isCustomWeightValid && customWeightQty > 0) {
                                selectedItems.add(customWeight to customWeightQty)
                            }
                            if (selectedItems.isNotEmpty()) {
                                onConfirm(selectedItems)
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f),
                        enabled = totalItemsCount > 0
                    ) {
                        Text("එකතු කරන්න (Add to Basket)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog for adding custom line item
@Composable
fun CustomLineItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ad-hoc Custom Item",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name / Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold)
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price (Rs.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold)
                )

                // Quantity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantity:", fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        Text(text = quantity.toString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && price > 0.0) {
                                onConfirm(name, price, quantity)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Add Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Checkout success overlay with quick sharing
@Composable
fun CheckoutSuccessDialog(
    invoiceWithItems: InvoiceWithLineItems,
    pdfService: com.example.data.PdfService,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Success celebration circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "ගනුදෙනුව සාර්ථකයි!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "POS Billing ledger updated successfully.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Brief invoice metadata summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Invoice ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("#${invoiceWithItems.invoice.invoiceNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Customer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(invoiceWithItems.invoice.customerName.ifBlank { "Walk-In Customer" }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paid Amount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Rs. ${String.format(Locale.US, "%,.2f", invoiceWithItems.grandTotal)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestMedium
                        )
                    }
                }

                // Sharing action options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = pdfService.generateInvoicePdf(context, invoiceWithItems)
                            sharePdfViaWhatsApp(context, file)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("WhatsApp PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val file = pdfService.generateInvoicePdf(context, invoiceWithItems)
                            sharePdfFile(context, file)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestMedium),
                        border = BorderStroke(1.dp, ForestMedium)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Print Receipt / PDF Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("අලුත් විකිණීමක් (New Sale)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Transaction checkout process linking POS screen directly with local Room Billing Ledger
suspend fun checkoutOrder(
    viewModel: InvoiceViewModel,
    cartItems: List<PosCartItem>,
    customerName: String,
    customerPhone: String,
    subTotal: Double,
    discount: Double,
    delivery: Double,
    taxRate: Double,
    context: android.content.Context
): InvoiceWithLineItems? {
    if (cartItems.isEmpty()) return null

    try {
        val invoiceNum = "POS-${System.currentTimeMillis().toString().takeLast(6)}"
        val invoiceEntity = InvoiceEntity(
            invoiceNumber = invoiceNum,
            invoiceDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis(), // Paid instantly on POS
            customerName = customerName.ifBlank { "Walk-In Client" },
            customerEmail = "",
            customerPhone = customerPhone,
            subTotal = subTotal,
            discount = discount,
            deliveryCharge = delivery,
            taxRate = taxRate
        )

        // Convert POS items to standard billing entities
        val billingLineItems = cartItems.mapIndexed { idx, item ->
            val desc = if (item.product != null) {
                "Ceylon ${item.itemName} (${item.selectedWeightGrams.toInt()}g pack)"
            } else {
                item.itemName
            }
            LineItemEntity(
                invoiceId = 0,
                description = desc,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unit = item.unit
            )
        }

        // Save directly using the Room Repository
        val invoiceId = viewModel.saveInvoiceForPos(invoiceEntity, billingLineItems)

        Toast.makeText(context, "Order Saved Successfully", Toast.LENGTH_SHORT).show()

        val savedInvoice = InvoiceWithLineItems(
            invoice = invoiceEntity.copy(id = invoiceId),
            lineItems = billingLineItems.map { it.copy(invoiceId = invoiceId) }
        )
        return savedInvoice
    } catch (e: Exception) {
        Toast.makeText(context, "POS Checkout Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var pricePerGramStr by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var isPriceError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = ForestMedium,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "නව නිෂ්පාදනයක් ඇතුළත් කිරීම",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Add new product to the central spice database catalog.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = productName,
                    onValueChange = { 
                        productName = it
                        isNameError = false 
                    },
                    label = { Text("නිෂ්පාදනයේ නම (Product Name)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Chili Powder") },
                    singleLine = true,
                    isError = isNameError,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestMedium,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isNameError) {
                    Text(
                        text = "කරුණාකර නිෂ්පාදන නම ඇතුළත් කරන්න",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                OutlinedTextField(
                    value = pricePerGramStr,
                    onValueChange = { 
                        pricePerGramStr = it
                        isPriceError = false 
                    },
                    label = { Text("ග්‍රෑමයක මිල (Price per gram - Rs.)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 12.5") },
                    singleLine = true,
                    isError = isPriceError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestMedium,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isPriceError) {
                    Text(
                        text = "කරුණාකර වලංගු මිලක් ඇතුළත් කරන්න",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("අවලංගු කරන්න (Cancel)", color = ForestMedium)
                    }

                    Button(
                        onClick = {
                            val nameTrimmed = productName.trim()
                            val priceDouble = pricePerGramStr.toDoubleOrNull()
                            if (nameTrimmed.isEmpty()) {
                                isNameError = true
                            }
                            if (priceDouble == null || priceDouble <= 0.0) {
                                isPriceError = true
                            }

                            if (nameTrimmed.isNotEmpty() && priceDouble != null && priceDouble > 0.0) {
                                onConfirm(nameTrimmed, priceDouble)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("සුරකින්න (Save)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
