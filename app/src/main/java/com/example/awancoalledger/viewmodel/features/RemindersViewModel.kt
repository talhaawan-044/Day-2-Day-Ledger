package com.example.awancoalledger.viewmodel.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import com.example.awancoalledger.utils.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val repository: LedgerRepository,
    private val scheduler: ReminderScheduler,
    private val syncManager: SyncManager
) : ViewModel() {

    val allReminders = repository.getAllReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeReminders = repository.getActiveReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completedReminders = repository.getCompletedReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(title: String, note: String?, dueDate: Long?, priority: ReminderPriority, category: ReminderCategory) {
        viewModelScope.launch {
            val allLists = repository.getAllReminderLists().first()
            val listId = if (allLists.isEmpty()) {
                repository.insertReminderList(ReminderList(name = "My Reminders", color = 0xFF2196F3.toInt(), iconName = "List", order = 0, isDefault = true))
            } else {
                allLists.first().id
            }

            val reminder = Reminder(
                title = title, note = note,
                dueDate = dueDate, remindTime = dueDate,
                priority = priority, category = category,
                listId = listId.toInt()
            )
            val id = repository.insertReminder(reminder)
            val saved = reminder.copy(id = id.toInt())
            if (dueDate != null) scheduler.schedule(saved)
            syncManager.uploadReminder(saved)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.insertReminder(reminder)
            if (reminder.dueDate != null && !reminder.isCompleted) {
                scheduler.schedule(reminder)
            } else {
                scheduler.cancel(reminder)
            }
            syncManager.uploadReminder(reminder)
        }
    }

    fun toggleReminderCompletion(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted, lastUpdated = System.currentTimeMillis())
            repository.insertReminder(updated)
            if (updated.isCompleted) {
                scheduler.cancel(updated)
            } else if (updated.dueDate != null) {
                scheduler.schedule(updated)
            }
            syncManager.uploadReminder(updated)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            scheduler.cancel(reminder)
            syncManager.deleteReminder(reminder)
        }
    }
}
