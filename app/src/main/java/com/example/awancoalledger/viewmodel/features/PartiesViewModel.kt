package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PartiesViewModel(
    private val repository: LedgerRepository,
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _isGridView = MutableStateFlow(settingsRepository.isPartiesGridView())
    val isGridView = _isGridView.asStateFlow()
    fun toggleGridView(isGrid: Boolean) { _isGridView.value = isGrid; settingsRepository.setPartiesGridView(isGrid) }

    val countryConfig = settingsRepository.getCountryConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            SUPPORTED_COUNTRIES.find { it.code == settingsRepository.getDefaultCountryCode() } ?: CountryConfig("Custom", settingsRepository.getDefaultCountryCode(), 15))

    val allPartiesWithDetails = repository.getAllPartiesWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addParty(name: String, phone: String, address: String, type: PartyType) {
        viewModelScope.launch {
            val p = Party(name = name, phone = phone, address = address, type = type)
            repository.upsertParty(p)
            syncManager.uploadParty(p)
        }
    }

    fun updateParty(party: Party) {
        viewModelScope.launch {
            val updated = party.copy(lastUpdated = System.currentTimeMillis())
            repository.upsertParty(updated)
            syncManager.uploadParty(updated)
        }
    }

    fun deleteParty(party: Party) {
        viewModelScope.launch {
            val deleted = party.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.upsertParty(deleted)
            syncManager.uploadParty(deleted)
            syncManager.deleteParty(deleted)
        }
    }


}
