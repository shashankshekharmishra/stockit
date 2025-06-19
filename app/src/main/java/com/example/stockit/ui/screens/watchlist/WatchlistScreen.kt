package com.example.stockit.ui.screens.watchlist

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.ui.geometry.Offset
import com.example.stockit.network.WatchlistStock
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: WatchlistViewModel = hiltViewModel()
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
    
    // Background retry mechanism - retry every 2 seconds if watchlist is empty for authenticated users
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            while (true) {
                delay(2000) // Wait 2 seconds
                
                // Check if we need to retry watchlist data
                if (uiState.watchlistStocks.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                    viewModel.retryWatchlistInBackground()
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
            WatchlistHeader(
                isAuthenticated = uiState.isAuthenticated,
                stockCount = uiState.watchlistStocks.size,
                onRefresh = { viewModel.refreshWatchlist() },
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.offset(y = headerOffset.dp)
            )
            
            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                when {
                    !uiState.isAuthenticated -> {
                        NotAuthenticatedMessage()
                    }
                    uiState.isLoading -> {
                        WatchlistLoadingState()
                    }
                    uiState.watchlistStocks.isEmpty() -> {
                        EmptyWatchlistMessage()
                    }
                    else -> {
                        WatchlistContent(
                            stocks = uiState.watchlistStocks,
                            onStockClick = onStockClick,
                            onRemoveStock = { symbol -> viewModel.removeFromWatchlist(symbol) }
                        )
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
fun WatchlistHeader(
    isAuthenticated: Boolean,
    stockCount: Int,
    onRefresh: () -> Unit,
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
                .height(72.dp) // Match HomeScreen header height
                .background(
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp) // Match HomeScreen corner radius
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
                .padding(horizontal = 20.dp, vertical = 12.dp), // Match HomeScreen padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Watchlist Icon (left side) - consistent with home's profile button
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
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Watchlist",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Title (center) - consistent styling
            Text(
                text = "My Watchlist",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                fontSize = 20.sp
            )
            
            // Refresh Button (right side) - consistent with home
            if (isAuthenticated) {
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
                            contentDescription = "Refresh watchlist",
                            tint = Color.White,
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
fun WatchlistContent(
    stocks: List<WatchlistStock>,
    onStockClick: (String) -> Unit,
    onRemoveStock: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(
            items = stocks,
            key = { it.symbol }
        ) { stock ->
            WatchlistStockCard(
                stock = stock,
                onClick = { onStockClick(stock.symbol) },
                onRemove = { onRemoveStock(stock.symbol) }
            )
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistStockCard(
    stock: WatchlistStock,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val change = stock.change ?: 0.0
    val changePercent = stock.changePercent ?: 0.0
    val isPositive = change >= 0
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
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
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
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Price and change
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        stock.price?.let { price ->
                            Text(
                                text = "₹${String.format("%.2f", price)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", change)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = changeColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${if (isPositive) "+" else ""}${String.format("%.2f", changePercent)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = changeColor.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove from watchlist",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
                    text = "Please log in to view your watchlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmptyWatchlistMessage() {
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
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF6366F1)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Empty Watchlist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start tracking stocks by adding them to your watchlist from stock detail pages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WatchlistLoadingState() {
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
                text = "Loading watchlist...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}