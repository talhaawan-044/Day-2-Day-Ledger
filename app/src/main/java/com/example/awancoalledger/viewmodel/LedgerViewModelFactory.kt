package com.example.awancoalledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.data.SettingsRepository
import com.example.awancoalledger.utils.ReminderScheduler
import com.example.awancoalledger.data.FirebaseManager
import com.example.awancoalledger.data.SyncManager

class LedgerViewModelFactory(
    private val repository: LedgerRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val firebaseManager: FirebaseManager,
    private val syncManager: SyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LedgerViewModel::class.java) -> LedgerViewModel(repository, settingsRepository, scheduler, firebaseManager, syncManager) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.DashboardViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.DashboardViewModel(repository, settingsRepository, syncManager, firebaseManager) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.PartiesViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.PartiesViewModel(repository, settingsRepository, syncManager) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.AuthViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.AuthViewModel(firebaseManager, syncManager, repository, settingsRepository) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.SettingsViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.SettingsViewModel(settingsRepository, syncManager, repository, firebaseManager) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.LedgerDetailViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.LedgerDetailViewModel(repository, syncManager, settingsRepository) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.StockViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.StockViewModel(repository, syncManager, settingsRepository) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.VehicleViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.VehicleViewModel(repository, syncManager, settingsRepository) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.ExpensesViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.ExpensesViewModel(repository, syncManager, settingsRepository) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.RemindersViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.RemindersViewModel(repository, scheduler, syncManager) as T
            modelClass.isAssignableFrom(com.example.awancoalledger.viewmodel.features.NotesViewModel::class.java) -> com.example.awancoalledger.viewmodel.features.NotesViewModel(repository, syncManager, settingsRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
