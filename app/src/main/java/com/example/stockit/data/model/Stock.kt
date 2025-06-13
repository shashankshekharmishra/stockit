package com.example.stockit.data.model

data class Stock(
    val id: String,
    val name: String,
    val symbol: String,
    val price: Double,
    val change: Double,
    val marketCap: Long,
    val description: String
)