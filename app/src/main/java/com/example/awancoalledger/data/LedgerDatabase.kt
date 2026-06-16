package com.example.awancoalledger.data

import android.content.Context
import androidx.room.*

@Database(entities = [Party::class, LedgerEntry::class, Payment::class, Expense::class, Reminder::class, ReminderList::class, Stock::class, StockEntry::class, Note::class, Folder::class, FuelEntry::class, MaintenanceEntry::class, Vehicle::class], version = 17, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN fontSize REAL")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN bgImageId INTEGER")
            }
        }

        fun getDatabase(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger_database"
                )
                .addMigrations(MIGRATION_15_16, MIGRATION_16_17)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
