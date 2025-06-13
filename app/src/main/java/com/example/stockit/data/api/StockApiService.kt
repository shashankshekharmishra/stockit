package com.example.stockit.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import com.example.stockit.data.model.Stock

interface StockApiService {
    @GET("stocks")
    suspend fun getAllStocks(): List<Stock>

    @GET("stocks/{id}")
    suspend fun getStockById(@Path("id") stockId: String): Stock
}