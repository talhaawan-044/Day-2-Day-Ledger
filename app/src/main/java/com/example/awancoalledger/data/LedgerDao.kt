package com.example.awancoalledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM parties")
    fun getAllParties(): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getPartyById(id: Int): Party?

    @Query("SELECT * FROM parties WHERE syncId = :syncId LIMIT 1")
    suspend fun getPartyBySyncId(syncId: String): Party?

    @Transaction
    @Query("SELECT * FROM parties WHERE id = :partyId")
    fun getPartyWithDetails(partyId: Int): Flow<PartyWithDetails?>

    @Transaction
    @Query("SELECT * FROM parties")
    fun getAllPartiesWithDetails(): Flow<List<PartyWithDetails>>

    @Upsert
    suspend fun upsertParty(party: Party)

    @Delete
    suspend fun deleteParty(party: Party)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry)

    @Query("SELECT * FROM ledger_entries WHERE partyId = :partyId ORDER BY date DESC")
    fun getEntriesForParty(partyId: Int): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getEntryBySyncId(syncId: String): LedgerEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Query("SELECT * FROM payments WHERE partyId = :partyId ORDER BY date DESC")
    fun getPaymentsForParty(partyId: Int): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE syncId = :syncId LIMIT 1")
    suspend fun getPaymentBySyncId(syncId: String): Payment?

    @Query("SELECT * FROM ledger_entries ORDER BY date DESC LIMIT 5")
    fun getRecentEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries")
    fun getAllEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM payments")
    fun getAllPayments(): Flow<List<Payment>>

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE syncId = :syncId LIMIT 1")
    suspend fun getExpenseBySyncId(syncId: String): Expense?

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Reminder Lists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderList(list: ReminderList): Long

    @Query("SELECT * FROM reminder_lists ORDER BY `order` ASC")
    fun getAllReminderLists(): Flow<List<ReminderList>>

    @Query("SELECT * FROM reminder_lists WHERE syncId = :syncId LIMIT 1")
    suspend fun getReminderListBySyncId(syncId: String): ReminderList?

    @Delete
    suspend fun deleteReminderList(list: ReminderList)

    // Reminders Enhanced
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE syncId = :syncId LIMIT 1")
    suspend fun getReminderBySyncId(syncId: String): Reminder?

    @Query("SELECT * FROM reminders WHERE listId = :listId ORDER BY isCompleted ASC, priority DESC")
    fun getRemindersForList(listId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE parentId = :parentId")
    fun getSubtasks(parentId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1")
    fun getCompletedReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    fun getAllReminders(): Flow<List<Reminder>>

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    // Stocks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: Stock): Long

    @Update
    suspend fun updateStock(stock: Stock)

    @Query("SELECT * FROM stocks ORDER BY updatedAt DESC")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM stocks WHERE mineName = :mineName LIMIT 1")
    suspend fun getStockByMineName(mineName: String): Stock?

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Int): Stock?

    @Query("SELECT * FROM stocks WHERE syncId = :syncId LIMIT 1")
    suspend fun getStockBySyncId(syncId: String): Stock?

    @Delete
    suspend fun deleteStock(stock: Stock)

    // Stock Entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntry(entry: StockEntry)

    @Query("SELECT * FROM stock_entries WHERE stockId = :stockId ORDER BY date DESC")
    fun getEntriesForStock(stockId: Int): Flow<List<StockEntry>>

    @Query("SELECT * FROM stock_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getStockEntryBySyncId(syncId: String): StockEntry?

    @Delete
    suspend fun deleteStockEntry(entry: StockEntry)

    // Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("SELECT * FROM notes WHERE syncId = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): Note?

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, date DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, date DESC")
    fun getNotesInFolder(folderId: Int): Flow<List<Note>>

    @Delete
    suspend fun deleteNote(note: Note)

    // Folders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE syncId = :syncId LIMIT 1")
    suspend fun getFolderBySyncId(syncId: String): Folder?

    @Delete
    suspend fun deleteFolder(folder: Folder)

    // Backup & Restore Support
    @Query("SELECT * FROM parties")
    suspend fun getAllPartiesList(): List<Party>

    @Query("SELECT * FROM ledger_entries")
    suspend fun getAllEntriesList(): List<LedgerEntry>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsList(): List<Payment>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT * FROM reminder_lists")
    suspend fun getAllReminderListsList(): List<ReminderList>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<Reminder>

    @Query("SELECT * FROM stocks")
    suspend fun getAllStocksList(): List<Stock>

    @Query("SELECT * FROM stock_entries")
    suspend fun getAllStockEntriesList(): List<StockEntry>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesList(): List<Note>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersList(): List<Folder>

    @Transaction
    suspend fun clearAllData() {
        deleteAllEntries()
        deleteAllPayments()
        deleteAllParties()
        deleteAllExpenses()
        deleteAllReminders()
        deleteAllReminderLists()
        deleteAllStockEntries()
        deleteAllStocks()
        deleteAllNotes()
        deleteAllFolders()
        deleteAllFuelEntries()
        deleteAllMaintenanceEntries()
        deleteAllVehicles()
    }

    @Query("DELETE FROM parties") suspend fun deleteAllParties()
    @Query("DELETE FROM ledger_entries") suspend fun deleteAllEntries()
    @Query("DELETE FROM payments") suspend fun deleteAllPayments()
    @Query("DELETE FROM expenses") suspend fun deleteAllExpenses()
    @Query("DELETE FROM reminder_lists") suspend fun deleteAllReminderLists()
    @Query("DELETE FROM reminders") suspend fun deleteAllReminders()
    @Query("DELETE FROM stocks") suspend fun deleteAllStocks()
    @Query("DELETE FROM stock_entries") suspend fun deleteAllStockEntries()
    @Query("DELETE FROM notes") suspend fun deleteAllNotes()
    @Query("DELETE FROM folders") suspend fun deleteAllFolders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(entry: FuelEntry)

    @Query("SELECT * FROM fuel_entries ORDER BY date DESC")
    fun getAllFuelEntries(): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries")
    suspend fun getAllFuelEntriesList(): List<FuelEntry>

    @Query("SELECT * FROM fuel_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getFuelEntryBySyncId(syncId: String): FuelEntry?

    @Delete
    suspend fun deleteFuelEntry(entry: FuelEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceEntry(entry: MaintenanceEntry)

    @Query("SELECT * FROM maintenance_entries ORDER BY date DESC")
    fun getAllMaintenanceEntries(): Flow<List<MaintenanceEntry>>

    @Query("SELECT * FROM maintenance_entries")
    suspend fun getAllMaintenanceEntriesList(): List<MaintenanceEntry>

    @Query("SELECT * FROM maintenance_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getMaintenanceEntryBySyncId(syncId: String): MaintenanceEntry?

    @Delete
    suspend fun deleteMaintenanceEntry(entry: MaintenanceEntry)

    @Query("DELETE FROM fuel_entries") suspend fun deleteAllFuelEntries()
    @Query("DELETE FROM maintenance_entries") suspend fun deleteAllMaintenanceEntries()
    @Query("DELETE FROM vehicles") suspend fun deleteAllVehicles()

    // Vehicles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Query("SELECT * FROM vehicles ORDER BY lastUpdated DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehiclesList(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?

    @Query("SELECT * FROM vehicles WHERE syncId = :syncId LIMIT 1")
    suspend fun getVehicleBySyncId(syncId: String): Vehicle?

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    // Scoped Queries
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>>

    @Query("SELECT * FROM maintenance_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getMaintenanceEntriesForVehicle(vehicleId: Int): Flow<List<MaintenanceEntry>>
}
