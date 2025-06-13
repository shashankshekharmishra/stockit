package com.example.stockit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.data.model.Stock
import com.example.stockit.data.repository.StockRepository
import kotlinx.coroutines.launch

class PortfolioViewModel(private val stockRepository: StockRepository) : ViewModel() {

    val portfolioStocks = stockRepository.getPortfolioStocks()

    fun addStockToPortfolio(stock: Stock) {
        viewModelScope.launch {
            stockRepository.addStockToPortfolio(stock)
        }
    }

    fun removeStockFromPortfolio(stock: Stock) {
        viewModelScope.launch {
            stockRepository.removeStockFromPortfolio(stock)
        }
    }
}