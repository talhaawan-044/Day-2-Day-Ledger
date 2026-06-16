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
        if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LedgerViewModel(repository, settingsRepository, scheduler, firebaseManager, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
