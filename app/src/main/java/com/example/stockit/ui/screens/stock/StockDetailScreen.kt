package com.example.stockit.ui.screens.stock

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.blur
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stockSymbol: String,
    onBackClick: () -> Unit,
    viewModel: StockDetailViewModel = viewModel()
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

    // Initialize authentication
    LaunchedEffect(Unit) {
        viewModel.initializeAuth(context)
        delay(100)
        startAnimation = true
        viewModel.loadStockData(stockSymbol)
        viewModel.debugSpecificEndpoint(stockSymbol, "1M")
    }
    
    LaunchedEffect(selectedTimeFrame) {
        if (uiState.stockData != null) {
            println("🔄 Loading chart data for timeframe: $selectedTimeFrame")
            viewModel.loadHistoricalData(stockSymbol, selectedTimeFrame)
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
    
    // Handle success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            delay(2000)
            viewModel.clearSuccessMessage()
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
                        Color(0xFF667EEA),
                        Color(0xFF764BA2),
                        Color(0xFF1A1A2E)
                    )
                )
            )
    ) {
        // Background decoration
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 3
            
            // Animated background circles
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.6f,
                center = Offset(centerX - size.width * 0.3f, centerY - 100.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = size.width * 0.8f,
                center = Offset(centerX + size.width * 0.2f, centerY + 200.dp.toPx())
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Custom Header
            item {
                ModernHeader(
                    stockSymbol = stockSymbol,
                    stockData = uiState.stockData,
                    onBackClick = onBackClick,
                    modifier = Modifier.offset(y = headerOffset.dp)
                )
            }
            
            // Price Card with Glass Morphism
            item {
                GlassPriceCard(
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
            
            // Smooth Chart Card
            item {
                SmoothChartCard(
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
            
            // Enhanced Statistics
            item {
                GlassStatisticsCard(
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
            
            // Modern Trading Buttons
            item {
                ModernTradingButtons(
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
fun ModernHeader(
    stockSymbol: String,
    stockData: StockData?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Glass morphism header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
        ) {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
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
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stockSymbol,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 24.sp
                )
                stockData?.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
            
            Row {
                IconButton(
                    onClick = { /* TODO: Add to watchlist */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { /* TODO: Share */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassPriceCard(
    stockData: StockData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated shimmer effect
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f)
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
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Glass morphism background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
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
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val change = stockData.change ?: 0.0
                val changePercent = stockData.changePercent ?: 0.0
                val isPositive = change >= 0
                val changeColor = if (isPositive) Color(0xFF00FF87) else Color(0xFFFF6B6B)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated change indicator
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
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
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
                            color = changeColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothChartCard(
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
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Glass morphism background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
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
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Chart
            if (isLoading) {
                SmoothChartShimmer()
            } else {
                SmoothStockChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Modern time frame selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeFrames.forEach { timeFrame ->
                    val isSelected = timeFrame == selectedTimeFrame
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                color = if (isSelected) {
                                    Color.White.copy(alpha = 0.3f)
                                } else {
                                    Color.White.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onTimeFrameSelected(timeFrame) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeFrame,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothStockChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No chart data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
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
    val chartColor = if (isPositive) Color(0xFF00FF87) else Color(0xFFFF6B6B)
    
    // Animation for chart drawing
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "chart_animation"
    )
    
    Canvas(modifier = modifier) {
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
        
        // Draw gradient fill
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
                    chartColor.copy(alpha = 0.4f),
                    chartColor.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )
        
        // Draw glow effect behind the line
        drawPath(
            path = path,
            color = chartColor.copy(alpha = 0.3f),
            style = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw main chart line
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw animated points
        animatedPoints.forEachIndexed { index, point ->
            val pointAlpha = if (index == animatedPoints.size - 1) 1f else 0.6f
            
            // Outer glow
            drawCircle(
                color = chartColor.copy(alpha = 0.3f * pointAlpha),
                radius = 8.dp.toPx(),
                center = point
            )
            // Main point
            drawCircle(
                color = chartColor.copy(alpha = pointAlpha),
                radius = 4.dp.toPx(),
                center = point
            )
            // Inner highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.8f * pointAlpha),
                radius = 2.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
fun GlassStatisticsCard(
    stockData: StockData?,
    stockDetails: StockDetails?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Glass morphism background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
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
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (isLoading) {
                ModernStatisticsShimmer()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ModernStatisticItem(
                            label = "Open",
                            value = stockData?.open?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatisticItem(
                            label = "Previous Close",
                            value = stockData?.previousClose?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ModernStatisticItem(
                            label = "Day High",
                            value = stockData?.high?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatisticItem(
                            label = "Day Low",
                            value = stockData?.low?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ModernStatisticItem(
                            label = "Volume",
                            value = stockData?.volume?.let { formatVolume(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatisticItem(
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
fun ModernStatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
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
fun ModernTradingButtons(
    stockSymbol: String,
    stockPrice: Double,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sell Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFEE5A6F)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clickable { onSellClick() }
                .clip(RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = null,
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00FF87),
                            Color(0xFF60EFFF)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clickable { onBuyClick() }
                .clip(RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
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

// Enhanced Shimmer Components
@Composable
fun SmoothChartShimmer() {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f)
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
    )
}

@Composable
fun ModernStatisticsShimmer() {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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