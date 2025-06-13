package com.example.stockit.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stockit.ui.theme.StockItTheme

@Composable
fun SignInSuccessScreen(onContinue: () -> Unit = {}) {
    // Main content of the Sign In Success screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Sign In Successful!",
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = onContinue,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Continue to Home")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInSuccessScreenPreview() {
    StockItTheme {
        SignInSuccessScreen(onContinue = {})
    }
}