package com.example.awancoalledger.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.awancoalledger.utils.BackupData

class LedgerRepository(private val ledgerDao: LedgerDao) {

    // Parties
    fun getAllParties(): Flow<List<Party>> = ledgerDao.getAllParties()
    fun getAllPartiesWithDetails(): Flow<List<PartyWithDetails>> = ledgerDao.getAllPartiesWithDetails().map { list ->
        list.map { details ->
            details.copy(
                entries = details.entries.filter { !it.isDeleted },
                payments = details.payments.filter { !it.isDeleted }
            )
        }
    }
    fun getPartyWithDetails(partyId: Int): Flow<PartyWithDetails?> = ledgerDao.getPartyWithDetails(partyId).map { details ->
        details?.copy(
            entries = details.entries.filter { !it.isDeleted },
            payments = details.payments.filter { !it.isDeleted }
        )
    }
    suspend fun getPartyById(id: Int): Party? = ledgerDao.getPartyById(id)
    suspend fun upsertParty(party: Party) = ledgerDao.upsertParty(party)
    suspend fun deleteParty(party: Party) = ledgerDao.upsertParty(party.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Ledger Entries
    fun getEntriesForParty(partyId: Int): Flow<List<LedgerEntry>> = ledgerDao.getEntriesForParty(partyId)
    fun getRecentEntries(): Flow<List<LedgerEntry>> = ledgerDao.getRecentEntries()
    suspend fun insertEntry(entry: LedgerEntry) = ledgerDao.insertEntry(entry)
    suspend fun deleteEntry(entry: LedgerEntry) = ledgerDao.insertEntry(entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Payments
    fun getPaymentsForParty(partyId: Int): Flow<List<Payment>> = ledgerDao.getPaymentsForParty(partyId)
    suspend fun insertPayment(payment: Payment) = ledgerDao.insertPayment(payment)
    suspend fun deletePayment(payment: Payment) = ledgerDao.insertPayment(payment.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Expenses
    fun getAllExpenses(): Flow<List<Expense>> = ledgerDao.getAllExpenses()
    suspend fun insertExpense(expense: Expense) = ledgerDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = ledgerDao.insertExpense(expense.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Reminder Lists
    fun getAllReminderLists(): Flow<List<ReminderList>> = ledgerDao.getAllReminderLists()
    suspend fun insertReminderList(list: ReminderList): Long = ledgerDao.insertReminderList(list)
    suspend fun deleteReminderList(list: ReminderList) = ledgerDao.insertReminderList(list.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Reminders
    fun getAllReminders(): Flow<List<Reminder>> = ledgerDao.getAllReminders()
    fun getActiveReminders(): Flow<List<Reminder>> = ledgerDao.getActiveReminders()
    fun getCompletedReminders(): Flow<List<Reminder>> = ledgerDao.getCompletedReminders()
    fun getRemindersForList(listId: Int): Flow<List<Reminder>> = ledgerDao.getRemindersForList(listId)
    fun getSubtasks(parentId: Int): Flow<List<Reminder>> = ledgerDao.getSubtasks(parentId)
    suspend fun getReminderById(id: Int): Reminder? = ledgerDao.getReminderById(id)
    suspend fun insertReminder(reminder: Reminder): Long = ledgerDao.insertReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = ledgerDao.insertReminder(reminder.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))
    suspend fun completeReminder(reminder: Reminder, scheduler: com.example.awancoalledger.utils.ReminderScheduler) {
        val updated = reminder.copy(isCompleted = true)
        ledgerDao.insertReminder(updated)
        scheduler.cancel(updated)
    }

    // Stocks
    fun getAllStocks(): Flow<List<Stock>> = ledgerDao.getAllStocks()
    suspend fun getStockByMineName(mineName: String): Stock? = ledgerDao.getStockByMineName(mineName)
    suspend fun getStockById(id: Int): Stock? = ledgerDao.getStockById(id)
    suspend fun insertStock(stock: Stock): Long = ledgerDao.insertStock(stock)
    suspend fun updateStock(stock: Stock) = ledgerDao.updateStock(stock)
    suspend fun deleteStock(stock: Stock) = ledgerDao.updateStock(stock.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Stock Entries
    fun getEntriesForStock(stockId: Int): Flow<List<StockEntry>> = ledgerDao.getEntriesForStock(stockId)
    suspend fun insertStockEntry(entry: StockEntry) = ledgerDao.insertStockEntry(entry)
    suspend fun deleteStockEntry(entry: StockEntry) = ledgerDao.insertStockEntry(entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Notes
    fun getAllNotes(): Flow<List<Note>> = ledgerDao.getAllNotes()
    fun getNotesInFolder(folderId: Int): Flow<List<Note>> = ledgerDao.getNotesInFolder(folderId)
    suspend fun insertNote(note: Note): Long = ledgerDao.insertNote(note)
    suspend fun updateNote(note: Note) = ledgerDao.updateNote(note)
    suspend fun deleteNote(note: Note) = ledgerDao.updateNote(note.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Folders
    fun getAllFolders(): Flow<List<Folder>> = ledgerDao.getAllFolders()
    suspend fun insertFolder(folder: Folder): Long = ledgerDao.insertFolder(folder)
    suspend fun deleteFolder(folder: Folder) = ledgerDao.insertFolder(folder.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Backup
    suspend fun getBackupData(): BackupData {
        return BackupData(
            parties = ledgerDao.getAllPartiesList(),
            entries = ledgerDao.getAllEntriesList(),
            payments = ledgerDao.getAllPaymentsList(),
            expenses = ledgerDao.getAllExpensesList(),
            reminderLists = ledgerDao.getAllReminderListsList(),
            reminders = ledgerDao.getAllRemindersList(),
            stocks = ledgerDao.getAllStocksList(),
            stockEntries = ledgerDao.getAllStockEntriesList(),
            notes = ledgerDao.getAllNotesList(),
            folders = ledgerDao.getAllFoldersList(),
            fuelEntries = ledgerDao.getAllFuelEntriesList(),
            maintenanceEntries = ledgerDao.getAllMaintenanceEntriesList(),
            vehicles = ledgerDao.getAllVehiclesList()
        )
    }

    @androidx.room.Transaction
    suspend fun restoreData(data: BackupData) {
        ledgerDao.clearAllData()
        data.parties.orEmpty().forEach { ledgerDao.upsertParty(it) }
        data.entries.orEmpty().forEach { ledgerDao.insertEntry(it) }
        data.payments.orEmpty().forEach { ledgerDao.insertPayment(it) }
        data.expenses.orEmpty().forEach { ledgerDao.insertExpense(it) }
        data.reminderLists.orEmpty().forEach { ledgerDao.insertReminderList(it) }
        data.reminders.orEmpty().forEach { ledgerDao.insertReminder(it) }
        data.stocks.orEmpty().forEach { ledgerDao.insertStock(it) }
        data.stockEntries.orEmpty().forEach { ledgerDao.insertStockEntry(it) }
        data.notes.orEmpty().forEach { ledgerDao.insertNote(it) }
        data.folders.orEmpty().forEach { ledgerDao.insertFolder(it) }
        data.fuelEntries.orEmpty().forEach { ledgerDao.insertFuelEntry(it) }
        data.maintenanceEntries.orEmpty().forEach { ledgerDao.insertMaintenanceEntry(it) }
        data.vehicles.orEmpty().forEach { ledgerDao.insertVehicle(it) }
    }

    // Vehicle Tracking
    fun getAllFuelEntries(): Flow<List<FuelEntry>> = ledgerDao.getAllFuelEntries()
    suspend fun getAllFuelEntriesList(): List<FuelEntry> = ledgerDao.getAllFuelEntriesList()
    suspend fun insertFuelEntry(entry: FuelEntry) = ledgerDao.insertFuelEntry(entry)
    suspend fun deleteFuelEntry(entry: FuelEntry) = ledgerDao.insertFuelEntry(entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    fun getAllMaintenanceEntries(): Flow<List<MaintenanceEntry>> = ledgerDao.getAllMaintenanceEntries()
    suspend fun getAllMaintenanceEntriesList(): List<MaintenanceEntry> = ledgerDao.getAllMaintenanceEntriesList()
    suspend fun insertMaintenanceEntry(entry: MaintenanceEntry) = ledgerDao.insertMaintenanceEntry(entry)
    suspend fun deleteMaintenanceEntry(entry: MaintenanceEntry) = ledgerDao.insertMaintenanceEntry(entry.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))

    // Vehicles
    fun getAllVehicles(): Flow<List<Vehicle>> = ledgerDao.getAllVehicles()
    suspend fun getVehicleById(id: Int): Vehicle? = ledgerDao.getVehicleById(id)
    suspend fun insertVehicle(vehicle: Vehicle): Long = ledgerDao.insertVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = ledgerDao.insertVehicle(vehicle.copy(isDeleted = true, lastUpdated = System.currentTimeMillis()))
    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>> = ledgerDao.getFuelEntriesForVehicle(vehicleId)
    fun getMaintenanceEntriesForVehicle(vehicleId: Int): Flow<List<MaintenanceEntry>> = ledgerDao.getMaintenanceEntriesForVehicle(vehicleId)

    suspend fun clearAllData() {
        ledgerDao.clearAllData()
    }
}
