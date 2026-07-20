package com.example.awancoalledger.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.awancoalledger.LedgerApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.awancoalledger.data.*
import com.example.awancoalledger.utils.DataExchangeUtils

class SyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val collection = inputData.getString("collection") ?: return Result.failure()
        val syncId = inputData.getString("syncId") ?: return Result.failure()

        val app = applicationContext as? LedgerApplication ?: return Result.failure()
        val dao = app.database.ledgerDao()
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        
        val userId = auth.currentUser?.uid ?: return Result.failure()

        val map = getMapForEntity(dao, collection, syncId, context, app)
        
        if (map == null) {
            // Entity might have been completely hard deleted locally or never existed
            return Result.success()
        }

        return try {
            firestore.collection("users").document(userId)
                .collection(collection).document(syncId)
                .set(map).await()
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync $collection/$syncId", e)
            Result.retry()
        }
    }

    private suspend fun getMapForEntity(dao: LedgerDao, collection: String, syncId: String, context: Context, app: LedgerApplication): Map<String, Any>? {
        return when (collection) {
            "parties" -> {
                val p = dao.getPartyBySyncId(syncId) ?: return null
                mapOf("schemaVersion" to 1, "isDeleted" to p.isDeleted, 
                    "name" to p.name, "phone" to p.phone, "address" to p.address,
                    "type" to p.type.name, "syncId" to p.syncId, "lastUpdated" to p.lastUpdated)
            }
            "ledger_entries" -> {
                val e = dao.getEntryBySyncId(syncId) ?: return null
                val party = dao.getPartyById(e.partyId) ?: return null
                val weight = e.weight ?: 0.0
                val rate = e.rate ?: 0.0
                val fare = e.fare ?: 0.0
                val advPayment = e.advPayment ?: 0.0
                if (isInvalidDouble(weight) || isInvalidDouble(rate) || isInvalidDouble(fare) || isInvalidDouble(advPayment)) return null
                mapOf("schemaVersion" to 1, "isDeleted" to e.isDeleted, 
                    "partySyncId" to party.syncId, "date" to e.date,
                    "truckNumber" to (e.truckNumber ?: ""), "mine" to (e.mine ?: ""),
                    "warehouse" to (e.warehouse ?: ""), "weight" to weight, "rate" to rate,
                    "fare" to fare, "advPayment" to advPayment,
                    "syncId" to e.syncId, "lastUpdated" to e.lastUpdated)
            }
            "payments" -> {
                val p = dao.getPaymentBySyncId(syncId) ?: return null
                val party = dao.getPartyById(p.partyId) ?: return null
                if (isInvalidDouble(p.amount)) return null
                mapOf("schemaVersion" to 1, "isDeleted" to p.isDeleted, 
                    "partySyncId" to party.syncId, "date" to p.date, "amount" to p.amount,
                    "type" to p.type.name, "note" to (p.note ?: ""),
                    "syncId" to p.syncId, "lastUpdated" to p.lastUpdated)
            }
            "expenses" -> {
                val e = dao.getExpenseBySyncId(syncId) ?: return null
                if (isInvalidDouble(e.amount)) return null
                mapOf("schemaVersion" to 1, "isDeleted" to e.isDeleted, 
                    "amount" to e.amount, "category" to e.category.name,
                    "date" to e.date, "note" to (e.note ?: ""),
                    "syncId" to e.syncId, "lastUpdated" to e.lastUpdated)
            }
            "notes" -> {
                val n = dao.getNoteBySyncId(syncId) ?: return null
                val folderSyncId = if (n.folderId != null) dao.getAllFoldersList().find { it.id == n.folderId }?.syncId else null
                mapOf("schemaVersion" to 1, "isDeleted" to n.isDeleted, 
                    "title" to n.title, "content" to n.content, "date" to n.date,
                    "isPinned" to n.isPinned, "folderSyncId" to (folderSyncId ?: ""),
                    "color" to (n.color ?: -1), "textColor" to (n.textColor ?: -1),
                    "isLocked" to n.isLocked, "fontSize" to (n.fontSize ?: -1f),
                    "bgImageId" to (n.bgImageId ?: -1),
                    "syncId" to n.syncId, "lastUpdated" to n.lastUpdated)
            }
            "folders" -> {
                val f = dao.getFolderBySyncId(syncId) ?: return null
                mapOf("schemaVersion" to 1, "isDeleted" to f.isDeleted, 
                    "name" to f.name, "dateCreated" to f.dateCreated,
                    "syncId" to f.syncId, "lastUpdated" to f.lastUpdated)
            }
            "reminder_lists" -> {
                val l = dao.getReminderListBySyncId(syncId) ?: return null
                mapOf("schemaVersion" to 1, "isDeleted" to l.isDeleted, 
                    "name" to l.name, "color" to l.color, "iconName" to l.iconName,
                    "order" to l.order, "isDefault" to l.isDefault,
                    "syncId" to l.syncId, "lastUpdated" to l.lastUpdated)
            }
            "reminders" -> {
                val r = dao.getReminderBySyncId(syncId) ?: return null
                val listSyncId = dao.getAllReminderListsList().find { it.id == r.listId }?.syncId ?: return null
                mapOf("schemaVersion" to 1, "isDeleted" to r.isDeleted, 
                    "title" to r.title, "note" to (r.note ?: ""),
                    "url" to (r.url ?: ""), "listSyncId" to listSyncId,
                    "dueDate" to (r.dueDate ?: 0L), "isCompleted" to r.isCompleted,
                    "isFlagged" to r.isFlagged, "priority" to r.priority.name,
                    "category" to r.category.name,
                    "syncId" to r.syncId, "lastUpdated" to r.lastUpdated)
            }
            "stocks" -> {
                val s = dao.getStockBySyncId(syncId) ?: return null
                if (isInvalidDouble(s.totalWeight) || isInvalidDouble(s.peakWeight)) return null
                mapOf("schemaVersion" to 1, "isDeleted" to s.isDeleted, 
                    "mineName" to s.mineName, "totalWeight" to s.totalWeight,
                    "peakWeight" to s.peakWeight, "lastWarehouse" to (s.lastWarehouse ?: ""),
                    "updatedAt" to s.updatedAt, "syncId" to s.syncId, "lastUpdated" to s.lastUpdated)
            }
            "stock_entries" -> {
                val e = dao.getStockEntryBySyncId(syncId) ?: return null
                if (isInvalidDouble(e.weight)) return null
                val stock = dao.getStockById(e.stockId) ?: return null
                mapOf("schemaVersion" to 1, "isDeleted" to e.isDeleted, 
                    "stockSyncId" to stock.syncId, "weight" to e.weight,
                    "warehouse" to e.warehouse, "date" to e.date,
                    "note" to (e.note ?: ""), "syncId" to e.syncId, "lastUpdated" to e.lastUpdated)
            }
            "fuel_entries" -> {
                val e = dao.getFuelEntryBySyncId(syncId) ?: return null
                if (isInvalidDouble(e.mileage) || isInvalidDouble(e.liters) || isInvalidDouble(e.amount)) return null
                val vehicleSyncId = dao.getVehicleById(e.vehicleId)?.syncId ?: ""
                mapOf("schemaVersion" to 1, "isDeleted" to e.isDeleted, 
                    "vehicleSyncId" to vehicleSyncId,
                    "mileage" to e.mileage, "liters" to e.liters, "amount" to e.amount,
                    "date" to e.date, "syncId" to e.syncId, "lastUpdated" to e.lastUpdated)
            }
            "maintenance_entries" -> {
                val e = dao.getMaintenanceEntryBySyncId(syncId) ?: return null
                if (isInvalidDouble(e.mileage) || isInvalidDouble(e.cost)) return null
                val vehicleSyncId = dao.getVehicleById(e.vehicleId)?.syncId ?: ""
                mapOf("schemaVersion" to 1, "isDeleted" to e.isDeleted, 
                    "vehicleSyncId" to vehicleSyncId,
                    "mileage" to e.mileage, "cost" to e.cost, "description" to e.description,
                    "type" to e.type, "date" to e.date, "syncId" to e.syncId, "lastUpdated" to e.lastUpdated)
            }
            "vehicles" -> {
                val v = dao.getVehicleBySyncId(syncId) ?: return null
                if (isInvalidDouble(v.currentMileage)) return null
                mapOf("schemaVersion" to 1, "isDeleted" to v.isDeleted, 
                    "name" to v.name, "plateNumber" to v.plateNumber,
                    "type" to v.type, "currentMileage" to v.currentMileage,
                    "isPrimary" to v.isPrimary,
                    "syncId" to v.syncId, "lastUpdated" to v.lastUpdated)
            }
            "settings" -> {
                val settingsRepository = app.settingsRepository
                val logoUri = settingsRepository.getCompanyLogoUri()
                val signatureUri = settingsRepository.getSignatureUri()
                
                val data = mutableMapOf<String, Any>(
                    "bizName" to settingsRepository.getBusinessName(),
                    "ownerName" to settingsRepository.getOwnerName(),
                    "bizPhone" to settingsRepository.getBusinessPhone(),
                    "bizAddress" to settingsRepository.getBusinessAddress(),
                    "darkMode" to settingsRepository.isDarkMode(),
                    "appLock" to settingsRepository.isAppLockEnabled(),
                    "biometrics" to settingsRepository.isBiometricsEnabled(),
                    "appPin" to settingsRepository.getAppPin(),
                    "oilChangeInterval" to settingsRepository.getOilChangeInterval(),
                    "lastUpdated" to System.currentTimeMillis()
                )

                if (logoUri == null) {
                    data["logoUri"] = ""
                } else if (logoUri.startsWith("http")) {
                    data["logoUri"] = logoUri
                }

                if (signatureUri == null) {
                    data["signatureUri"] = ""
                } else if (signatureUri.startsWith("http")) {
                    data["signatureUri"] = signatureUri
                }
                
                data["logoBase64"] = DataExchangeUtils.fileToBase64(context, "company_logo.png") ?: ""
                data["signatureBase64"] = DataExchangeUtils.fileToBase64(context, "signature.png") ?: ""
                data
            }
            else -> null
        }
    }

    private fun isInvalidDouble(value: Double?): Boolean {
        return value == null || value.isNaN() || value.isInfinite()
    }
}
