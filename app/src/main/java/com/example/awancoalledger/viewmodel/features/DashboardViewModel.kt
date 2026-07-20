package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.Calendar
import kotlin.math.absoluteValue

data class RecentActivity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val partyName: String,
    val partyType: PartyType,
    val amount: Double,
    val date: Long,
    val isPayment: Boolean,
    val entry: LedgerEntry? = null,
    val payment: Payment? = null
)

class DashboardViewModel(
    private val repository: LedgerRepository,
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val firebaseManager: FirebaseManager
) : ViewModel() {
    
    val ownerName = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getOwnerName() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getOwnerName())

    // Meta / Sync Status
    val syncStatus = MutableStateFlow(SyncStatus.LocalOnly) // simplify for now
    val lastSyncTime = MutableStateFlow<Long?>(null)

    fun forceSync() {
        syncManager.startSync()
    }

    private fun getBalance(details: PartyWithDetails): Double {
        val totalTruckValue = details.entries.sumOf {
            ((it.weight ?: 0.0) * (it.rate ?: 0.0)) + (it.fare ?: 0.0)
        }
        val totalTheyPaid = details.payments.filter { it.type == PaymentType.THEY_PAID }.sumOf { it.amount }
        val totalIPaid = details.payments.filter { it.type == PaymentType.I_PAID }.sumOf { it.amount }

        return if (details.party.type == PartyType.BUYER) {
            totalTruckValue - totalTheyPaid + totalIPaid
        } else {
            totalTruckValue - totalIPaid + totalTheyPaid
        }
    }

    val allPartiesWithDetails = repository.getAllPartiesWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partiesCount = allPartiesWithDetails.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalReceivable = allPartiesWithDetails.map { detailsList ->
        detailsList.sumOf { details ->
            val balance = getBalance(details)
            if (details.party.type == PartyType.BUYER) {
                if (balance > 0) balance else 0.0 
            } else {
                if (balance < 0) balance.absoluteValue else 0.0 
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayable = allPartiesWithDetails.map { detailsList ->
        detailsList.sumOf { details ->
            val balance = getBalance(details)
            if (details.party.type == PartyType.BUYER) {
                if (balance < 0) balance.absoluteValue else 0.0 
            } else {
                if (balance > 0) balance else 0.0 
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netMarketCredit = combine(totalReceivable, totalPayable) { rec, pay -> rec - pay }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentActivity = allPartiesWithDetails.map { detailsList ->
        val activities = mutableListOf<RecentActivity>()
        detailsList.forEach { details ->
            details.entries.forEach { entries ->
                activities.add(RecentActivity(
                    partyName = details.party.name,
                    partyType = details.party.type,
                    amount = ((entries.weight ?: 0.0) * (entries.rate ?: 0.0)) + (entries.fare ?: 0.0),
                    date = entries.date,
                    isPayment = false,
                    entry = entries
                ))
            }
            details.payments.forEach { payment ->
                activities.add(RecentActivity(
                    partyName = details.party.name,
                    partyType = details.party.type,
                    amount = payment.amount,
                    date = payment.date,
                    isPayment = true,
                    payment = payment
                ))
            }
        }
        activities.sortedByDescending { it.date }.take(10)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInventoryWeight = repository.getAllStocks().map { stocks ->
        stocks.sumOf { it.totalWeight }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpenses = repository.getAllExpenses().map { expenses ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        expenses.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaysExpenses = repository.getAllExpenses().map { expenses ->
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        expenses.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.DAY_OF_YEAR) == today && cal.get(Calendar.YEAR) == year
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeRemindersCount = repository.getActiveReminders()
        .map { it.count { r -> !r.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notesCount = repository.getAllNotes()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // Note: To perfectly decouple Vehicle from Dashboard, we might expose these through VehicleViewModel
    // or keep a simple flow. Here we just return 0.0 for now, or you can copy the Vehicle logic.
    val kmsRemainingPrimary = MutableStateFlow(0.0)
    val nextOilChangePrimary = MutableStateFlow(0.0)
}

enum class SyncStatus { LocalOnly, Syncing, Synced, Error }
