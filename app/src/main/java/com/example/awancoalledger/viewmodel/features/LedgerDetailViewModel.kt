package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LedgerDetailViewModel(
    private val repository: LedgerRepository,
    private val syncManager: SyncManager,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    val countryConfig = settingsRepository.getSettingsFlow().map { val code = settingsRepository.getDefaultCountryCode(); SUPPORTED_COUNTRIES.find { it.code == code } ?: CountryConfig("Custom", code, 15) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SUPPORTED_COUNTRIES.find { it.code == settingsRepository.getDefaultCountryCode() } ?: CountryConfig("Custom", settingsRepository.getDefaultCountryCode(), 15))

    private val _selectedPartyId = MutableStateFlow<Int?>(null)

    val partyDetails: StateFlow<PartyWithDetails?> = _selectedPartyId.flatMapLatest { id ->
        if (id != null) repository.getPartyWithDetails(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectParty(id: Int) {
        _selectedPartyId.value = id
    }

    fun updateParty(party: Party) {
        viewModelScope.launch {
            repository.upsertParty(party)
            syncManager.uploadParty(party)
        }
    }

    fun addEntry(partyId: Int, truck: String?, mine: String?, warehouse: String?, weight: Double?, rate: Double?, fare: Double?, adv: Double?, date: Long) {
        viewModelScope.launch {
            val entry = LedgerEntry(partyId = partyId, truckNumber = truck, mine = mine, warehouse = warehouse, weight = weight, rate = rate, fare = fare, advPayment = adv, date = date)
            repository.insertEntry(entry)
            syncManager.uploadEntry(entry)
        }
    }

    fun updateEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            val updated = entry.copy(lastUpdated = System.currentTimeMillis())
            repository.insertEntry(updated)
            syncManager.uploadEntry(updated)
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            val deleted = entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertEntry(deleted)
            syncManager.uploadEntry(deleted)
            syncManager.deleteEntry(deleted)
        }
    }

    fun addPayment(partyId: Int, amount: Double, type: PaymentType, note: String?, date: Long) {
        viewModelScope.launch {
            val p = Payment(partyId = partyId, amount = amount, note = note, type = type, date = date)
            repository.insertPayment(p)
            syncManager.uploadPayment(p)
        }
    }

    fun updatePayment(payment: Payment) {
        viewModelScope.launch {
            val updated = payment.copy(lastUpdated = System.currentTimeMillis())
            repository.insertPayment(updated)
            syncManager.uploadPayment(updated)
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            val deleted = payment.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertPayment(deleted)
            syncManager.uploadPayment(deleted)
            syncManager.deletePayment(deleted)
        }
    }

    fun getBalance(details: PartyWithDetails): Double {
        val totalTruckValue = details.entries.sumOf {
            ((it.weight ?: 0.0) * (it.rate ?: 0.0)) + (it.fare ?: 0.0)
        }
        val totalTheyPaid = details.payments.filter { it.type == PaymentType.THEY_PAID }.sumOf { it.amount }
        val totalIPaid = details.payments.filter { it.type == PaymentType.I_PAID }.sumOf { it.amount }
        
        return if (details.party.type == PartyType.BUYER) {
            totalTruckValue + totalIPaid - totalTheyPaid
        } else {
            totalTruckValue + totalTheyPaid - totalIPaid
        }
    }
}
