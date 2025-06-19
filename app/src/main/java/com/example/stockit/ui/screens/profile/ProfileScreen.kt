package com.example.stockit.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stockit.data.model.PortfolioStock
import com.example.stockit.data.model.RecentTransaction
import com.example.stockit.data.model.UserProfile
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onStockClick: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var startAnimation by remember { mutableStateOf(false) }
    
    // Animation states
    val headerOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -200f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "header_offset"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    // Start animation
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }
    
    // Continuous background retry mechanism - silently fills missing data
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            while (true) {
                delay(3000) // Wait 3 seconds between attempts
                
                // Check if we have any missing data that we can try to fill
                val hasMissingData = uiState.userProfile == null || 
                                   uiState.portfolioStocks.isEmpty() || 
                                   uiState.recentTransactions.isEmpty() ||
                                   (uiState.userProfile?.balance == 0.0 && uiState.userProfile?.totalInvested == 0.0)
                
                // Only retry if we have missing data and we're not already loading/refreshing
                if (hasMissingData && !uiState.isLoading && !uiState.isRefreshing) {
                    viewModel.retryProfileInBackground()
                }
                
                // Also do periodic updates every 30 seconds to keep data fresh
                // This helps ensure we have the latest portfolio values and prices
                delay(27000) // Total 30 seconds (3 + 27)
                if (!uiState.isLoading && !uiState.isRefreshing) {
                    viewModel.retryProfileInBackground()
                }
            }
        }
    }
    
    // Handle error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            delay(3000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // Background decoration
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 3
            
            drawCircle(
                color = Color.White.copy(alpha = 0.02f),
                radius = size.width * 0.6f,
                center = Offset(centerX - size.width * 0.3f, centerY - 100.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.015f),
                radius = size.width * 0.8f,
                center = Offset(centerX + size.width * 0.2f, centerY + 200.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp)
        ) {
            // Header
            ProfileHeader(
                isAuthenticated = uiState.isAuthenticated,
                userProfile = uiState.userProfile,
                onRefresh = { viewModel.refreshProfile() },
                onLogout = {
                    viewModel.logout()
                    onLogout()
                },
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.offset(y = headerOffset.dp)
            )
            
            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Create local variable to enable smart cast
                val currentUserProfile = uiState.userProfile
                when {
                    !uiState.isAuthenticated -> {
                        NotAuthenticatedMessage()
                    }
                    uiState.isLoading -> {
                        ProfileLoadingState()
                    }
                    currentUserProfile != null -> {
                        ProfileContent(
                            userProfile = currentUserProfile,
                            portfolioStocks = uiState.portfolioStocks,
                            recentTransactions = uiState.recentTransactions,
                            onStockClick = onStockClick
                        )
                    }
                    else -> {
                        ErrorMessage("Failed to load profile data")
                    }
                }
            }
        }
        
        // Snackbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
fun ProfileHeader(
    isAuthenticated: Boolean,
    userProfile: UserProfile?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // Match WatchlistScreen header height
                .background(
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp) // Match WatchlistScreen corner radius
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp), // Match WatchlistScreen padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile Icon (left side) - consistent with watchlist's icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Title (center) - consistent styling
            Text(
                text = if (isAuthenticated) "My Profile" else "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                fontSize = 20.sp // Match WatchlistScreen font size
            )
            
            // Action Buttons (right side) - consistent with watchlist
            if (isAuthenticated) {
                Row {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF475569),
                                        Color(0xFF334155)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh profile",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.3f),
                                        Color(0xFFDC2626).copy(alpha = 0.2f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // Empty spacer to maintain layout balance when not authenticated
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun ProfileContent(
    userProfile: UserProfile,
    portfolioStocks: List<PortfolioStock>,
    recentTransactions: List<RecentTransaction>,
    onStockClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Financial Summary Card
        item {
            FinancialSummaryCard(userProfile = userProfile)
        }
        
        // Portfolio Section
        item {
            SectionHeader(
                title = "Portfolio",
                subtitle = "${portfolioStocks.size} stocks"
            )
        }
        
        if (portfolioStocks.isNotEmpty()) {
            items(
                items = portfolioStocks,
                key = { it.symbol }
            ) { stock ->
                PortfolioStockCard(
                    stock = stock,
                    onClick = { onStockClick(stock.symbol) }
                )
            }
        } else {
            item {
                EmptyPortfolioMessage()
            }
        }
        
        // Recent Transactions Section
        item {
            SectionHeader(
                title = "Recent Transactions",
                subtitle = "${recentTransactions.size} recent trades"
            )
        }
        
        if (recentTransactions.isNotEmpty()) {
            items(
                items = recentTransactions,
                key = { it.id }
            ) { transaction ->
                TransactionCard(transaction = transaction)
            }
        } else {
            item {
                EmptyTransactionsMessage()
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun FinancialSummaryCard(userProfile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // Glass morphism background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Financial Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Balance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Available Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "₹${String.format("%.2f", userProfile.balance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Invested",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "₹${String.format("%.2f", userProfile.totalInvested)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Portfolio Value and P&L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Portfolio Value",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "₹${String.format("%.2f", userProfile.totalValue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Profit/Loss",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        val isPositive = userProfile.profitLoss >= 0
                        val changeColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}₹${String.format("%.2f", userProfile.profitLoss)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = changeColor,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 20.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioStockCard(
    stock: PortfolioStock,
    onClick: () -> Unit
) {
    val isPositive = stock.profitLoss >= 0
    val changeColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // Glass morphism background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        stock.companyName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}₹${String.format("%.2f", stock.profitLoss)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = changeColor,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.profitLossPercent)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Quantity: ${stock.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Avg: ₹${String.format("%.2f", stock.averagePrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current: ₹${String.format("%.2f", stock.currentPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Value: ₹${String.format("%.2f", stock.currentValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: RecentTransaction) {
    val isBuy = transaction.type.lowercase() == "buy"
    val typeColor = if (isBuy) Color(0xFF10B981) else Color(0xFFEF4444)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBuy) Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${transaction.type.uppercase()} ${transaction.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "${transaction.quantity} shares @ ₹${String.format("%.2f", transaction.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    fontSize = 16.sp
                )
                Text(
                    text = transaction.timestamp.substringBefore('T'),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyPortfolioMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Investments Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Start investing to build your portfolio",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyTransactionsMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Your trading history will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NotAuthenticatedMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF6366F1)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Login Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please log in to view your profile and portfolio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProfileLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF6366F1),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading profile...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}