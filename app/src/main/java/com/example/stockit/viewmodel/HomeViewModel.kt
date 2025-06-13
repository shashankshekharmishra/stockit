package com.example.stockit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.data.model.Stock
import com.example.stockit.data.repository.StockRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val stockRepository: StockRepository) : ViewModel() {

    private val _stocks = mutableListOf<Stock>()
    val stocks: List<Stock> get() = _stocks

    init {
        fetchStocks()
    }

    private fun fetchStocks() {
        viewModelScope.launch {
            _stocks.clear()
            _stocks.addAll(stockRepository.getStocks())
        }
    }
}