package com.example.stockit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.data.model.Stock
import com.example.stockit.data.repository.StockRepository
import kotlinx.coroutines.launch

class StockDetailViewModel(private val stockRepository: StockRepository) : ViewModel() {

    private var _stock: Stock? = null
    val stock: Stock?
        get() = _stock

    fun fetchStockDetails(stockId: String) {
        viewModelScope.launch {
            _stock = stockRepository.getStockById(stockId)
        }
    }
}