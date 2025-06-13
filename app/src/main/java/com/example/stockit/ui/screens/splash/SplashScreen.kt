package com.example.stockit.ui.screens.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stockit.ui.theme.StockItTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToNext: () -> Unit = {}) {
    // Auto-navigate after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateToNext()
    }
    
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "StockIt",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    StockItTheme {
        SplashScreen()
    }
}