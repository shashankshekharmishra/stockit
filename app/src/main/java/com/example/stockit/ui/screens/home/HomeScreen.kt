package com.example.stockit.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Login // Change this import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockit.ui.components.StockCard
import com.example.stockit.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.example.stockit.utils.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onStockClick: (String) -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {}, // Add this parameter
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var startAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
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
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "content_alpha"
    )
    
    val contentScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "content_scale"
    )

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }
    
    // Get auth token and set it in ViewModel
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        val token = authManager.getAccessToken()
        viewModel.setAuthToken(token)
        viewModel.loadData()
    }

    // Background retry mechanism - retry every 2 seconds if data is missing
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Wait 2 seconds
            
            // Check if we need to retry portfolio data
            if (uiState.portfolioSummary == null && !uiState.isLoadingPortfolio) {
                viewModel.retryPortfolioInBackground()
            }
            
            // Check if we need to retry trending stocks (if list is empty and not loading)
            if (uiState.trendingStocks.isEmpty() && !uiState.isLoadingStocks) {
                viewModel.retryTrendingStocksInBackground()
            }
        }
    }

    // Show error snackbar and auto-dismiss after 1 second
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            delay(1000)
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
        // Background decoration (same as watchlist)
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
            HomeHeader(
                onRefresh = { viewModel.loadData() },
                onNavigateToOnboarding = onNavigateToOnboarding, // Pass the callback
                isRefreshing = uiState.isLoadingStocks || uiState.isLoadingPortfolio,
                modifier = Modifier.offset(y = headerOffset.dp)
            )
            
            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        alpha = contentAlpha,
                        scaleX = contentScale,
                        scaleY = contentScale
                    )
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Portfolio Card with Enhanced Design
                    item {
                        EnhancedPortfolioCard(
                            summary = uiState.portfolioSummary,
                            isLoading = uiState.isLoadingPortfolio
                        )
                    }

                    // Stock List Header
                    item {
                        Text(
                            text = "Trending Stocks",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Stock List
                    when {
                        uiState.isLoadingStocks -> {
                            items(5) {
                                EnhancedStockItemShimmer()
                            }
                        }
                        uiState.trendingStocks.isNotEmpty() -> {
                            items(uiState.trendingStocks) { stock ->
                                EnhancedStockListItem(
                                    stock = stock,
                                    onClick = { onStockClick(stock.symbol) }
                                )
                            }
                        }
                        else -> {
                            item {
                                EnhancedEmptyState(onRetry = { viewModel.retry() })
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
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
fun HomeHeader(
    onRefresh: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
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
                .height(72.dp) // Slightly reduced height
                .background(
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Login Button (left side) - removed shadow
            IconButton(
                onClick = onNavigateToOnboarding,
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
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = "Login / Sign Up",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // App Title (center) - Fixed styling
            Text(
                text = "StockIt",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                fontSize = 26.sp,
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Refresh Button (right side) - removed shadow
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
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPortfolioCard(
    summary: PortfolioSummary?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                if (isLoading) {
                    EnhancedPortfolioShimmer()
                } else if (summary != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Equity Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "₹${formatPortfolioNumber(summary.totalValue)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "EQUITY",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        }

                        // Enhanced Divider
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(60.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.1f),
                                            Color.White.copy(alpha = 0.3f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )

                        // Balance Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "₹${formatPortfolioNumber(summary.availableBalance)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "BALANCE",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    // Enhanced Empty state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "₹0.00",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Start investing today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedStockListItem(
    stock: StockData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced Stock Icon with only rank
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = getStockIconColor(stock.symbol).copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Show only rank if available
                    stock.rank?.let { rank ->
                        Text(
                            text = "#$rank",
                            color = getStockIconColor(stock.symbol),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } ?: run {
                        // Fallback to first letter if no rank
                        Text(
                            text = stock.symbol.take(1).uppercase(),
                            color = getStockIconColor(stock.symbol),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stock Info with text overflow handling
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stock.name ?: stock.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    // Show volume if available
                    stock.volume?.let { volume ->
                        Text(
                            text = "Vol: ${formatVolume(volume)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Price and Change with High/Low if available
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "₹ ${formatPrice(stock.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    val change = stock.change ?: 0.0
                    val changePercent = stock.changePercent ?: 0.0
                    val isPositive = stock.positive ?: (change >= 0)
                    val changeColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = changeColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePercent)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = changeColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // Show High/Low if available
                    if (stock.high != null && stock.low != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "H: ₹${formatPrice(stock.high)} L: ₹${formatPrice(stock.low)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedStockItemShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(18.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(9.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(7.dp)
                        )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(9.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(14.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun EnhancedPortfolioShimmer() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(20.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(6.dp)
                    )
            )
        }

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(70.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(20.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
fun EnhancedEmptyState(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📈",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No stocks available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check your connection and try again",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Retry",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Utility functions
fun formatPortfolioNumber(number: Double): String {
    return String.format("%.2f", number)
}

fun formatPrice(price: Double): String {
    return String.format("%.2f", price)
}

fun getStockIcon(symbol: String): String {
    return when (symbol.uppercase()) {
        "BTC/BIDR", "BTC" -> "₿"
        "PPC" -> "P"
        "AAPL" -> "🍎"
        "BABA" -> "🛒"
        "DOGE" -> "🐕"
        "LTC" -> "L"
        "RELIANCE" -> "R"
        "TCS" -> "T"
        "INFY" -> "I"
        "HDFCBANK" -> "H"
        "ICICIBANK" -> "I"
        "HINDUNILVR" -> "H"
        "BHARTIARTL" -> "B"
        "ITC" -> "I"
        "KOTAKBANK" -> "K"
        "LT" -> "L"
        "SBIN" -> "S"
        "ASIANPAINT" -> "A"
        "MARUTI" -> "M"
        "HCLTECH" -> "H"
        "AXISBANK" -> "A"
        "BAJFINANCE" -> "B"
        "WIPRO" -> "W"
        "TECHM" -> "T"
        "ULTRACEMCO" -> "U"
        "NESTLEIND" -> "N"
        else -> symbol.take(1).uppercase()
    }
}

fun getStockIconColor(symbol: String): Color {
    return when (symbol.uppercase()) {
        "BTC/BIDR", "BTC" -> Color(0xFFF7931A)
        "PPC" -> Color(0xFF3FA9F5)
        "AAPL" -> Color(0xFF000000)
        "BABA" -> Color(0xFFFF6900)
        "DOGE" -> Color(0xFFBAA332)
        "LTC" -> Color(0xFF345D9D)
        "RELIANCE" -> Color(0xFF0066CC)
        "TCS" -> Color(0xFF004B9F)
        "INFY" -> Color(0xFF007CC3)
        "HDFCBANK" -> Color(0xFF004B87)
        "ICICIBANK" -> Color(0xFFF47721)
        "HINDUNILVR" -> Color(0xFF0066B2)
        "BHARTIARTL" -> Color(0xFFE60000)
        "ITC" -> Color(0xFFFFD700)
        "KOTAKBANK" -> Color(0xFF0066CC)
        "LT" -> Color(0xFF0099CC)
        "SBIN" -> Color(0xFF22409A)
        "ASIANPAINT" -> Color(0xFFFF6600)
        "MARUTI" -> Color(0xFFFF0000)
        "HCLTECH" -> Color(0xFF0066CC)
        "AXISBANK" -> Color(0xFF800080)
        "BAJFINANCE" -> Color(0xFF0066CC)
        "WIPRO" -> Color(0xFF0066CC)
        "TECHM" -> Color(0xFF9933CC)
        "ULTRACEMCO" -> Color(0xFF808080)
        "NESTLEIND" -> Color(0xFFFF0000)
        else -> Color(0xFF6366F1)
    }
}

// Add volume formatting utility function
fun formatVolume(volume: Long): String {
    return when {
        volume >= 1_00_00_000 -> String.format("%.1fCr", volume / 1_00_00_000.0)
        volume >= 1_00_000 -> String.format("%.1fL", volume / 1_00_000.0)
        volume >= 1_000 -> String.format("%.1fK", volume / 1_000.0)
        else -> volume.toString()
    }
}