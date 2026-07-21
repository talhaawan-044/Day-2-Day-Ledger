package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import com.example.awancoalledger.viewmodel.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import java.util.Calendar
import kotlin.math.absoluteValue



class DashboardViewModel(
    private val repository: LedgerRepository,
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val firebaseManager: FirebaseManager
) : ViewModel() {
    
    val ownerName = settingsRepository.getOwnerNameFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getOwnerName())

    // Notifications
    val notifications = repository.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount = repository.getUnreadNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // Meta / Sync Status
    private val _syncStatus = MutableStateFlow(SyncStatus.LocalOnly)
    val syncStatus = _syncStatus.asStateFlow()

    val syncErrorMessage = MutableStateFlow<String?>(null)

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    init {
        viewModelScope.launch {
            syncManager.syncErrors.collect { error ->
                syncErrorMessage.value = error
                _syncStatus.value = SyncStatus.Error
            }
        }
        
        viewModelScope.launch {
            firebaseManager.currentUser.collect { user ->
                if (user != null) {
                    _syncStatus.value = SyncStatus.Synced
                } else {
                    _syncStatus.value = SyncStatus.LocalOnly
                }
            }
        }
    }

    fun forceSync() {
        if (firebaseManager.currentUser.value == null) {
            _syncStatus.value = SyncStatus.LocalOnly
            syncErrorMessage.value = null
            return
        }
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            syncErrorMessage.value = null
            try {
                syncManager.uploadAll()
                _syncStatus.value = SyncStatus.Synced
                _lastSyncTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Sync failed", e)
                _syncStatus.value = SyncStatus.Error
            }
        }
    }


    val allPartiesWithDetails = repository.getAllPartiesWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partiesCount = allPartiesWithDetails.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalReceivable = allPartiesWithDetails.map { detailsList ->
        detailsList.sumOf { details ->
            val balance = details.getBalance()
            if (details.party.type == PartyType.BUYER) {
                if (balance > 0) balance else 0.0 
            } else {
                if (balance < 0) balance.absoluteValue else 0.0 
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayable = allPartiesWithDetails.map { detailsList ->
        detailsList.sumOf { details ->
            val balance = details.getBalance()
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
