package com.example.stockit.ui.screens.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TradingDialog(
    isVisible: Boolean,
    isBuying: Boolean,
    stockSymbol: String,
    stockPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, pricePerShare: Double?) -> Unit,
    onCheckAffordability: ((quantity: Int, pricePerShare: Double?) -> Unit)? = null,
    isLoading: Boolean = false,
    affordabilityResult: AffordabilityResult? = null,
    isAuthenticated: Boolean = true
) {
    if (!isVisible) return
    
    var quantity by remember { mutableStateOf("") }
    var useMarketPrice by remember { mutableStateOf(true) }
    var customPrice by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf("") }
    var priceError by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    // Calculate total cost
    val totalCost = remember(quantity, useMarketPrice, customPrice) {
        val qty = quantity.toIntOrNull() ?: 0
        val price = if (useMarketPrice) stockPrice else (customPrice.toDoubleOrNull() ?: stockPrice)
        qty * price
    }
    
    // Validation
    val isValidQuantity = quantity.toIntOrNull()?.let { it > 0 } == true
    val isValidPrice = useMarketPrice || customPrice.toDoubleOrNull()?.let { it > 0 } == true
    val isFormValid = isValidQuantity && isValidPrice && isAuthenticated
    
    // Auto-check affordability when buying
    LaunchedEffect(quantity, useMarketPrice, customPrice) {
        if (isBuying && isValidQuantity && isValidPrice && onCheckAffordability != null) {
            delay(500) // Debounce
            val qty = quantity.toIntOrNull() ?: 0
            val price = if (useMarketPrice) null else customPrice.toDoubleOrNull()
            onCheckAffordability(qty, price)
        }
    }
    
    // Validate inputs
    LaunchedEffect(quantity) {
        quantityError = when {
            quantity.isEmpty() -> ""
            quantity.toIntOrNull() == null -> "Please enter a valid number"
            quantity.toIntOrNull()!! <= 0 -> "Quantity must be greater than 0"
            quantity.toIntOrNull()!! > 10000 -> "Maximum quantity is 10,000 shares"
            else -> ""
        }
    }
    
    LaunchedEffect(customPrice, useMarketPrice) {
        if (!useMarketPrice) {
            priceError = when {
                customPrice.isEmpty() -> "Please enter a price"
                customPrice.toDoubleOrNull() == null -> "Please enter a valid price"
                customPrice.toDoubleOrNull()!! <= 0 -> "Price must be greater than 0"
                customPrice.toDoubleOrNull()!! > 1000000 -> "Maximum price is ₹10,00,000"
                else -> ""
            }
        } else {
            priceError = ""
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with consistent styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBuying) "Buy $stockSymbol" else "Sell $stockSymbol",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFF475569),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Authentication Check with consistent dark styling
                if (!isAuthenticated) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Please sign in to trade stocks",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Current Price with consistent styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF0F172A).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Price:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹${String.format("%.2f", stockPrice)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Quantity Input with consistent styling
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            quantity = it
                        }
                    },
                    label = { Text("Quantity", color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = quantityError.isNotEmpty(),
                    supportingText = if (quantityError.isNotEmpty()) {
                        { Text(quantityError, color = Color(0xFFEF4444)) }
                    } else null,
                    enabled = isAuthenticated,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        disabledBorderColor = Color(0xFF334155),
                        disabledTextColor = Color(0xFF64748B)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Price Type Selection with consistent styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useMarketPrice,
                        onCheckedChange = { useMarketPrice = it },
                        enabled = isAuthenticated,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6366F1),
                            uncheckedColor = Color(0xFF475569),
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = "Use Market Price",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAuthenticated) Color.White else Color(0xFF64748B)
                    )
                }
                
                if (!useMarketPrice) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Custom Price", color = Color(0xFF94A3B8)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("₹", color = Color.White) },
                        isError = priceError.isNotEmpty(),
                        supportingText = if (priceError.isNotEmpty()) {
                            { Text(priceError, color = Color(0xFFEF4444)) }
                        } else null,
                        enabled = isAuthenticated,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            disabledBorderColor = Color(0xFF334155),
                            disabledTextColor = Color(0xFF64748B)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Total Cost Card with consistent styling
                if (isValidQuantity && isValidPrice) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total ${if (isBuying) "Cost" else "Value"}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", totalCost)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            // Affordability Check Result
                            affordabilityResult?.let { result ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (result.canAfford) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (result.canAfford) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (result.canAfford) {
                                            "You can afford this purchase"
                                        } else {
                                            "Insufficient balance (Available: ₹${String.format("%.2f", result.availableBalance)})"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (result.canAfford) Color(0xFF10B981) else Color(0xFFEF4444),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons with consistent styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF475569),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    // Confirm Button
                    Button(
                        onClick = {
                            val qty = quantity.toIntOrNull() ?: 0
                            val price = if (useMarketPrice) null else customPrice.toDoubleOrNull()
                            if (isFormValid && (affordabilityResult?.canAfford != false || !isBuying)) {
                                onConfirm(qty, price)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isLoading && isFormValid && (affordabilityResult?.canAfford != false || !isBuying),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBuying) Color(0xFF10B981) else Color(0xFFEF4444),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isBuying) "Buy" else "Sell",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}