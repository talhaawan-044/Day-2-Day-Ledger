package com.example.awancoalledger.viewmodel.features

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import com.example.awancoalledger.utils.ExportUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val repository: LedgerRepository,
    private val syncManager: SyncManager,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    val allExpenses = repository.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(amount: Double, category: ExpenseCategory, note: String?, date: Long) {
        viewModelScope.launch {
            val expense = Expense(amount = amount, category = category, note = note, date = date)
            repository.insertExpense(expense)
            syncManager.uploadExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            val updated = expense.copy(lastUpdated = System.currentTimeMillis())
            repository.insertExpense(updated)
            syncManager.uploadExpense(updated)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val deleted = expense.copy(isDeleted = true, lastUpdated = System.currentTimeMillis())
            repository.insertExpense(deleted)
            syncManager.uploadExpense(deleted)
            syncManager.deleteExpense(deleted)
        }
    }

    fun exportExpensesToNativePdf(context: Context, expenses: List<Expense>, title: String) {
        viewModelScope.launch {
            ExportUtils.generateExpensesPdf(
                context = context,
                expenses = expenses,
                title = title,
                businessName = settingsRepository.getBusinessName(),
                ownerName = settingsRepository.getOwnerName(),
                logoUri = settingsRepository.getCompanyLogoUri(),
                signatureUri = settingsRepository.getSignatureUri()
            )
        }
    }
}
