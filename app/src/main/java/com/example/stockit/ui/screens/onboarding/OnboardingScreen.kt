package com.example.stockit.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.stockit.ui.theme.StockItTheme

@Composable
fun OnboardingScreen(onNext: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome to StockIt!")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Your personal stock market assistant.")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onNext) {
                Text(text = "Get Started")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    StockItTheme {
        OnboardingScreen(onNext = {})
    }
}