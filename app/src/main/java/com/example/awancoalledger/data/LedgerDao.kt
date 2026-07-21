package com.example.awancoalledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM parties WHERE isDeleted = 0")
    fun getAllParties(): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getPartyById(id: Int): Party?

    @Query("SELECT * FROM parties WHERE syncId = :syncId LIMIT 1")
    suspend fun getPartyBySyncId(syncId: String): Party?

    @Transaction
    @Query("SELECT * FROM parties WHERE id = :partyId AND isDeleted = 0")
    fun getPartyWithDetails(partyId: Int): Flow<PartyWithDetails?>

    @Transaction
    @Query("SELECT * FROM parties WHERE isDeleted = 0")
    fun getAllPartiesWithDetails(): Flow<List<PartyWithDetails>>

    @Upsert
    suspend fun upsertParty(party: Party)

    @Delete
    suspend fun hardDeleteParty(party: Party)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry)

    @Query("SELECT * FROM ledger_entries WHERE partyId = :partyId AND isDeleted = 0 ORDER BY date DESC")
    fun getEntriesForParty(partyId: Int): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getEntryBySyncId(syncId: String): LedgerEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Query("SELECT * FROM payments WHERE partyId = :partyId AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsForParty(partyId: Int): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE syncId = :syncId LIMIT 1")
    suspend fun getPaymentBySyncId(syncId: String): Payment?

    @Query("SELECT * FROM ledger_entries WHERE isDeleted = 0 AND partyId IN (SELECT id FROM parties WHERE isDeleted = 0) ORDER BY date DESC LIMIT 5")
    fun getRecentEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE isDeleted = 0 AND partyId IN (SELECT id FROM parties WHERE isDeleted = 0)")
    fun getAllEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM payments WHERE isDeleted = 0 AND partyId IN (SELECT id FROM parties WHERE isDeleted = 0)")
    fun getAllPayments(): Flow<List<Payment>>

    @Delete
    suspend fun hardDeleteEntry(entry: LedgerEntry)

    @Delete
    suspend fun hardDeletePayment(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE syncId = :syncId LIMIT 1")
    suspend fun getExpenseBySyncId(syncId: String): Expense?

    @Delete
    suspend fun hardDeleteExpense(expense: Expense)

    // Reminder Lists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderList(list: ReminderList): Long

    @Query("SELECT * FROM reminder_lists WHERE isDeleted = 0 ORDER BY `order` ASC")
    fun getAllReminderLists(): Flow<List<ReminderList>>

    @Query("SELECT * FROM reminder_lists WHERE syncId = :syncId LIMIT 1")
    suspend fun getReminderListBySyncId(syncId: String): ReminderList?

    @Delete
    suspend fun hardDeleteReminderList(list: ReminderList)

    // Reminders Enhanced
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE syncId = :syncId LIMIT 1")
    suspend fun getReminderBySyncId(syncId: String): Reminder?

    @Query("SELECT * FROM reminders WHERE listId = :listId AND isDeleted = 0 ORDER BY isCompleted ASC, priority DESC")
    fun getRemindersForList(listId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE parentId = :parentId AND isDeleted = 0")
    fun getSubtasks(parentId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isDeleted = 0")
    fun getCompletedReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0")
    fun getAllReminders(): Flow<List<Reminder>>

    @Delete
    suspend fun hardDeleteReminder(reminder: Reminder)

    // Stocks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: Stock): Long

    @Update
    suspend fun updateStock(stock: Stock)

    @Query("SELECT * FROM stocks WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM stocks WHERE mineName = :mineName LIMIT 1")
    suspend fun getStockByMineName(mineName: String): Stock?

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Int): Stock?

    @Query("SELECT * FROM stocks WHERE syncId = :syncId LIMIT 1")
    suspend fun getStockBySyncId(syncId: String): Stock?

    @Delete
    suspend fun hardDeleteStock(stock: Stock)

    // Stock Entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntry(entry: StockEntry)

    @Query("SELECT * FROM stock_entries WHERE stockId = :stockId AND isDeleted = 0 ORDER BY date DESC")
    fun getEntriesForStock(stockId: Int): Flow<List<StockEntry>>

    @Query("SELECT * FROM stock_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getStockEntryBySyncId(syncId: String): StockEntry?

    @Delete
    suspend fun hardDeleteStockEntry(entry: StockEntry)

    // Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("SELECT * FROM notes WHERE syncId = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): Note?

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, date DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isPinned DESC, date DESC")
    fun getNotesInFolder(folderId: Int): Flow<List<Note>>

    @Delete
    suspend fun hardDeleteNote(note: Note)

    // Folders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE syncId = :syncId LIMIT 1")
    suspend fun getFolderBySyncId(syncId: String): Folder?

    @Delete
    suspend fun hardDeleteFolder(folder: Folder)

    // Backup & Restore Support
    @Query("SELECT * FROM parties WHERE isDeleted = 0")
    suspend fun getAllPartiesList(): List<Party>

    @Query("SELECT * FROM ledger_entries WHERE isDeleted = 0 AND partyId IN (SELECT id FROM parties WHERE isDeleted = 0)")
    suspend fun getAllEntriesList(): List<LedgerEntry>

    @Query("SELECT * FROM payments WHERE isDeleted = 0 AND partyId IN (SELECT id FROM parties WHERE isDeleted = 0)")
    suspend fun getAllPaymentsList(): List<Payment>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT * FROM reminder_lists WHERE isDeleted = 0")
    suspend fun getAllReminderListsList(): List<ReminderList>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0")
    suspend fun getAllRemindersList(): List<Reminder>

    @Query("SELECT * FROM stocks WHERE isDeleted = 0")
    suspend fun getAllStocksList(): List<Stock>

    @Query("SELECT * FROM stock_entries WHERE isDeleted = 0")
    suspend fun getAllStockEntriesList(): List<StockEntry>

    @Query("SELECT * FROM notes WHERE isDeleted = 0")
    suspend fun getAllNotesList(): List<Note>

    @Query("SELECT * FROM folders WHERE isDeleted = 0")
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

    @Query("SELECT * FROM fuel_entries WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllFuelEntries(): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE isDeleted = 0")
    suspend fun getAllFuelEntriesList(): List<FuelEntry>

    @Query("SELECT * FROM fuel_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getFuelEntryBySyncId(syncId: String): FuelEntry?

    @Delete
    suspend fun hardDeleteFuelEntry(entry: FuelEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceEntry(entry: MaintenanceEntry)

    @Query("SELECT * FROM maintenance_entries WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllMaintenanceEntries(): Flow<List<MaintenanceEntry>>

    @Query("SELECT * FROM maintenance_entries WHERE isDeleted = 0")
    suspend fun getAllMaintenanceEntriesList(): List<MaintenanceEntry>

    @Query("SELECT * FROM maintenance_entries WHERE syncId = :syncId LIMIT 1")
    suspend fun getMaintenanceEntryBySyncId(syncId: String): MaintenanceEntry?

    @Delete
    suspend fun hardDeleteMaintenanceEntry(entry: MaintenanceEntry)

    @Query("DELETE FROM fuel_entries") suspend fun deleteAllFuelEntries()
    @Query("DELETE FROM maintenance_entries") suspend fun deleteAllMaintenanceEntries()
    @Query("DELETE FROM vehicles") suspend fun deleteAllVehicles()

    // Vehicles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Query("SELECT * FROM vehicles WHERE isDeleted = 0 ORDER BY lastUpdated DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE isDeleted = 0")
    suspend fun getAllVehiclesList(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?

    @Query("SELECT * FROM vehicles WHERE syncId = :syncId LIMIT 1")
    suspend fun getVehicleBySyncId(syncId: String): Vehicle?

    @Delete
    suspend fun hardDeleteVehicle(vehicle: Vehicle)

    // Scoped Queries
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY date DESC")
    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>>

    @Query("SELECT * FROM maintenance_entries WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY date DESC")
    fun getMaintenanceEntriesForVehicle(vehicleId: Int): Flow<List<MaintenanceEntry>>

    // App Notifications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    fun getUnreadNotificationCount(): Flow<Int>

    @Query("UPDATE app_notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead()

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("DELETE FROM app_notifications WHERE id NOT IN (SELECT id FROM app_notifications ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimNotifications(limit: Int = 100)
}
