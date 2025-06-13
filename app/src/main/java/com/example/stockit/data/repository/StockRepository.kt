package com.example.stockit.data.repository

import com.example.stockit.data.model.Stock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StockRepository {

    private val _portfolioStocks = MutableStateFlow<List<Stock>>(emptyList())

    suspend fun getStocks(): List<Stock> {
        return emptyList()
    }

    suspend fun getStockById(stockId: String): Stock? {
        return null
    }

    fun getPortfolioStocks(): Flow<List<Stock>> {
        return _portfolioStocks.asStateFlow()
    }

    suspend fun addStockToPortfolio(stock: Stock) {
        val currentStocks = _portfolioStocks.value.toMutableList()
        if (!currentStocks.contains(stock)) {
            currentStocks.add(stock)
            _portfolioStocks.value = currentStocks
        }
    }

    suspend fun removeStockFromPortfolio(stock: Stock) {
        val currentStocks = _portfolioStocks.value.toMutableList()
        currentStocks.remove(stock)
        _portfolioStocks.value = currentStocks
    }
}