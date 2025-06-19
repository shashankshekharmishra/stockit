package com.example.stockit.ui.screens.stock

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow  // Add this import
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow  // Add this import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.BorderStroke  // Add this import
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stockSymbol: String,
    onBackClick: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedTimeFrame by remember { mutableStateOf("1M") }
    var startAnimation by remember { mutableStateOf(false) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    
    // Enhanced animations
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
    
    val cardsScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cards_scale"
    )

    // Initialize data loading
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        viewModel.loadStockData(stockSymbol)
        viewModel.debugSpecificEndpoint(stockSymbol, "1M")
        // Check watchlist status
        viewModel.checkWatchlistStatus(stockSymbol)
    }
    
    LaunchedEffect(selectedTimeFrame) {
        if (uiState.stockData != null) {
            println("🔄 Loading chart data for timeframe: $selectedTimeFrame")
            viewModel.loadHistoricalData(stockSymbol, selectedTimeFrame)
        }
    }
    
    // Background retry mechanism - Updated for new holdings strategy
    LaunchedEffect(uiState.isAuthenticated) {
        while (true) {
            delay(2000) // Wait 2 seconds
            
            // Retry stock data if missing and not loading
            if (uiState.stockData == null && !uiState.isLoading) {
                viewModel.retryStockDataInBackground(stockSymbol)
            }
            
            // Retry chart data if missing and not loading
            if (uiState.chartData.isEmpty() && !uiState.isLoadingChart && uiState.stockData != null) {
                viewModel.retryChartDataInBackground(stockSymbol, selectedTimeFrame)
            }
            
            // Retry user holdings based on new strategy:
            // - Only retry twice initially when screen opens
            // - Continuously retry after successful trades
            if (uiState.isAuthenticated && uiState.userHolding == null && !uiState.isLoadingHolding) {
                viewModel.retryUserHoldingInBackground(stockSymbol)
            }
        }
    }
    
    // Handle success messages and reload holdings after trades
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            
            // No need to manually reload holdings here as it's handled in the ViewModel
            // after successful trades
            
            delay(2000)
            viewModel.clearSuccessMessage()
        }
    }

    // Add cleanup when user navigates away
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopContinuousHoldingsRetry()
        }
    }

    // Trading Dialogs
    TradingDialog(
        isVisible = showBuyDialog,
        isBuying = true,
        stockSymbol = stockSymbol,
        stockPrice = uiState.stockData?.price ?: 0.0,
        onDismiss = { 
            showBuyDialog = false
            viewModel.clearAffordabilityResult()
        },
        onConfirm = { quantity, pricePerShare ->
            viewModel.buyStock(stockSymbol, quantity, pricePerShare)
            showBuyDialog = false
        },
        onCheckAffordability = { quantity, pricePerShare ->
            viewModel.checkAffordability(stockSymbol, quantity, pricePerShare)
        },
        isLoading = uiState.isTradingLoading,
        affordabilityResult = uiState.affordabilityResult,
        isAuthenticated = uiState.isAuthenticated
    )
    
    TradingDialog(
        isVisible = showSellDialog,
        isBuying = false,
        stockSymbol = stockSymbol,
        stockPrice = uiState.stockData?.price ?: 0.0,
        onDismiss = { 
            showSellDialog = false
            viewModel.clearAffordabilityResult()
        },
        onConfirm = { quantity, pricePerShare ->
            viewModel.sellStock(stockSymbol, quantity, pricePerShare)
            showSellDialog = false
        },
        isLoading = uiState.isTradingLoading,
        isAuthenticated = uiState.isAuthenticated
    )

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
            .semantics {
                contentDescription = "Stock detail screen for $stockSymbol"
            }
    ) {
        // Simplified background decoration for better contrast
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 3
            
            // Subtle background circles with lower opacity
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
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp)
                .semantics {
                    contentDescription = "Stock information and trading options"
                },
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Custom Header with Watchlist Integration
            item {
                AccessibleHeader(
                    stockSymbol = stockSymbol,
                    stockData = uiState.stockData,
                    onBackClick = onBackClick,
                    onWatchlistClick = {
                        if (uiState.isInWatchlist) {
                            viewModel.removeFromWatchlist(stockSymbol)
                        } else {
                            viewModel.addToWatchlist(stockSymbol)
                        }
                    },
                    isInWatchlist = uiState.isInWatchlist,
                    isAuthenticated = uiState.isAuthenticated,
                    modifier = Modifier.offset(y = headerOffset.dp)
                )
            }
            
            // Price Card with Better Contrast
            item {
                AccessiblePriceCard(
                    stockData = uiState.stockData,
                    isLoading = uiState.isLoading,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(
                            alpha = contentAlpha,
                            scaleX = cardsScale,
                            scaleY = cardsScale
                        )
                )
            }
            
            // Accessible Chart Card
            item {
                AccessibleChartCard(
                    chartData = uiState.chartData,
                    selectedTimeFrame = selectedTimeFrame,
                    onTimeFrameSelected = { selectedTimeFrame = it },
                    isLoading = uiState.isLoadingChart,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(
                            alpha = contentAlpha,
                            scaleX = cardsScale,
                            scaleY = cardsScale
                        )
                )
            }
            
            // Accessible Statistics
            item {
                AccessibleStatisticsCard(
                    stockData = uiState.stockData,
                    stockDetails = uiState.stockDetails,
                    isLoading = uiState.isLoading,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(
                            alpha = contentAlpha,
                            scaleX = cardsScale,
                            scaleY = cardsScale
                        )
                )
            }
            
            // User Holdings (if authenticated and has holdings)
            if (uiState.isAuthenticated) {
                item {
                    AccessibleHoldingsCard(
                        userHolding = uiState.userHolding,
                        isLoading = uiState.isLoadingHolding,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .graphicsLayer(
                                alpha = contentAlpha,
                                scaleX = cardsScale,
                                scaleY = cardsScale
                            )
                    )
                }
            }
            
            // Accessible Trading Buttons
            item {
                AccessibleTradingButtons(
                    stockSymbol = stockSymbol,
                    stockPrice = uiState.stockData?.price ?: 0.0,
                    onBuyClick = { showBuyDialog = true },
                    onSellClick = { showSellDialog = true },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(
                            alpha = contentAlpha,
                            scaleX = cardsScale,
                            scaleY = cardsScale
                        )
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
        // Custom Snackbar
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
fun AccessibleHeader(
    stockSymbol: String,
    stockData: StockData?,
    onBackClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    isInWatchlist: Boolean,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = "Header section for $stockSymbol stock"
            }
    ) {
        // Enhanced glass morphism with better contrast - matching watchlist style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // Match watchlist header height
                .background(
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp) // Match watchlist corner radius
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            // Subtle gradient overlay - matching watchlist
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
        
        // Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp), // Match watchlist padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button (left side) - removed shadow
            Box(
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
                    .clickable { onBackClick() }
                    .semantics {
                        contentDescription = "Go back to previous screen"
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Stock Info (center) - consistent styling with watchlist
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Stock symbol: $stockSymbol"
                    }
            ) {
                Text(
                    text = stockSymbol,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    fontSize = 20.sp // Match watchlist title size
                )
                stockData?.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .semantics {
                                contentDescription = "Company name: $name"
                            }
                    )
                }
            }
            
            // Watchlist button (right side) - removed shadow
            if (isAuthenticated) {
                IconButton(
                    onClick = onWatchlistClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = if (isInWatchlist) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF475569),
                                        Color(0xFF334155)
                                    )
                                )
                            },
                            shape = CircleShape
                        )
                        .semantics {
                            contentDescription = if (isInWatchlist) {
                                "Remove $stockSymbol from watchlist"
                            } else {
                                "Add $stockSymbol to watchlist"
                            }
                            role = Role.Button
                        }
                ) {
                    Icon(
                        if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp) // Match watchlist icon size
                    )
                }
            } else {
                // Empty spacer to maintain layout balance when not authenticated
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun AccessiblePriceCard(
    stockData: StockData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated shimmer effect
    val shimmerColors = listOf(
        Color(0xFF334155).copy(alpha = 0.3f),
        Color(0xFF475569).copy(alpha = 0.5f),
        Color(0xFF334155).copy(alpha = 0.3f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = Color(0xFF1E293B).copy(alpha = 0.95f), // Better contrast
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .semantics {
                contentDescription = if (isLoading) {
                    "Loading price information"
                } else {
                    stockData?.let { data ->
                        val change = data.change ?: 0.0
                        val changePercent = data.changePercent ?: 0.0
                        val changeDirection = if (change >= 0) "up" else "down"
                        "Current price: ₹${String.format("%.2f", data.price)}. " +
                        "Change: ${if (change >= 0) "+" else ""}${String.format("%.2f", change)} " +
                        "or ${if (changePercent >= 0) "+" else ""}${String.format("%.2f", changePercent)}% $changeDirection"
                    } ?: "Price information unavailable"
                }
            }
    ) {
        // Subtle glass morphism background
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
        
        // Content
        if (isLoading) {
            // Enhanced shimmer effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = shimmerColors,
                            start = Offset(translateAnim - 200f, 0f),
                            end = Offset(translateAnim, 0f)
                        )
                    )
            )
        } else if (stockData != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Animated price
                val animatedPrice by animateFloatAsState(
                    targetValue = stockData.price.toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "price_animation"
                )
                
                Text(
                    text = "₹${String.format("%.2f", animatedPrice)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 42.sp,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.semantics {
                        contentDescription = "Current stock price: ₹${String.format("%.2f", animatedPrice)}"
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val change = stockData.change ?: 0.0
                val changePercent = stockData.changePercent ?: 0.0
                val isPositive = change >= 0
                val changeColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444) // Better contrast colors
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        val changeDirection = if (isPositive) "increased" else "decreased"
                        contentDescription = "Stock has $changeDirection by ₹${String.format("%.2f", kotlin.math.abs(change))} " +
                                "or ${String.format("%.2f", kotlin.math.abs(changePercent))} percent"
                    }
                ) {
                    // Enhanced change indicator
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = changeColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", change)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = changeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePercent)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = changeColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibleChartCard(
    chartData: List<ChartPoint>,
    selectedTimeFrame: String,
    onTimeFrameSelected: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFrames = listOf("1W", "1M", "3M", "Y")
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .semantics {
                contentDescription = if (isLoading) {
                    "Loading price chart for $selectedTimeFrame timeframe"
                } else {
                    "Price chart for $selectedTimeFrame timeframe showing ${chartData.size} data points"
                }
            }
    ) {
        // Subtle glass morphism background
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
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Price Chart",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Chart
            if (isLoading) {
                AccessibleChartShimmer()
            } else {
                AccessibleStockChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Enhanced accessible time frame selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp) // Increased height from 60dp to 68dp
                    .semantics {
                        contentDescription = "Time frame selector. Currently selected: $selectedTimeFrame"
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeFrames.forEach { timeFrame ->
                    val isSelected = timeFrame == selectedTimeFrame
                    
                    Button(
                        onClick = { onTimeFrameSelected(timeFrame) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = "Select $timeFrame timeframe"
                                if (isSelected) {
                                    stateDescription = "Selected"
                                }
                                role = Role.Button
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                Color(0xFF0F172A).copy(alpha = 0.9f)
                            } else {
                                Color(0xFF0F172A).copy(alpha = 0.6f)
                            },
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(0.dp), // Remove all padding
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isSelected) 0.3f else 0.15f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center // Force center alignment
                        ) {
                            Text(
                                text = timeFrame,
                                color = Color.White,
                                fontSize = 12.sp, // Reduced from 14sp to 12sp
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics {
                                    contentDescription = "$timeFrame timeframe button"
                            }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibleStockChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .semantics {
                    contentDescription = "No chart data available for the selected timeframe"
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No chart data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }
    
    val isPositive = if (data.size >= 2) {
        data.last().price >= data.first().price
    } else {
        true
    }
    val chartColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
    
    // Animation for chart drawing
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "chart_animation"
    )
    
    Canvas(
        modifier = modifier.semantics {
            val trend = if (isPositive) "upward" else "downward"
            val startPrice = data.firstOrNull()?.price ?: 0.0
            val endPrice = data.lastOrNull()?.price ?: 0.0
            contentDescription = "Stock price chart showing $trend trend from ₹${String.format("%.2f", startPrice)} to ₹${String.format("%.2f", endPrice)}"
        }
    ) {
        val width = size.width
        val height = size.height
        val padding = 40.dp.toPx()
        
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        
        if (data.size < 2) return@Canvas
        
        val minPrice = data.minOf { it.price }
        val maxPrice = data.maxOf { it.price }
        val priceRange = maxPrice - minPrice
        
        if (priceRange == 0.0) return@Canvas
        
        // Calculate smooth curve points using Bézier curves
        val points = data.mapIndexed { index, point ->
            val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
            val y = padding + ((maxPrice - point.price) / priceRange * chartHeight).toFloat()
            Offset(x, y)
        }
        
        // Only draw up to the animated progress
        val animatedPointCount = (points.size * animationProgress).toInt().coerceAtLeast(2)
        val animatedPoints = points.take(animatedPointCount)
        
        // Create smooth path using cubic Bézier curves
        val path = Path().apply {
            if (animatedPoints.isNotEmpty()) {
                moveTo(animatedPoints.first().x, animatedPoints.first().y)
                
                for (i in 1 until animatedPoints.size) {
                    val controlPoint1 = if (i > 1) {
                        val prev = animatedPoints[i - 2]
                        val curr = animatedPoints[i - 1]
                        val next = animatedPoints[i]
                        Offset(
                            curr.x + (next.x - prev.x) * 0.15f,
                            curr.y + (next.y - prev.y) * 0.15f
                        )
                    } else {
                        val curr = animatedPoints[i - 1]
                        val next = animatedPoints[i]
                        Offset(
                            curr.x + (next.x - curr.x) * 0.3f,
                            curr.y
                        )
                    }
                    
                    val controlPoint2 = if (i < animatedPoints.size - 1) {
                        val prev = animatedPoints[i - 1]
                        val curr = animatedPoints[i]
                        val next = animatedPoints[i + 1]
                        Offset(
                            curr.x - (next.x - prev.x) * 0.15f,
                            curr.y - (next.y - prev.y) * 0.15f
                        )
                    } else {
                        val prev = animatedPoints[i - 1]
                        val curr = animatedPoints[i]
                        Offset(
                            curr.x - (curr.x - prev.x) * 0.3f,
                            curr.y
                        )
                    }
                    
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        animatedPoints[i].x, animatedPoints[i].y
                    )
                }
            }
        }
        
        // Draw gradient fill with better visibility
        val fillPath = Path().apply {
            addPath(path)
            lineTo(animatedPoints.last().x, height - padding)
            lineTo(animatedPoints.first().x, height - padding)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    chartColor.copy(alpha = 0.3f),
                    chartColor.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )
        
        // Draw main chart line with better visibility
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw key points only to avoid clutter
        if (animatedPoints.size >= 2) {
            // Start point
            drawCircle(
                color = chartColor,
                radius = 3.dp.toPx(),
                center = animatedPoints.first()
            )
            // End point
            drawCircle(
                color = chartColor,
                radius = 4.dp.toPx(), // Fixed: added .toPx()
                center = animatedPoints.last()
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(), // Fixed: added .toPx()
                center = animatedPoints.last()
            )
        }
    }
}

@Composable
fun AccessibleStatisticsCard(
    stockData: StockData?,
    stockDetails: StockDetails?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .semantics {
                contentDescription = if (isLoading) {
                    "Loading key statistics"
                } else {
                    "Key statistics section with market data"
                }
            }
    ) {
        // Subtle glass morphism background
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
                .padding(24.dp)
        ) {
            Text(
                text = "Key Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoading) {
                AccessibleStatisticsShimmer()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccessibleStatisticItem(
                            label = "Open",
                            value = stockData?.open?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        AccessibleStatisticItem(
                            label = "Previous Close",
                            value = stockData?.previousClose?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccessibleStatisticItem(
                            label = "Day High",
                            value = stockData?.high?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        AccessibleStatisticItem(
                            label = "Day Low",
                            value = stockData?.low?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccessibleStatisticItem(
                            label = "Volume",
                            value = stockData?.volume?.let { formatVolume(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        AccessibleStatisticItem(
                            label = "Market Cap",
                            value = stockDetails?.marketCap?.let { formatMarketCap(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibleStatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8), // Better contrast
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun AccessibleTradingButtons(
    stockSymbol: String,
    stockPrice: Double,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Trading options for $stockSymbol at ₹${String.format("%.2f", stockPrice)} per share"
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sell Button
        Button(
            onClick = onSellClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp) // Proper minimum touch target
                .semantics {
                    contentDescription = "Sell $stockSymbol stock"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = "Sell $stockSymbol stock",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sell",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // Buy Button
        Button(
            onClick = onBuyClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp) // Proper minimum touch target
                .semantics {
                    contentDescription = "Buy $stockSymbol stock"
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Buy $stockSymbol stock",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buy",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// Enhanced Shimmer Components with better contrast
@Composable
fun AccessibleChartShimmer() {
    val shimmerColors = listOf(
        Color(0xFF334155).copy(alpha = 0.3f),
        Color(0xFF475569).copy(alpha = 0.5f),
        Color(0xFF334155).copy(alpha = 0.3f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim - 200f, 0f),
                    end = Offset(translateAnim, 0f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .semantics {
                contentDescription = "Loading chart animation"
            }
    )
}

@Composable
fun AccessibleStatisticsShimmer() {
    val shimmerColors = listOf(
        Color(0xFF334155).copy(alpha = 0.3f),
        Color(0xFF475569).copy(alpha = 0.5f),
        Color(0xFF334155).copy(alpha = 0.3f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.semantics {
            contentDescription = "Loading statistics animation"
        }
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(2) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(12.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = shimmerColors,
                                        start = Offset(translateAnim - 100f, 0f),
                                        end = Offset(translateAnim, 0f)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(16.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = shimmerColors,
                                        start = Offset(translateAnim - 100f, 0f),
                                        end = Offset(translateAnim, 0f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

// Add this new composable after AccessibleStatisticsCard
@Composable
fun AccessibleHoldingsCard(
    userHolding: UserHolding?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Don't show anything if loading or if no holding data
    if (isLoading || userHolding == null) return
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .semantics {
                contentDescription = "Your holdings: ${userHolding.quantity} shares at average price ₹${String.format("%.2f", userHolding.averagePrice)}"
            }
    ) {
        // Subtle glass morphism background
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
                .padding(24.dp)
        ) {
            Text(
                text = "Your Holdings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccessibleStatisticItem(
                        label = "Quantity",
                        value = "${userHolding.quantity} shares",
                        modifier = Modifier.weight(1f)
                    )
                    AccessibleStatisticItem(
                        label = "Avg. Price",
                        value = "₹${String.format("%.2f", userHolding.averagePrice)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccessibleStatisticItem(
                        label = "Invested",
                        value = "₹${String.format("%.2f", userHolding.investedAmount)}",
                        modifier = Modifier.weight(1f)
                    )
                    AccessibleStatisticItem(
                        label = "Current Value",
                        value = "₹${String.format("%.2f", userHolding.currentValue)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isProfit = userHolding.profitLoss >= 0
                    val profitLossColor = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Profit/Loss: ${if (isProfit) "profit" else "loss"} of ₹${String.format("%.2f", kotlin.math.abs(userHolding.profitLoss))}"
                            }
                    ) {
                        Text(
                            text = "P&L",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = profitLossColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (isProfit) "+" else ""}₹${String.format("%.2f", userHolding.profitLoss)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = profitLossColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    if (userHolding.firstBuyDate != null) {
                        AccessibleStatisticItem(
                            label = "First Buy",
                            value = userHolding.firstBuyDate.take(10), // Show only date part
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Keep existing utility functions
fun formatVolume(volume: Long): String {
    return when {
        volume >= 1_000_000_000 -> String.format("%.1fB", volume / 1_000_000_000.0)
        volume >= 1_000_000 -> String.format("%.1fM", volume / 1_000_000.0)
        volume >= 1_000 -> String.format("%.1fK", volume / 1_000.0)
        else -> volume.toString()
    }
}

fun formatMarketCap(marketCap: Double): String {
    return when {
        marketCap >= 1_000_000_000_000 -> String.format("₹%.1fT", marketCap / 1_000_000_000_000.0)
        marketCap >= 1_000_000_000 -> String.format("₹%.1fB", marketCap / 1_000_000_000.0)
        marketCap >= 1_000_000 -> String.format("₹%.1fM", marketCap / 1_000_000.0)
        else -> String.format("₹%.1f", marketCap)
    }
}