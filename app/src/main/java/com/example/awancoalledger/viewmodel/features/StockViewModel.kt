package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.asStateFlow

class StockViewModel(
    private val repository: LedgerRepository,
    private val syncManager: SyncManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isGridView = kotlinx.coroutines.flow.MutableStateFlow(settingsRepository.isStockGridView())
    val isGridView: kotlinx.coroutines.flow.StateFlow<Boolean> = _isGridView
    fun toggleGridView(isGrid: Boolean) { _isGridView.value = isGrid; settingsRepository.setStockGridView(isGrid) }

    val totalInventoryWeight = repository.getAllStocks().map { stocks -> stocks.sumOf { it.totalWeight } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val stocks: StateFlow<List<Stock>> = repository.getAllStocks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedStockId = MutableStateFlow(0)
    fun selectStock(id: Int) { selectedStockId.value = id }
    
    // There is no getStockWithEntries in LedgerRepository currently, so we simulate it or fetch entries directly
    val stockEntries: StateFlow<List<StockEntry>> = selectedStockId.flatMapLatest { id ->
        repository.getEntriesForStock(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addStockEntry(mine: String, weight: Double, warehouse: String) {
        viewModelScope.launch {
            val stock = repository.getStockByMineName(mine) ?: Stock(mineName = mine)
            val stockId = if (stock.id == 0) repository.insertStock(stock).toInt() else stock.id
            
            val newTotalWeight = stock.totalWeight + weight
            val newPeakWeight = maxOf(stock.peakWeight, newTotalWeight)
            
            repository.updateStock(stock.copy(
                id = stockId,
                totalWeight = newTotalWeight,
                peakWeight = newPeakWeight,
                lastWarehouse = warehouse
            ))
            
            val entry = StockEntry(stockId = stockId, weight = weight, warehouse = warehouse)
            repository.insertStockEntry(entry)
            
            syncManager.uploadStock(stock.copy(id = stockId, totalWeight = newTotalWeight, peakWeight = newPeakWeight, lastWarehouse = warehouse))
            syncManager.uploadStockEntry(entry)
        }
    }

    fun deleteStockEntry(entry: StockEntry) {
        viewModelScope.launch {
            repository.deleteStockEntry(entry)
            syncManager.deleteStockEntry(entry)
            
            // Also update stock total weight
            val stock = repository.getStockById(entry.stockId)
            if (stock != null) {
                val newTotalWeight = stock.totalWeight - entry.weight
                val updatedStock = stock.copy(totalWeight = newTotalWeight)
                repository.updateStock(updatedStock)
                syncManager.uploadStock(updatedStock)
            }
        }
    }

    fun deleteStock(stock: Stock) {
        viewModelScope.launch {
            repository.deleteStock(stock)
            syncManager.deleteStock(stock)
        }
    }
}
