package com.example.stockit.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onStockClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var startAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "home_animations")
    
    val backgroundPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_pulse"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
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

    // Floating particles animation
    val particleOffsets = remember {
        (0..12).map {
            Animatable((-50f + it * 20f))
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
        // Animate floating particles
        particleOffsets.forEachIndexed { index, animatable ->
            scope.launch {
                animatable.animateTo(
                    targetValue = animatable.value + (20f + index * 5f),
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 3000 + index * 200,
                            easing = EaseInOutSine
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }
    
    // Get auth token and set it in ViewModel
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        val token = authManager.getAccessToken()
        viewModel.setAuthToken(token)
        viewModel.loadData()
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
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC).copy(alpha = backgroundPulse),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0),
                        Color(0xFFCBD5E1)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // Enhanced decorative floating particles (similar to onboarding)
        particleOffsets.forEachIndexed { index, offset ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (30 * (index - 6)).dp,
                        y = offset.value.dp
                    )
                    .size((3 + index % 4).dp)
                    .background(
                        Color(0xFF6366F1).copy(alpha = 0.08f + index * 0.01f),
                        CircleShape
                    )
                    .graphicsLayer {
                        rotationZ = offset.value * 0.5f
                    }
            )
        }

        // Additional background particles effect (like onboarding)
        repeat(8) { index ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (60 * (index - 4)).dp,
                        y = (-150 + 50 * index).dp
                    )
                    .size(4.dp)
                    .background(
                        Color(0xFF6366F1).copy(alpha = 0.1f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "StockIt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.5.sp,
                            fontSize = 24.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Handle menu */ }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFA855F7)
                                )
                            )
                        )
                        .shadow(
                            elevation = 8.dp,
                            ambientColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                            spotColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
                        )
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .scale(contentScale)
                    .alpha(contentAlpha),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
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
}

@Composable
fun EnhancedPortfolioCard(
    summary: PortfolioSummary?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                spotColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
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
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "EQUITY",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    // Enhanced Divider
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(70.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.1f),
                                        Color.White.copy(alpha = 0.5f),
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
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "BALANCE",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
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
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Start investing today",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced Stock Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = CircleShape,
                        ambientColor = getStockIconColor(stock.symbol).copy(alpha = 0.1f)
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                getStockIconColor(stock.symbol),
                                getStockIconColor(stock.symbol).copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getStockIcon(stock.symbol),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Stock Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = stock.name ?: stock.symbol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            // Price and Change
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹ ${formatPrice(stock.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )

                val change = stock.change ?: 0.0
                val changePercent = stock.changePercent ?: 0.0
                val isPositive = change >= 0
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
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePercent)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor,
                            fontWeight = FontWeight.Bold
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        Color(0xFFF1F5F9),
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
                            Color(0xFFF1F5F9),
                            RoundedCornerShape(9.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .background(
                            Color(0xFFF8FAFC),
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
                            Color(0xFFF1F5F9),
                            RoundedCornerShape(9.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(28.dp)
                        .background(
                            Color(0xFFF8FAFC),
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
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(20.dp)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(6.dp)
                    )
            )
        }

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(70.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(20.dp)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
fun EnhancedEmptyState(onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.1f),
                spotColor = Color(0xFF8B5CF6).copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check your connection and try again",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                    spotColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFA855F7)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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