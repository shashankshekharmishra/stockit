package com.example.stockit.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stockit.ui.components.StockCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StockIt") },
                actions = {
                    IconButton(onClick = { /* TODO: Add action */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = "Market Overview", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            // Here you would typically fetch and display a list of stocks
            StockCard(stockName = "AAPL", stockPrice = "$150.00", onClick = { /* TODO */ })
            StockCard(stockName = "GOOGL", stockPrice = "$2800.00", onClick = { /* TODO */ })
            StockCard(stockName = "AMZN", stockPrice = "$3400.00", onClick = { /* TODO */ })
        }
    }
}