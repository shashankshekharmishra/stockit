package com.example.stockit.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.stockit.ui.theme.StockItTheme

@Composable
fun SignUpScreen(onSignUp: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Sign Up", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))
            // Add TextFields for user input (e.g., email, password, etc.)
            // Add a Sign Up button that calls onSignUp when clicked
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    StockItTheme {
        SignUpScreen(onSignUp = {})
    }
}