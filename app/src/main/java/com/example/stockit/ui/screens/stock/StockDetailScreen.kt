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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max
import kotlin.math.min

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
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "content_alpha"
    )
    
    // Initialize authentication
    LaunchedEffect(Unit) {
        viewModel.initializeAuth(context)
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
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0)
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stockSymbol,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            uiState.stockData?.let { stock ->
                                Text(
                                    text = stock.name ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Add to watchlist */ }) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = "Add to Watchlist",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { /* TODO: Share */ }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
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
                        .shadow(8.dp)
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Price Card
                item {
                    PriceCard(
                        stockData = uiState.stockData,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                
                // Chart Card
                item {
                    ChartCard(
                        chartData = uiState.chartData,
                        selectedTimeFrame = selectedTimeFrame,
                        onTimeFrameSelected = { selectedTimeFrame = it },
                        isLoading = uiState.isLoadingChart,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                
                // Key Statistics
                item {
                    KeyStatisticsCard(
                        stockData = uiState.stockData,
                        stockDetails = uiState.stockDetails,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                
                // Trading Buttons
                item {
                    TradingButtonsCard(
                        stockSymbol = stockSymbol,
                        stockPrice = uiState.stockData?.price ?: 0.0,
                        onBuyClick = { showBuyDialog = true },
                        onSellClick = { showSellDialog = true },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                
                // Add some bottom spacing
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun PriceCard(
    stockData: StockData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            if (isLoading) {
                PriceCardShimmer()
            } else if (stockData != null) {
                Column {
                    Text(
                        text = "₹${String.format("%.2f", stockData.price)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        fontSize = 36.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val change = stockData.change ?: 0.0
                    val changePercent = stockData.changePercent ?: 0.0
                    val isPositive = change >= 0
                    val changeColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", change)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = changeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${if (isPositive) "+" else ""}${String.format("%.2f", changePercent)}%)",
                            style = MaterialTheme.typography.titleMedium,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    stockData.volume?.let { volume ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Volume: ${formatVolume(volume)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartCard(
    chartData: List<ChartPoint>,
    selectedTimeFrame: String,
    onTimeFrameSelected: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFrames = listOf("1W", "1M", "3M", "Y")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Chart
            if (isLoading) {
                ChartShimmer()
            } else {
                StockChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time frame selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                timeFrames.forEach { timeFrame ->
                    val isSelected = timeFrame == selectedTimeFrame
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF8B5CF6)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFF1F5F9),
                                            Color(0xFFF1F5F9)
                                        )
                                    )
                                }
                            )
                            .clickable { onTimeFrameSelected(timeFrame) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = timeFrame,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockChart(
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
                    tint = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No chart data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try selecting a different time frame",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
        return
    }
    
    // Debug info
    println("📊 Drawing chart with ${data.size} points")
    if (data.isNotEmpty()) {
        val minPrice = data.minOf { it.price }
        val maxPrice = data.maxOf { it.price }
        println("📊 Price range: $minPrice - $maxPrice")
        println("📊 First timestamp: ${data.first().timestamp}")
        println("📊 Last timestamp: ${data.last().timestamp}")
    }
    
    val isPositive = if (data.size >= 2) {
        data.last().price >= data.first().price
    } else {
        true
    }
    val chartColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20.dp.toPx()
        
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        
        if (data.size < 2) {
            // Draw a single point if we only have one data point
            if (data.size == 1) {
                val centerX = width / 2
                val centerY = height / 2
                
                // Draw a circle for single point
                drawCircle(
                    color = chartColor,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                
                // Draw horizontal line to show the price level
                drawLine(
                    color = chartColor.copy(alpha = 0.5f),
                    start = Offset(padding, centerY),
                    end = Offset(width - padding, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            return@Canvas
        }
        
        val minPrice = data.minOf { it.price }
        val maxPrice = data.maxOf { it.price }
        val priceRange = maxPrice - minPrice
        
        if (priceRange == 0.0) {
            // Draw a horizontal line if all prices are the same
            val y = height / 2
            drawLine(
                color = chartColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw points along the line
            data.forEachIndexed { index, _ ->
                val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
                drawCircle(
                    color = chartColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            return@Canvas
        }
        
        val points = data.mapIndexed { index, point ->
            val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
            val y = padding + ((maxPrice - point.price) / priceRange * chartHeight).toFloat()
            Offset(x, y)
        }
        
        // Draw gradient fill
        val path = android.graphics.Path().apply {
            moveTo(points.first().x, height - padding)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, height - padding)
            close()
        }
        
        drawPath(
            path = path.asComposePath(),
            brush = Brush.verticalGradient(
                colors = listOf(
                    chartColor.copy(alpha = 0.3f),
                    chartColor.copy(alpha = 0.05f)
                ),
                startY = 0f,
                endY = height
            )
        )
        
        // Draw chart line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = chartColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Draw points
        points.forEach { point ->
            drawCircle(
                color = chartColor,
                radius = 3.dp.toPx(),
                center = point
            )
            // Draw a white center for better visibility
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
fun KeyStatisticsCard(
    stockData: StockData?,
    stockDetails: StockDetails?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
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
                Text(
                    text = "Key Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                
                TextButton(onClick = { /* TODO: View all */ }) {
                    Text(
                        text = "View All",
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                KeyStatisticsShimmer()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatisticItem(
                            label = "Open",
                            value = stockData?.open?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        StatisticItem(
                            label = "Closed",
                            value = stockData?.previousClose?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatisticItem(
                            label = "High",
                            value = stockData?.high?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        StatisticItem(
                            label = "Low",
                            value = stockData?.low?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatisticItem(
                            label = "Volume",
                            value = stockData?.volume?.let { formatVolume(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        StatisticItem(
                            label = "Mkt Cap",
                            value = stockDetails?.marketCap?.let { formatMarketCap(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    stockDetails?.let { details ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticItem(
                                label = "P/E",
                                value = details.peRatio?.let { String.format("%.2f", it) } ?: "—",
                                modifier = Modifier.weight(1f)
                            )
                            StatisticItem(
                                label = "EPS",
                                value = details.eps?.let { "₹${String.format("%.2f", it)}" } ?: "—",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TradingButtonsCard(
    stockSymbol: String,
    stockPrice: Double,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sell Button
            Button(
                onClick = onSellClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFEF4444),
                                    Color(0xFFDC2626)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
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
                            text = "Sell Shares",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            // Buy Button
            Button(
                onClick = onBuyClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF10B981),
                                    Color(0xFF059669)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
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
                            text = "Buy Shares",
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

// Shimmer components
@Composable
fun PriceCardShimmer() {
    Column {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
                .background(
                    Color(0xFFF1F5F9),
                    RoundedCornerShape(8.dp)
                )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(24.dp)
                .background(
                    Color(0xFFF8FAFC),
                    RoundedCornerShape(6.dp)
                )
        )
    }
}

@Composable
fun ChartShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Color(0xFFF1F5F9),
                RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun KeyStatisticsShimmer() {
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
                                .height(16.dp)
                                .background(
                                    Color(0xFFF1F5F9),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(20.dp)
                                .background(
                                    Color(0xFFF8FAFC),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

// Utility functions
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