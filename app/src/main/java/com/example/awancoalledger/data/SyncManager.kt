package com.example.awancoalledger.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlin.ExperimentalStdlibApi

import android.content.Context
import com.example.awancoalledger.utils.DataExchangeUtils
import androidx.room.withTransaction

@OptIn(ExperimentalStdlibApi::class)
class SyncManager(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val repository: LedgerRepository,
    private val database: LedgerDatabase,
    private val firebaseManager: FirebaseManager,
    private val settingsRepository: SettingsRepository
) {
    private val dao = database.ledgerDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    private fun isInvalidDouble(value: Double?): Boolean {
        return value == null || value.isNaN() || value.isInfinite()
    }

    // ── Real-time Firestore listeners (download direction) ─────────────────────

    fun startSync() {
        stopSync() // Clear existing listeners to prevent duplicates
        val userId = firebaseManager.getUserId() ?: return

        // --- 1. Parties ---
        listenToCollection(userId, "parties") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach
                    
                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getPartyBySyncId(syncId)?.let { dao.hardDeleteParty(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getPartyBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val party = Party(
                            id = local?.id ?: 0,
                            name = data["name"] as? String ?: "",
                            phone = data["phone"] as? String ?: "",
                            address = data["address"] as? String ?: "",
                            type = runCatching { PartyType.valueOf(data["type"] as? String ?: "BUYER") }.getOrDefault(PartyType.BUYER),
                            syncId = syncId,
                            lastUpdated = lastUpdated,
                            isDeleted = data["isDeleted"] as? Boolean ?: false
                        )
                        dao.upsertParty(party)
                    }
                }
        }
            }

        // --- 2. Ledger Entries ---
        listenToCollection(userId, "ledger_entries") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getEntryBySyncId(syncId)?.let { dao.hardDeleteEntry(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getEntryBySyncId(syncId)
                    
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val partySyncId = data["partySyncId"] as? String ?: return@forEach
                        var party = dao.getPartyBySyncId(partySyncId)
                        
                        if (party == null) {
                            try {
                                val partyDoc = firestore.collection("users").document(userId)
                                    .collection("parties").document(partySyncId).get().await()
                                if (partyDoc.exists()) {
                                    val pData = partyDoc.data
                                    if (pData != null) {
                                        val newParty = Party(
                                            name = pData["name"] as? String ?: "",
                                            phone = pData["phone"] as? String ?: "",
                                            address = pData["address"] as? String ?: "",
                                            type = PartyType.valueOf(pData["type"] as? String ?: "Customer"),
                                            syncId = partySyncId,
                                            lastUpdated = pData["lastUpdated"] as? Long ?: 0L
                                        )
                                        dao.upsertParty(newParty)
                                        party = dao.getPartyBySyncId(partySyncId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("SyncManager", "Failed to fetch missing party $partySyncId", e)
                            }
                        }
                        
                        if (party == null) {
                            Log.w("SyncManager", "Skipping entry $syncId: Party $partySyncId not found")
                            return@forEach
                        }

                        val weight = (data["weight"] as? Double) ?: (data["weight"] as? Long)?.toDouble() ?: 0.0
                        val rate = (data["rate"] as? Double) ?: (data["rate"] as? Long)?.toDouble() ?: 0.0
                        val fare = (data["fare"] as? Double) ?: (data["fare"] as? Long)?.toDouble() ?: 0.0
                        val advPayment = (data["advPayment"] as? Double) ?: (data["advPayment"] as? Long)?.toDouble() ?: 0.0
                        
                        if (isInvalidDouble(weight) || isInvalidDouble(rate) || isInvalidDouble(fare) || isInvalidDouble(advPayment)) {
                            Log.w("SyncManager", "Skipping corrupted entry: $syncId")
                            return@forEach
                        }

                        dao.insertEntry(LedgerEntry(
                            id = local?.id ?: 0,
                            partyId = party.id, date = (data["date"] as? Long) ?: 0L,
                            truckNumber = data["truckNumber"] as? String,
                            mine = data["mine"] as? String, warehouse = data["warehouse"] as? String,
                            weight = weight, rate = rate, fare = fare, advPayment = advPayment,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "payments") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getPaymentBySyncId(syncId)?.let { dao.hardDeletePayment(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getPaymentBySyncId(syncId)
                    
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val partySyncId = data["partySyncId"] as? String ?: return@forEach
                        var party = dao.getPartyBySyncId(partySyncId)
                        
                        if (party == null) {
                            try {
                                val partyDoc = firestore.collection("users").document(userId)
                                    .collection("parties").document(partySyncId).get().await()
                                if (partyDoc.exists()) {
                                    val pData = partyDoc.data
                                    if (pData != null) {
                                        val newParty = Party(
                                            name = pData["name"] as? String ?: "",
                                            phone = pData["phone"] as? String ?: "",
                                            address = pData["address"] as? String ?: "",
                                            type = PartyType.valueOf(pData["type"] as? String ?: "Customer"),
                                            syncId = partySyncId,
                                            lastUpdated = pData["lastUpdated"] as? Long ?: 0L
                                        )
                                        dao.upsertParty(newParty)
                                        party = dao.getPartyBySyncId(partySyncId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("SyncManager", "Failed to fetch missing party $partySyncId", e)
                            }
                        }

                        if (party == null) {
                            Log.w("SyncManager", "Skipping payment $syncId: Party $partySyncId not found")
                            return@forEach
                        }

                        val amount = (data["amount"] as? Double) ?: (data["amount"] as? Long)?.toDouble() ?: 0.0
                        if (isInvalidDouble(amount)) { Log.w("SyncManager", "Skipping corrupted payment: $syncId"); return@forEach }
                        
                        dao.insertPayment(Payment(
                            id = local?.id ?: 0,
                            partyId = party.id, date = (data["date"] as? Long) ?: 0L, amount = amount,
                            type = runCatching { PaymentType.valueOf(data["type"] as? String ?: "THEY_PAID") }.getOrDefault(PaymentType.THEY_PAID), note = data["note"] as? String,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "expenses") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getExpenseBySyncId(syncId)?.let { dao.hardDeleteExpense(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getExpenseBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val amount = (data["amount"] as? Double) ?: (data["amount"] as? Long)?.toDouble() ?: 0.0
                        dao.insertExpense(Expense(
                            id = local?.id ?: 0,
                            amount = amount,
                            category = runCatching { ExpenseCategory.valueOf(data["category"] as? String ?: "OTHERS") }.getOrDefault(ExpenseCategory.OTHERS),
                            date = (data["date"] as? Long) ?: 0L, note = data["note"] as? String,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "stocks") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getStockBySyncId(syncId)?.let { dao.hardDeleteStock(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getStockBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val tw = (data["totalWeight"] as? Double) ?: (data["totalWeight"] as? Long)?.toDouble() ?: 0.0
                        val pw = (data["peakWeight"] as? Double) ?: (data["peakWeight"] as? Long)?.toDouble() ?: 0.0
                        if (isInvalidDouble(tw) || isInvalidDouble(pw)) return@forEach
                        dao.insertStock(Stock(
                            id = local?.id ?: 0,
                            mineName = data["mineName"] as? String ?: "", totalWeight = tw, peakWeight = pw,
                            lastWarehouse = data["lastWarehouse"] as? String, updatedAt = (data["updatedAt"] as? Long) ?: 0L,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "stock_entries") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getStockEntryBySyncId(syncId)?.let { dao.hardDeleteStockEntry(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val stockSyncId = data["stockSyncId"] as? String ?: return@forEach
                    var stock = dao.getStockBySyncId(stockSyncId)
                    
                    // Fetch missing parent stock from Firestore if not found locally
                    if (stock == null) {
                        try {
                            val stockDoc = firestore.collection("users").document(userId)
                                .collection("stocks").document(stockSyncId).get().await()
                            if (stockDoc.exists()) {
                                val sData = stockDoc.data
                                if (sData != null) {
                                    val tw = (sData["totalWeight"] as? Double) ?: (sData["totalWeight"] as? Long)?.toDouble() ?: 0.0
                                    val pw = (sData["peakWeight"] as? Double) ?: (sData["peakWeight"] as? Long)?.toDouble() ?: 0.0
                                    if (isInvalidDouble(tw) || isInvalidDouble(pw)) return@forEach
                                    val newStock = Stock(
                                        mineName = sData["mineName"] as? String ?: "",
                                        totalWeight = tw, peakWeight = pw,
                                        lastWarehouse = sData["lastWarehouse"] as? String,
                                        updatedAt = sData["updatedAt"] as? Long ?: System.currentTimeMillis(),
                                        syncId = stockSyncId,
                                        lastUpdated = sData["lastUpdated"] as? Long ?: 0L
                                    )
                                    dao.insertStock(newStock)
                                    stock = dao.getStockBySyncId(stockSyncId)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SyncManager", "Failed to fetch missing stock $stockSyncId", e)
                        }
                    }
                    
                    if (stock == null) {
                        Log.w("SyncManager", "Skipping stock_entry $syncId: Stock $stockSyncId not found")
                        return@forEach
                    }

                    val local = dao.getStockEntryBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val w = (data["weight"] as? Double) ?: (data["weight"] as? Long)?.toDouble() ?: 0.0
                        if (isInvalidDouble(w)) return@forEach
                        dao.insertStockEntry(StockEntry(
                            id = local?.id ?: 0,
                            stockId = stock.id, weight = w, warehouse = data["warehouse"] as? String ?: "",
                            date = (data["date"] as? Long) ?: 0L, note = data["note"] as? String,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "folders") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getFolderBySyncId(syncId)?.let { dao.hardDeleteFolder(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getFolderBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        dao.insertFolder(Folder(
                            id = local?.id ?: 0,
                            name = data["name"] as? String ?: "", dateCreated = (data["dateCreated"] as? Long) ?: 0L,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "notes") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getNoteBySyncId(syncId)?.let { dao.hardDeleteNote(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val folderSyncId = data["folderSyncId"] as? String
                    val folderId = if (folderSyncId != null && folderSyncId.isNotBlank()) dao.getFolderBySyncId(folderSyncId)?.id else null
                    val local = dao.getNoteBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        dao.insertNote(Note(
                            id = local?.id ?: 0,
                            title = data["title"] as? String ?: "", content = data["content"] as? String ?: "",
                            date = (data["date"] as? Long) ?: 0L, isPinned = data["isPinned"] as? Boolean ?: false,
                            folderId = folderId,
                            color = (data["color"] as? Long)?.toInt()?.takeIf { it != -1 },
                            textColor = (data["textColor"] as? Long)?.toInt()?.takeIf { it != -1 },
                            isLocked = data["isLocked"] as? Boolean ?: false,
                            fontSize = (data["fontSize"] as? Double)?.toFloat() ?: (data["fontSize"] as? Long)?.toFloat()?.takeIf { it != -1f },
                            bgImageId = (data["bgImageId"] as? Long)?.toInt()?.takeIf { it != -1 },
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "reminder_lists") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getReminderListBySyncId(syncId)?.let { dao.hardDeleteReminderList(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getReminderListBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        dao.insertReminderList(ReminderList(
                            id = local?.id ?: 0,
                            name = data["name"] as? String ?: "",
                            color = (data["color"] as? Long)?.toInt() ?: 0,
                            iconName = data["iconName"] as? String ?: "",
                            order = (data["order"] as? Long)?.toInt() ?: 0,
                            isDefault = data["isDefault"] as? Boolean ?: false,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "reminders") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getReminderBySyncId(syncId)?.let { dao.hardDeleteReminder(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val listSyncId = data["listSyncId"] as? String ?: return@forEach
                    val list = dao.getReminderListBySyncId(listSyncId) ?: return@forEach
                    val local = dao.getReminderBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        dao.insertReminder(Reminder(
                            id = local?.id ?: 0,
                            title = data["title"] as? String ?: "", note = data["note"] as? String,
                            url = data["url"] as? String, listId = list.id,
                            dueDate = data["dueDate"] as? Long,
                            isCompleted = data["isCompleted"] as? Boolean ?: false,
                            isFlagged = data["isFlagged"] as? Boolean ?: false,
                            priority = ReminderPriority.valueOf(data["priority"] as? String ?: "NONE"),
                            category = ReminderCategory.valueOf(data["category"] as? String ?: "GENERAL"),
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "settings") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) return@forEach
                    val data = change.document.data
                    
                    (data["bizName"] as? String)?.let { if (it != settingsRepository.getBusinessName()) settingsRepository.setBusinessName(it) }
                    (data["ownerName"] as? String)?.let { if (it != settingsRepository.getOwnerName()) settingsRepository.setOwnerName(it) }
                    (data["bizPhone"] as? String)?.let { if (it != settingsRepository.getBusinessPhone()) settingsRepository.setBusinessPhone(it) }
                    (data["bizAddress"] as? String)?.let { if (it != settingsRepository.getBusinessAddress()) settingsRepository.setBusinessAddress(it) }
                    (data["darkMode"] as? Boolean)?.let { if (it != settingsRepository.isDarkMode()) settingsRepository.setDarkMode(it) }
                    (data["appLock"] as? Boolean)?.let { if (it != settingsRepository.isAppLockEnabled()) settingsRepository.setAppLockEnabled(it) }
                    (data["biometrics"] as? Boolean)?.let { if (it != settingsRepository.isBiometricsEnabled()) settingsRepository.setBiometricsEnabled(it) }
                    (data["appPin"] as? String)?.let { if (it != settingsRepository.getAppPin()) settingsRepository.setAppPin(it) }
                    // Rely exclusively on Base64 embedded logo for cross-device sync
                    (data["logoBase64"] as? String)?.let { b64 ->
                        if (b64.isNotBlank()) {
                            val restoredUri = DataExchangeUtils.base64ToFile(context, b64, "company_logo.png")
                            // Append timestamp to force UI refresh (Coil caching issue)
                            restoredUri?.let { settingsRepository.setCompanyLogoUri(it.toString() + "?ts=${System.currentTimeMillis()}") }
                        } else {
                            settingsRepository.setCompanyLogoUri(null)
                        }
                    }

                    // Rely exclusively on Base64 embedded signature
                    (data["signatureBase64"] as? String)?.let { b64 ->
                        if (b64.isNotBlank()) {
                            val restoredUri = DataExchangeUtils.base64ToFile(context, b64, "signature.png")
                            restoredUri?.let { settingsRepository.setSignatureUri(it.toString() + "?ts=${System.currentTimeMillis()}") }
                        } else {
                            settingsRepository.setSignatureUri(null)
                        }
                    }

                    (data["oilChangeInterval"] as? Long)?.let { if (it.toInt() != settingsRepository.getOilChangeInterval()) settingsRepository.setOilChangeInterval(it.toInt()) }
                }
        }
            }

        listenToCollection(userId, "fuel_entries") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getFuelEntryBySyncId(syncId)?.let { dao.hardDeleteFuelEntry(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getFuelEntryBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val vehicleSyncId = data["vehicleSyncId"] as? String ?: ""
                        val vehicleId = if (vehicleSyncId.isNotBlank()) dao.getVehicleBySyncId(vehicleSyncId)?.id ?: 1 else 1
                        
                        val mileage = (data["mileage"] as? Double) ?: (data["mileage"] as? Long)?.toDouble() ?: 0.0
                        val liters = (data["liters"] as? Double) ?: (data["liters"] as? Long)?.toDouble() ?: 0.0
                        val amount = (data["amount"] as? Double) ?: (data["amount"] as? Long)?.toDouble() ?: 0.0
                        if (isInvalidDouble(mileage) || isInvalidDouble(liters) || isInvalidDouble(amount)) return@forEach
                        dao.insertFuelEntry(FuelEntry(
                            id = local?.id ?: 0,
                            vehicleId = vehicleId,
                            mileage = mileage, liters = liters, amount = amount,
                            date = (data["date"] as? Long) ?: 0L, syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "maintenance_entries") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getMaintenanceEntryBySyncId(syncId)?.let { dao.hardDeleteMaintenanceEntry(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getMaintenanceEntryBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val vehicleSyncId = data["vehicleSyncId"] as? String ?: ""
                        val vehicleId = if (vehicleSyncId.isNotBlank()) dao.getVehicleBySyncId(vehicleSyncId)?.id ?: 1 else 1
                        
                        val mileage = (data["mileage"] as? Double) ?: (data["mileage"] as? Long)?.toDouble() ?: 0.0
                        val cost = (data["cost"] as? Double) ?: (data["cost"] as? Long)?.toDouble() ?: 0.0
                        if (isInvalidDouble(mileage) || isInvalidDouble(cost)) return@forEach
                        dao.insertMaintenanceEntry(MaintenanceEntry(
                            id = local?.id ?: 0,
                            vehicleId = vehicleId,
                            mileage = mileage, cost = cost, description = data["description"] as? String ?: "",
                            type = data["type"] as? String ?: "OIL_CHANGE", date = (data["date"] as? Long) ?: 0L,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
            }

        listenToCollection(userId, "vehicles") { changes ->
                syncMutex.withLock {
                    changes.forEach { change ->
                    val data = change.document.data
                    val type = change.type
                    val syncId = data["syncId"] as? String ?: return@forEach

                    if (type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                        dao.getVehicleBySyncId(syncId)?.let { dao.hardDeleteVehicle(it) }
                        return@forEach
                    }

                    val lastUpdated = data["lastUpdated"] as? Long ?: 0L
                    val local = dao.getVehicleBySyncId(syncId)
                    if (local == null || lastUpdated > local.lastUpdated) {
                        val mileage = (data["currentMileage"] as? Double) ?: (data["currentMileage"] as? Long)?.toDouble() ?: 0.0
                        if (isInvalidDouble(mileage)) return@forEach
                        dao.insertVehicle(Vehicle(
                            id = local?.id ?: 0,
                            name = data["name"] as? String ?: "", plateNumber = data["plateNumber"] as? String ?: "",
                            type = data["type"] as? String ?: "TRUCK", currentMileage = mileage,
                            isPrimary = data["isPrimary"] as? Boolean ?: false,
                            syncId = syncId, lastUpdated = lastUpdated, isDeleted = data["isDeleted"] as? Boolean ?: false
                        ))
                    }
                }
        }
    }

    }

    fun stopSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }


    private fun listenToCollection(userId: String, collectionName: String, onUpdate: suspend (List<com.google.firebase.firestore.DocumentChange>) -> Unit) {
        val listener = firestore.collection("users").document(userId).collection(collectionName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("SyncManager", "Listen failed for $collectionName", e)
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.let { changes ->
                    scope.launch {
                        database.withTransaction {
                            onUpdate(changes)
                        }
                    }
                }
            }
        listeners.add(listener)
    }

    // ── Full upload (used by forceSync / pull-to-refresh) ─────────────────────

    suspend fun uploadAll() {
        val userId = firebaseManager.getUserId() ?: return

        dao.getAllPartiesList().forEach { uploadPartyInternal(userId, it) }

        dao.getAllEntriesList().forEach { entry ->
            val party = dao.getPartyById(entry.partyId) ?: return@forEach
            uploadEntryInternal(userId, entry, party)
        }

        dao.getAllPaymentsList().forEach { payment ->
            val party = dao.getPartyById(payment.partyId) ?: return@forEach
            uploadPaymentInternal(userId, payment, party)
        }

        dao.getAllExpensesList().forEach { uploadExpenseInternal(userId, it) }

        dao.getAllStocksList().forEach { stock ->
            uploadEntity(userId, "stocks", stock.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to stock.isDeleted, 
                "mineName" to stock.mineName, "totalWeight" to stock.totalWeight,
                "peakWeight" to stock.peakWeight, "lastWarehouse" to (stock.lastWarehouse ?: ""),
                "updatedAt" to stock.updatedAt, "syncId" to stock.syncId, "lastUpdated" to stock.lastUpdated
            ))
        }

        dao.getAllFoldersList().forEach { uploadFolderInternal(userId, it) }

        val allFolders = dao.getAllFoldersList()
        dao.getAllNotesList().forEach { note ->
            val folderSyncId = if (note.folderId != null) allFolders.find { it.id == note.folderId }?.syncId else null
            uploadNoteInternal(userId, note, folderSyncId)
        }

        val allLists = dao.getAllReminderListsList()
        allLists.forEach { list ->
            uploadEntity(userId, "reminder_lists", list.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to list.isDeleted, 
                "name" to list.name, "color" to list.color, "iconName" to list.iconName,
                "order" to list.order, "isDefault" to list.isDefault,
                "syncId" to list.syncId, "lastUpdated" to list.lastUpdated
            ))
        }

        dao.getAllRemindersList().forEach { reminder ->
            val listSyncId = allLists.find { it.id == reminder.listId }?.syncId ?: return@forEach
            uploadReminderInternal(userId, reminder, listSyncId)
        }

        dao.getAllFuelEntriesList().forEach { uploadFuelEntryInternal(userId, it) }
        dao.getAllMaintenanceEntriesList().forEach { uploadMaintenanceEntryInternal(userId, it) }
        dao.getAllVehiclesList().forEach { uploadVehicleInternal(userId, it) }

        uploadSettingsInternal(userId)
    }

    // ── Single-entity upload helpers (called right after local Room insert) ────

    suspend fun uploadParty(party: Party) {
        val userId = firebaseManager.getUserId() ?: return
        uploadPartyInternal(userId, party)
    }

    suspend fun uploadEntry(entry: LedgerEntry) {
        val userId = firebaseManager.getUserId() ?: return
        val party = dao.getPartyById(entry.partyId) ?: return
        uploadEntryInternal(userId, entry, party)
    }

    suspend fun uploadPayment(payment: Payment) {
        val userId = firebaseManager.getUserId() ?: return
        val party = dao.getPartyById(payment.partyId) ?: return
        uploadPaymentInternal(userId, payment, party)
    }

    suspend fun uploadExpense(expense: Expense) {
        val userId = firebaseManager.getUserId() ?: return
        uploadExpenseInternal(userId, expense)
    }

    suspend fun uploadNote(note: Note) {
        val userId = firebaseManager.getUserId() ?: return
        val folderSyncId = if (note.folderId != null) dao.getAllFoldersList().find { it.id == note.folderId }?.syncId else null
        uploadNoteInternal(userId, note, folderSyncId)
    }

    suspend fun uploadFolder(folder: Folder) {
        val userId = firebaseManager.getUserId() ?: return
        uploadFolderInternal(userId, folder)
    }

    suspend fun uploadReminderList(list: ReminderList) {
        val userId = firebaseManager.getUserId() ?: return
        uploadEntity(userId, "reminder_lists", list.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to list.isDeleted, 
            "name" to list.name, "color" to list.color, "iconName" to list.iconName,
            "order" to list.order, "isDefault" to list.isDefault,
            "syncId" to list.syncId, "lastUpdated" to list.lastUpdated
        ))
    }

    suspend fun uploadStock(stock: Stock) {
        val userId = firebaseManager.getUserId() ?: return
        uploadEntity(userId, "stocks", stock.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to stock.isDeleted, 
            "mineName" to stock.mineName, "totalWeight" to stock.totalWeight,
            "peakWeight" to stock.peakWeight, "lastWarehouse" to (stock.lastWarehouse ?: ""),
            "updatedAt" to stock.updatedAt, "syncId" to stock.syncId, "lastUpdated" to stock.lastUpdated
        ))
    }

    suspend fun uploadStockEntry(entry: StockEntry) {
        val userId = firebaseManager.getUserId() ?: return
        val stock = dao.getStockById(entry.stockId) ?: return
        uploadEntity(userId, "stock_entries", entry.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to entry.isDeleted, 
            "stockSyncId" to stock.syncId, "weight" to entry.weight,
            "warehouse" to entry.warehouse, "date" to entry.date,
            "note" to (entry.note ?: ""), "syncId" to entry.syncId, "lastUpdated" to entry.lastUpdated
        ))
    }

    suspend fun uploadReminder(reminder: Reminder) {
        val userId = firebaseManager.getUserId() ?: return
        val listSyncId = dao.getAllReminderListsList().find { it.id == reminder.listId }?.syncId ?: return
        uploadReminderInternal(userId, reminder, listSyncId)
    }

    suspend fun uploadFuelEntry(entry: FuelEntry) {
        val userId = firebaseManager.getUserId() ?: return
        uploadFuelEntryInternal(userId, entry)
    }

    suspend fun uploadMaintenanceEntry(entry: MaintenanceEntry) {
        val userId = firebaseManager.getUserId() ?: return
        uploadMaintenanceEntryInternal(userId, entry)
    }

    suspend fun uploadVehicle(vehicle: Vehicle) {
        val userId = firebaseManager.getUserId() ?: return
        uploadVehicleInternal(userId, vehicle)
    }

    suspend fun uploadSettings() {
        val userId = firebaseManager.getUserId() ?: return
        uploadSettingsInternal(userId)
    }

    // ── Private upload internals ───────────────────────────────────────────────

    private suspend fun uploadSettingsInternal(userId: String) {
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

        // Only include logoUri if it's a remote URL or explicitly cleared (null)
        // If it's a local file path, we omit it so that Firestore merge() preserves the existing cloud URL
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

        uploadEntity(userId, "settings", "business_profile", data)
    }

    private suspend fun uploadPartyInternal(userId: String, party: Party) {
        uploadEntity(userId, "parties", party.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to party.isDeleted, 
            "name" to party.name, "phone" to party.phone, "address" to party.address,
            "type" to party.type.name, "syncId" to party.syncId, "lastUpdated" to party.lastUpdated
        ))
    }

    private suspend fun uploadEntryInternal(userId: String, entry: LedgerEntry, party: Party) {
        val weight = entry.weight ?: 0.0
        val rate = entry.rate ?: 0.0
        val fare = entry.fare ?: 0.0
        val advPayment = entry.advPayment ?: 0.0
        if (isInvalidDouble(weight) || isInvalidDouble(rate) || isInvalidDouble(fare) || isInvalidDouble(advPayment)) {
            Log.e("SyncManager", "Refusing to upload corrupted entry: ${entry.syncId}")
            return
        }
        uploadEntity(userId, "ledger_entries", entry.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to entry.isDeleted, 
            "partySyncId" to party.syncId, "date" to entry.date,
            "truckNumber" to (entry.truckNumber ?: ""), "mine" to (entry.mine ?: ""),
            "warehouse" to (entry.warehouse ?: ""), "weight" to weight, "rate" to rate,
            "fare" to fare, "advPayment" to advPayment,
            "syncId" to entry.syncId, "lastUpdated" to entry.lastUpdated
        ))
    }

    private suspend fun uploadPaymentInternal(userId: String, payment: Payment, party: Party) {
        if (isInvalidDouble(payment.amount)) {
            Log.e("SyncManager", "Refusing to upload corrupted payment: ${payment.syncId}")
            return
        }
        uploadEntity(userId, "payments", payment.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to payment.isDeleted, 
            "partySyncId" to party.syncId, "date" to payment.date, "amount" to payment.amount,
            "type" to payment.type.name, "note" to (payment.note ?: ""),
            "syncId" to payment.syncId, "lastUpdated" to payment.lastUpdated
        ))
    }

    private suspend fun uploadExpenseInternal(userId: String, expense: Expense) {
        if (isInvalidDouble(expense.amount)) return
        uploadEntity(userId, "expenses", expense.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to expense.isDeleted, 
            "amount" to expense.amount, "category" to expense.category.name,
            "date" to expense.date, "note" to (expense.note ?: ""),
            "syncId" to expense.syncId, "lastUpdated" to expense.lastUpdated
        ))
    }

    private suspend fun uploadNoteInternal(userId: String, note: Note, folderSyncId: String?) {
        uploadEntity(userId, "notes", note.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to note.isDeleted, 
            "title" to note.title, "content" to note.content, "date" to note.date,
            "isPinned" to note.isPinned, "folderSyncId" to (folderSyncId ?: ""),
            "color" to (note.color ?: -1), "textColor" to (note.textColor ?: -1),
            "isLocked" to note.isLocked, "fontSize" to (note.fontSize ?: -1f),
            "bgImageId" to (note.bgImageId ?: -1),
            "syncId" to note.syncId, "lastUpdated" to note.lastUpdated
        ))
    }

    private suspend fun uploadFolderInternal(userId: String, folder: Folder) {
        uploadEntity(userId, "folders", folder.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to folder.isDeleted, 
            "name" to folder.name, "dateCreated" to folder.dateCreated,
            "syncId" to folder.syncId, "lastUpdated" to folder.lastUpdated
        ))
    }

    private suspend fun uploadReminderInternal(userId: String, reminder: Reminder, listSyncId: String) {
        uploadEntity(userId, "reminders", reminder.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to reminder.isDeleted, 
            "title" to reminder.title, "note" to (reminder.note ?: ""),
            "url" to (reminder.url ?: ""), "listSyncId" to listSyncId,
            "dueDate" to (reminder.dueDate ?: 0L), "isCompleted" to reminder.isCompleted,
            "isFlagged" to reminder.isFlagged, "priority" to reminder.priority.name,
            "category" to reminder.category.name,
            "syncId" to reminder.syncId, "lastUpdated" to reminder.lastUpdated
        ))
    }

    private suspend fun uploadFuelEntryInternal(userId: String, entry: FuelEntry) {
        if (isInvalidDouble(entry.mileage) || isInvalidDouble(entry.liters) || isInvalidDouble(entry.amount)) return
        val vehicleSyncId = dao.getVehicleById(entry.vehicleId)?.syncId ?: ""
        uploadEntity(userId, "fuel_entries", entry.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to entry.isDeleted, 
            "vehicleSyncId" to vehicleSyncId,
            "mileage" to entry.mileage, "liters" to entry.liters, "amount" to entry.amount,
            "date" to entry.date, "syncId" to entry.syncId, "lastUpdated" to entry.lastUpdated
        ))
    }

    private suspend fun uploadMaintenanceEntryInternal(userId: String, entry: MaintenanceEntry) {
        if (isInvalidDouble(entry.mileage) || isInvalidDouble(entry.cost)) return
        val vehicleSyncId = dao.getVehicleById(entry.vehicleId)?.syncId ?: ""
        uploadEntity(userId, "maintenance_entries", entry.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to entry.isDeleted, 
            "vehicleSyncId" to vehicleSyncId,
            "mileage" to entry.mileage, "cost" to entry.cost, "description" to entry.description,
            "type" to entry.type, "date" to entry.date, "syncId" to entry.syncId, "lastUpdated" to entry.lastUpdated
        ))
    }

    private suspend fun uploadVehicleInternal(userId: String, vehicle: Vehicle) {
        uploadEntity(userId, "vehicles", vehicle.syncId, mapOf<String, Any>("schemaVersion" to 1, "isDeleted" to vehicle.isDeleted, 
            "name" to vehicle.name, "plateNumber" to vehicle.plateNumber,
            "type" to vehicle.type, "currentMileage" to vehicle.currentMileage,
            "isPrimary" to vehicle.isPrimary,
            "syncId" to vehicle.syncId, "lastUpdated" to vehicle.lastUpdated
        ))
    }

    private suspend fun uploadEntity(userId: String, collection: String, syncId: String, data: Map<String, Any>) {
        enqueueSyncWorker(collection, syncId)
    }

    // ── Deletion Helpers ──────────────────────────────────────────────────────

    suspend fun deleteParty(party: Party) = deleteEntity("parties", party.syncId)
    suspend fun deleteEntry(entry: LedgerEntry) = deleteEntity("ledger_entries", entry.syncId)
    suspend fun deletePayment(payment: Payment) = deleteEntity("payments", payment.syncId)
    suspend fun deleteExpense(expense: Expense) = deleteEntity("expenses", expense.syncId)
    suspend fun deleteNote(note: Note) = deleteEntity("notes", note.syncId)
    suspend fun deleteFolder(folder: Folder) = deleteEntity("folders", folder.syncId)
    suspend fun deleteReminderList(list: ReminderList) = deleteEntity("reminder_lists", list.syncId)
    suspend fun deleteReminder(reminder: Reminder) = deleteEntity("reminders", reminder.syncId)
    suspend fun deleteFuelEntry(entry: FuelEntry) = deleteEntity("fuel_entries", entry.syncId)
    suspend fun deleteMaintenanceEntry(entry: MaintenanceEntry) = deleteEntity("maintenance_entries", entry.syncId)
    suspend fun deleteVehicle(vehicle: Vehicle) = deleteEntity("vehicles", vehicle.syncId)
    suspend fun deleteStock(stock: Stock) = deleteEntity("stocks", stock.syncId)
    suspend fun deleteStockEntry(entry: StockEntry) = deleteEntity("stock_entries", entry.syncId)

    private suspend fun deleteEntity(collection: String, syncId: String) {
        enqueueSyncWorker(collection, syncId)
    }

    private fun enqueueSyncWorker(collection: String, syncId: String) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        val workData = androidx.work.workDataOf("collection" to collection, "syncId" to syncId)
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val request = androidx.work.OneTimeWorkRequestBuilder<com.example.awancoalledger.workers.SyncWorker>()
            .setConstraints(constraints)
            .setInputData(workData)
            .build()
        workManager.enqueueUniqueWork("sync_${collection}_${syncId}", androidx.work.ExistingWorkPolicy.REPLACE, request)
    }
}
