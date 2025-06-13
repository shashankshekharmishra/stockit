package com.example.stockit.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val portfolio: List<String> // List of stock IDs or symbols
)