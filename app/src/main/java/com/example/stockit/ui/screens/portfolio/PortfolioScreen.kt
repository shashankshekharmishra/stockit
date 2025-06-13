package com.example.stockit.ui.screens.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stockit.ui.components.StockCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Portfolio") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Placeholder for portfolio items
            Text(text = "Your Stocks", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            // Example of stock cards
            StockCard(stockName = "AAPL", stockPrice = "$150.00")
            StockCard(stockName = "GOOGL", stockPrice = "$2800.00")
            StockCard(stockName = "AMZN", stockPrice = "$3400.00")
        }
    }
}