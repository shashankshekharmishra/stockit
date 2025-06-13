package com.example.stockit.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stockit.ui.theme.StockItTheme

@Composable
fun ProfileScreen() {
    StockItTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(text = "Profile", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                // Add more UI elements here for user profile details
                Text(text = "User Name: John Doe")
                Text(text = "Email: john.doe@example.com")
                // Additional profile information can be added here
            }
        }
    }
}