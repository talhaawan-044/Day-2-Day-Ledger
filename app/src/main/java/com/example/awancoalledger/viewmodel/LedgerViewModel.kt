package com.example.awancoalledger.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import com.example.awancoalledger.utils.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlin.math.absoluteValue

enum class SyncStatus {
    Synced, Syncing, LocalOnly, Error
}

data class RecentActivity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val partyName: String,
    val partyType: PartyType,
    val amount: Double,
    val date: Long,
    val isPayment: Boolean,
    val entry: LedgerEntry? = null,
    val payment: Payment? = null
)

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val firebaseManager: FirebaseManager,
    private val syncManager: SyncManager
) : ViewModel() {

    // --- Sync & Meta ---
    private val _syncStatus = MutableStateFlow(
        if (firebaseManager.getUserId() != null) SyncStatus.Synced else SyncStatus.LocalOnly
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private var lastManualSnapshotTime = 0L

    private var isPendingLoginConfirmation = false

    init {
        // Start real-time Firestore listeners immediately if signed in and not pending confirmation
        if (firebaseManager.getUserId() != null && !isPendingLoginConfirmation) {
            syncManager.startSync()
        } else if (firebaseManager.getUserId() == null) {
            // Phase 1: Invisible Guest Mode (Anonymous Auth)
            viewModelScope.launch {
                firebaseManager.signInAnonymously()
            }
        }

        viewModelScope.launch {
            firebaseManager.currentUser.collect { user ->
                if (user != null) {
                    if (!isPendingLoginConfirmation) {
                        syncManager.startSync()
                        _syncStatus.value = SyncStatus.Synced
                    }
                } else {
                    syncManager.stopSync()
                    _syncStatus.value = SyncStatus.LocalOnly
                }
            }
        }
    }

    fun forceSync() {
        if (firebaseManager.getUserId() == null) {
            _syncStatus.value = SyncStatus.LocalOnly
            return
        }
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                syncManager.uploadAll()
                _syncStatus.value = SyncStatus.Synced
                _lastSyncTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("LedgerViewModel", "Sync failed", e)
                _syncStatus.value = SyncStatus.Error
            }
        }
    }

    private fun syncAfterWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    // --- Auth Placeholder ---
    // --- Auth ---
    val currentUser = firebaseManager.currentUser
    val isAnonymous = currentUser.map { it?.isAnonymous == true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseManager.isAnonymous())

    private val _showConflictDialog = MutableStateFlow(false)
    val showConflictDialog: StateFlow<Boolean> = _showConflictDialog.asStateFlow()

    private val _needsRestart = MutableStateFlow(false)
    val needsRestart: StateFlow<Boolean> = _needsRestart.asStateFlow()
    
    private var pendingCredential: com.google.firebase.auth.AuthCredential? = null

    fun linkAccount(credential: com.google.firebase.auth.AuthCredential, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseManager.linkWithCredential(credential)
            if (result.isSuccess) {
                // Phase 2: Cloud Registration (Account Linking) Successful
                syncManager.startSync()
                _syncStatus.value = SyncStatus.Synced
                syncManager.uploadAll() // Background sync
                onResult(true, null)
            } else {
                val exception = result.exceptionOrNull()
                if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    // Phase 3: Conflict Resolution
                    pendingCredential = credential
                    _showConflictDialog.value = true
                    onResult(false, "COLLISION")
                } else {
                    onResult(false, exception?.localizedMessage ?: "Linking Failed")
                }
            }
        }
    }

    fun handleCollisionRestore(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val credential = pendingCredential ?: return@launch
            _showConflictDialog.value = false
            
            // 1. Sign out of Anonymous account
            // 2. Sign into existing account
            // 3. Wipe local DB + stale image paths
            // 4. Start sync (will download cloud data)
            
            syncManager.stopSync()
            repository.clearAllData()
            settingsRepository.clearImageUris()
            
            val result = firebaseManager.signInWithCredential(credential)
            if (result.isSuccess) {
                syncManager.startSync()
                _syncStatus.value = SyncStatus.Synced
                _needsRestart.value = true
                onSuccess()
            }
            pendingCredential = null
        }
    }

    fun handleCollisionCancel() {
        _showConflictDialog.value = false
        pendingCredential = null
    }

    fun signInWithFirebase(credential: com.google.firebase.auth.AuthCredential, onResult: (Boolean, Boolean) -> Unit) {
        viewModelScope.launch {
            isPendingLoginConfirmation = true
            val result = firebaseManager.signInWithCredential(credential)
            if (result.isSuccess) {
                val (_, isNewUser) = result.getOrThrow()
                onResult(true, isNewUser)
            } else {
                isPendingLoginConfirmation = false
                onResult(false, false)
            }
        }
    }

    fun signInWithEmail(email: String, psw: String, onResult: (Boolean, Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isPendingLoginConfirmation = true
            val result = firebaseManager.signInWithEmail(email, psw)
            if (result.isSuccess) {
                // Email login is inherently an existing user
                onResult(true, false, null)
            } else {
                isPendingLoginConfirmation = false
                onResult(false, false, result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun signUpWithEmail(email: String, psw: String, name: String, onResult: (Boolean, Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isPendingLoginConfirmation = true
            val result = firebaseManager.signUpWithEmail(email, psw, name)
            if (result.isSuccess) {
                // Email sign up is inherently a new user
                onResult(true, true, null)
            } else {
                isPendingLoginConfirmation = false
                onResult(false, false, result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun confirmLogin(isNewUser: Boolean) {
        viewModelScope.launch {
            if (isNewUser) {
                syncManager.startSync()
                _syncStatus.value = SyncStatus.Synced
                syncManager.uploadAll()
            } else {
                syncManager.stopSync()
                repository.clearAllData()
                // Clear stale local file paths so cloud images can be re-downloaded
                settingsRepository.clearImageUris()
                syncManager.startSync()
                _syncStatus.value = SyncStatus.Synced
                _needsRestart.value = true
            }
            isPendingLoginConfirmation = false
        }
    }

    fun cancelLogin(context: Context) {
        // Sign out but do NOT clear local data, as they are aborting the linking process
        firebaseManager.signOut(context) {
            isPendingLoginConfirmation = false
            syncManager.stopSync()
            _syncStatus.value = SyncStatus.LocalOnly
        }
    }

    fun signOut(context: Context, clearData: Boolean = true) {
        firebaseManager.signOut(context) {
            if (clearData) {
                viewModelScope.launch {
                    syncManager.stopSync()
                    repository.clearAllData()
                }
            } else {
                syncManager.stopSync()
            }
        }
    }

    fun getGoogleSignInClient(context: Context): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        return firebaseManager.getGoogleSignInClient(context)
    }

    // --- Settings & App State ---
    val isDarkMode = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isDarkMode() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isDarkMode())

    val accentColorHex = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getAccentColorHex() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getAccentColorHex())

    val isFrostedGlassEnabled = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isFrostedGlassEnabled() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isFrostedGlassEnabled())

    private val _isLocked = MutableStateFlow(settingsRepository.isAppLockEnabled())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    
    val isAppLockEnabled = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isAppLockEnabled() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isAppLockEnabled())

    val ownerName = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getOwnerName() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getOwnerName())

    val businessName = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getBusinessName() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getBusinessName())

    val businessPhone = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getBusinessPhone() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getBusinessPhone())

    val businessAddress = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getBusinessAddress() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getBusinessAddress())

    val countryConfig = settingsRepository.getSettingsFlow()
        .map { 
            val code = settingsRepository.getDefaultCountryCode()
            SUPPORTED_COUNTRIES.find { it.code == code } ?: SUPPORTED_COUNTRIES.first()
        }
        .stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            SUPPORTED_COUNTRIES.find { it.code == settingsRepository.getDefaultCountryCode() } ?: SUPPORTED_COUNTRIES.first()
        )

    val isBiometricsEnabled = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isBiometricsEnabled() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isBiometricsEnabled())

    val companyLogoUri = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getCompanyLogoUri()?.let { Uri.parse(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getCompanyLogoUri()?.let { Uri.parse(it) })

    val signatureUri = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getSignatureUri()?.let { Uri.parse(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getSignatureUri()?.let { Uri.parse(it) })

    private val _isLogoUploading = MutableStateFlow(false)
    val isLogoUploading = _isLogoUploading.asStateFlow()

    private val _isSignatureUploading = MutableStateFlow(false)
    val isSignatureUploading = _isSignatureUploading.asStateFlow()

    val isGridView = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isGridView() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isGridView())

    val dockItems = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getDockItems() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getDockItems())

    fun toggleDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun toggleFrostedGlass(enabled: Boolean) {
        settingsRepository.setFrostedGlassEnabled(enabled)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun toggleAppLock(enabled: Boolean) {
        settingsRepository.setAppLockEnabled(enabled)
        if (!enabled) {
            _isLocked.value = false
        }
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun toggleBiometrics(enabled: Boolean) {
        settingsRepository.setBiometricsEnabled(enabled)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun toggleGridView(enabled: Boolean) {
        settingsRepository.setGridView(enabled)
    }

    fun setPinAndEnable(pin: String) {
        settingsRepository.setAppPin(pin)
        settingsRepository.setAppLockEnabled(true)
        _isLocked.value = true
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun unlock(pin: String): Boolean {
        if (pin == settingsRepository.getAppPin()) {
            _isLocked.value = false
            return true
        }
        return false
    }

    fun unlockWithBiometrics() {
        _isLocked.value = false
    }

    fun lock() {
        if (settingsRepository.isAppLockEnabled()) {
            _isLocked.value = true
        }
    }

    fun updateAccentColorHex(hex: String) {
        settingsRepository.setAccentColorHex(hex)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateOwnerName(name: String) {
        settingsRepository.setOwnerName(name)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateBusinessName(name: String) {
        settingsRepository.setBusinessName(name)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateBusinessPhone(phone: String) {
        settingsRepository.setBusinessPhone(phone)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateBusinessAddress(address: String) {
        settingsRepository.setBusinessAddress(address)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateCountryCode(code: String) {
        settingsRepository.setDefaultCountryCode(code)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateDockItems(items: List<String>) {
        settingsRepository.setDockItems(items)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    fun updateCompanyLogoUri(context: Context, uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                settingsRepository.setCompanyLogoUri(null)
                firebaseManager.deleteFile("company_logo.png")
                syncManager.uploadSettings()
            } else {
                _isLogoUploading.value = true
                val internalUri = DataExchangeUtils.copyImageToInternal(context, uri, "company_logo.png")
                // Set local path for immediate UI update
                settingsRepository.setCompanyLogoUri(internalUri?.toString())
                
                // Upload to Firebase Storage for sync using the internal persistent file
                internalUri?.let { fileUri ->
                    val downloadUrl = firebaseManager.uploadFile(fileUri, "company_logo.png")
                    if (downloadUrl != null) {
                        settingsRepository.setCompanyLogoUri(downloadUrl)
                        syncManager.uploadSettings()
                    }
                }
                _isLogoUploading.value = false
            }
        }
    }

    fun updateSignatureUri(context: Context, uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                settingsRepository.setSignatureUri(null)
                firebaseManager.deleteFile("signature.png")
                syncManager.uploadSettings()
            } else {
                _isSignatureUploading.value = true
                val internalUri = DataExchangeUtils.copyImageToInternal(context, uri, "signature.png")
                settingsRepository.setSignatureUri(internalUri?.toString())
                
                // Upload to Firebase Storage for sync using the internal persistent file
                internalUri?.let { fileUri ->
                    val downloadUrl = firebaseManager.uploadFile(fileUri, "signature.png")
                    if (downloadUrl != null) {
                        settingsRepository.setSignatureUri(downloadUrl)
                        syncManager.uploadSettings()
                    }
                }
                _isSignatureUploading.value = false
            }
        }
    }

    // --- Backup & Restore ---
    private val _autoSnapshots = MutableStateFlow<List<java.io.File>>(emptyList())
    val autoSnapshots: StateFlow<List<java.io.File>> = _autoSnapshots.asStateFlow()

    fun refreshSnapshots(context: Context) {
        val dir = java.io.File(context.filesDir, "backups")
        if (dir.exists()) {
            _autoSnapshots.value = dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }

    fun createManualSnapshot(context: Context, onResult: (Boolean) -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastManualSnapshotTime < 5000) {
            // Prevent spamming (5 second cooldown)
            onResult(false)
            return
        }
        
        viewModelScope.launch {
            try {
                val data = repository.getBackupData()
                val json = DataExchangeUtils.serializeBackup(data)
                val dir = java.io.File(context.filesDir, "backups")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, "manual_${System.currentTimeMillis()}.json")
                file.writeText(json)
                
                DataExchangeUtils.cleanupBackups(context)
                lastManualSnapshotTime = System.currentTimeMillis()
                
                refreshSnapshots(context)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun restoreSnapshot(context: Context, file: java.io.File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = file.readText()
                val backupData = DataExchangeUtils.deserializeBackup(json)
                if (backupData != null) {
                    repository.restoreData(backupData)
                    onSuccess()
                } else {
                    onError("Invalid Backup File")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun shareBackup(context: Context, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = repository.getBackupData()
                
                // Include settings in backup
                val settingsMap = mapOf(
                    "bizName" to settingsRepository.getBusinessName(),
                    "ownerName" to settingsRepository.getOwnerName(),
                    "bizPhone" to settingsRepository.getBusinessPhone(),
                    "bizAddress" to settingsRepository.getBusinessAddress(),
                    "darkMode" to settingsRepository.isDarkMode().toString(),
                    "oilChangeInterval" to settingsRepository.getOilChangeInterval().toString()
                )
                
                // Encode logo and signature as base64
                val logoBase64 = DataExchangeUtils.fileToBase64(context, "company_logo.png")
                val sigBase64 = DataExchangeUtils.fileToBase64(context, "signature.png")
                
                val fullData = data.copy(
                    settings = settingsMap,
                    logoBase64 = logoBase64,
                    signatureBase64 = sigBase64
                )
                
                val json = DataExchangeUtils.serializeBackup(fullData)
                
                // Write to file and share via FileProvider (no size limit)
                val fileName = "AwanCoalLedger_Backup_${System.currentTimeMillis()}.json"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(json)
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_SUBJECT, "Awan Coal Ledger Backup")
                    putExtra(Intent.EXTRA_TEXT, "Here is your complete Ledger Data Backup file.")
                }
                context.startActivity(Intent.createChooser(intent, "Export Ledger Data"))
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Export Failed")
            }
        }
    }

    fun restoreDatabase(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val backupData = DataExchangeUtils.deserializeBackup(json)
                if (backupData != null) {
                    repository.restoreData(backupData)
                    
                    // Restore settings if present
                    backupData.settings.let { settings ->
                        settings["bizName"]?.let { settingsRepository.setBusinessName(it) }
                        settings["ownerName"]?.let { settingsRepository.setOwnerName(it) }
                        settings["bizPhone"]?.let { settingsRepository.setBusinessPhone(it) }
                        settings["bizAddress"]?.let { settingsRepository.setBusinessAddress(it) }
                        settings["darkMode"]?.toBooleanStrictOrNull()?.let { settingsRepository.setDarkMode(it) }
                        settings["oilChangeInterval"]?.toIntOrNull()?.let { settingsRepository.setOilChangeInterval(it) }
                    }
                    
                    // Restore logo and signature images from base64
                    backupData.logoBase64?.let { b64 ->
                        val logoUri = DataExchangeUtils.base64ToFile(context, b64, "company_logo.png")
                        logoUri?.let { settingsRepository.setCompanyLogoUri(it.toString()) }
                    }
                    backupData.signatureBase64?.let { b64 ->
                        val sigUri = DataExchangeUtils.base64ToFile(context, b64, "signature.png")
                        sigUri?.let { settingsRepository.setSignatureUri(it.toString()) }
                    }
                    
                    onSuccess()
                } else {
                    onError("Invalid Backup File")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Restore Failed")
            }
        }
    }

    // --- Parties & Ledger Flows ---
    val allPartiesWithDetails = repository.getAllPartiesWithDetails()
        .map { list -> 
            list.map { party -> 
                party.copy(
                    entries = party.entries.filter { !it.isDeleted },
                    payments = party.payments.filter { !it.isDeleted }
                ) 
            } 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partiesCount = allPartiesWithDetails.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeRemindersCount = repository.getActiveReminders()
        .map { it.count { r -> !r.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notesCount = repository.getAllNotes()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalReceivable = allPartiesWithDetails.map { detailsList ->
        detailsList.sumOf { details ->
            val balance = getBalance(details)
            if (details.party.type == PartyType.BUYER) {
                if (balance > 0) balance else 0.0 // Buyer owes us
            } else {
                if (balance < 0) balance.absoluteValue else 0.0 // Supplier: we paid advance (they owe us)
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayable = allPartiesWithDetails.map { detailsList ->
        detailsList.sumOf { details ->
            val balance = getBalance(details)
            if (details.party.type == PartyType.BUYER) {
                if (balance < 0) balance.absoluteValue else 0.0 // Buyer: they paid advance (we owe them)
            } else {
                if (balance > 0) balance else 0.0 // Supplier: we owe them
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netMarketCredit = combine(totalReceivable, totalPayable) { rec, pay -> rec - pay }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentActivity = allPartiesWithDetails.map { detailsList ->
        val activities = mutableListOf<RecentActivity>()
        detailsList.forEach { details ->
            details.entries.forEach { entries ->
                activities.add(RecentActivity(
                    partyName = details.party.name,
                    partyType = details.party.type,
                    amount = ((entries.weight ?: 0.0) * (entries.rate ?: 0.0)) + (entries.fare ?: 0.0),
                    date = entries.date,
                    isPayment = false,
                    entry = entries
                ))
            }
            details.payments.forEach { payment ->
                activities.add(RecentActivity(
                    partyName = details.party.name,
                    partyType = details.party.type,
                    amount = payment.amount,
                    date = payment.date,
                    isPayment = true,
                    payment = payment
                ))
            }
        }
        activities.sortedByDescending { it.date }.take(10)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _partyDetails = MutableStateFlow<PartyWithDetails?>(null)
    val partyDetails: StateFlow<PartyWithDetails?> = _partyDetails.asStateFlow()

    private var selectPartyJob: kotlinx.coroutines.Job? = null

    fun selectParty(partyId: Int) {
        selectPartyJob?.cancel()
        selectPartyJob = viewModelScope.launch {
            repository.getPartyWithDetails(partyId).collect { details ->
                _partyDetails.value = details?.copy(
                    entries = details.entries.filter { !it.isDeleted },
                    payments = details.payments.filter { !it.isDeleted }
                )
            }
        }
    }

    fun getBalance(details: PartyWithDetails): Double {
        val totalTruckValue = details.entries.sumOf { 
            ((it.weight ?: 0.0) * (it.rate ?: 0.0)) + (it.fare ?: 0.0)
        }
        val totalTheyPaid = details.payments.filter { it.type == PaymentType.THEY_PAID }.sumOf { it.amount }
        val totalIPaid = details.payments.filter { it.type == PaymentType.I_PAID }.sumOf { it.amount }
        
        return if (details.party.type == PartyType.BUYER) {
            // Buyer: He owes for coal (+), he pays me (-), I give him money (+)
            totalTruckValue - totalTheyPaid + totalIPaid
        } else {
            // Supplier: I owe him for coal (+), I pay him (-), he gives me money (+)
            totalTruckValue - totalIPaid + totalTheyPaid
        }
    }

    fun addParty(name: String, phone: String, address: String, type: PartyType) {
        viewModelScope.launch {
            val party = Party(name = name, phone = phone, address = address, type = type)
            repository.upsertParty(party)
            syncAfterWrite { syncManager.uploadParty(party) }
        }
    }

    fun updateParty(party: Party) {
        viewModelScope.launch {
            repository.upsertParty(party)
            syncAfterWrite { syncManager.uploadParty(party) }
        }
    }

    fun deleteParty(party: Party) {
        viewModelScope.launch {
            repository.deleteParty(party)
            syncManager.deleteParty(party)
        }
    }

    // --- Ledger Entries ---
    fun addEntry(partyId: Int, truck: String?, mine: String?, warehouse: String?, weight: Double?, rate: Double?, fare: Double?, adv: Double?, date: Long) {
        viewModelScope.launch {
            val entry = LedgerEntry(
                partyId = partyId, truckNumber = truck, mine = mine, warehouse = warehouse,
                weight = weight, rate = rate, fare = fare, advPayment = adv, date = date
            )
            repository.insertEntry(entry)
            syncAfterWrite { syncManager.uploadEntry(entry) }
        }
    }

    fun updateEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
            syncAfterWrite { syncManager.uploadEntry(entry) }
        }
    }

    fun deleteEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            syncManager.deleteEntry(entry)
        }
    }

    // --- Payments ---
    fun addPayment(partyId: Int, amount: Double, type: PaymentType, note: String?, date: Long) {
        viewModelScope.launch {
            val payment = Payment(partyId = partyId, amount = amount, type = type, note = note, date = date)
            repository.insertPayment(payment)
            syncAfterWrite { syncManager.uploadPayment(payment) }
        }
    }

    fun updatePayment(payment: Payment) {
        viewModelScope.launch {
            repository.insertPayment(payment)
            syncAfterWrite { syncManager.uploadPayment(payment) }
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            syncManager.deletePayment(payment)
        }
    }

    // --- Stocks & Inventory ---
    val allStocks = repository.getAllStocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedStockEntries = MutableStateFlow<List<StockEntry>>(emptyList())
    val selectedStockEntries: StateFlow<List<StockEntry>> = _selectedStockEntries.asStateFlow()

    fun selectStock(stockId: Int) {
        viewModelScope.launch {
            repository.getEntriesForStock(stockId).collect {
                _selectedStockEntries.value = it
            }
        }
    }

    val totalInventoryWeight = allStocks.map { stocks ->
        stocks.sumOf { it.totalWeight }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addStockEntry(mine: String, weight: Double, warehouse: String) {
        viewModelScope.launch {
            val existingStock = repository.getStockByMineName(mine)
            val stockId: Int
            if (existingStock == null) {
                val newStock = Stock(mineName = mine, totalWeight = weight, peakWeight = weight, lastWarehouse = warehouse)
                stockId = repository.insertStock(newStock).toInt()
                syncAfterWrite { syncManager.uploadStock(newStock.copy(id = stockId)) }
            } else {
                val newWeight = existingStock.totalWeight + weight
                val newPeak = maxOf(existingStock.peakWeight, newWeight)
                val updatedStock = existingStock.copy(totalWeight = newWeight, peakWeight = newPeak, lastWarehouse = warehouse, updatedAt = System.currentTimeMillis())
                repository.updateStock(updatedStock)
                stockId = existingStock.id
                syncAfterWrite { syncManager.uploadStock(updatedStock) }
            }
            
            val entry = StockEntry(stockId = stockId, weight = weight, warehouse = warehouse)
            repository.insertStockEntry(entry)
            syncAfterWrite { syncManager.uploadStockEntry(entry) }
        }
    }

    fun updateStockEntry(entry: StockEntry) {
        viewModelScope.launch {
            repository.insertStockEntry(entry)
        }
    }

    fun deleteStockEntry(entry: StockEntry) {
        viewModelScope.launch {
            repository.deleteStockEntry(entry)
            syncManager.deleteStockEntry(entry)
        }
    }

    // --- Expenses ---
    val allExpenses = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Vehicle Tracker ---
    val allVehicles = repository.getAllVehicles()
        .map { list -> 
            list.sortedWith(compareByDescending<Vehicle> { it.isPrimary }.thenByDescending { it.id })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val primaryVehicle = allVehicles.map { vehicles ->
        vehicles.find { it.isPrimary } ?: vehicles.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId: StateFlow<Int?> = _selectedVehicleId.asStateFlow()

    val selectedVehicle = allVehicles.combine(_selectedVehicleId) { vehicles, id ->
        vehicles.find { it.id == id } ?: vehicles.find { it.isPrimary } ?: vehicles.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectVehicle(id: Int) { _selectedVehicleId.value = id }

    init {
        viewModelScope.launch {
            allVehicles.collect { vehicles ->
                if (_selectedVehicleId.value == null && vehicles.isNotEmpty()) {
                    _selectedVehicleId.value = vehicles.first().id
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allFuelEntries = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) repository.getAllFuelEntries()
        else repository.getFuelEntriesForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    @OptIn(ExperimentalCoroutinesApi::class)
    val allMaintenanceEntries = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) repository.getAllMaintenanceEntries()
        else repository.getMaintenanceEntriesForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val oilChangeInterval = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getOilChangeInterval() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getOilChangeInterval())
    
    fun updateOilChangeInterval(interval: Int) {
        settingsRepository.setOilChangeInterval(interval)
        syncAfterWrite { syncManager.uploadSettings() }
    }

    val latestFuelEntry = allFuelEntries.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val avgKmPerLiter = allFuelEntries.map { entries ->
        if (entries.size < 2) 0.0
        else {
            var totalKm = 0.0
            var totalLiters = 0.0
            for (i in 0 until entries.size - 1) {
                val current = entries[i]
                val previous = entries[i+1]
                totalKm += (current.mileage - previous.mileage).absoluteValue
                totalLiters += current.liters
            }
            if (totalLiters > 0) totalKm / totalLiters else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lastOilChangeMileage = allMaintenanceEntries.map { entries ->
        entries.filter { it.type == "OIL_CHANGE" }.firstOrNull()?.mileage ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val nextOilChangeMileage = lastOilChangeMileage.combine(oilChangeInterval) { mileage, interval ->
        if (mileage > 0) mileage + interval else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentVehicleMileage = combine(latestFuelEntry, allMaintenanceEntries, selectedVehicle) { fuel, maint, vehicle ->
        val maxFuel = fuel?.mileage ?: 0.0
        val maxMaint = maint.maxOfOrNull { it.mileage } ?: 0.0
        val maxVehicle = vehicle?.currentMileage ?: 0.0
        maxOf(maxFuel, maxMaint, maxVehicle)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val kmsRemainingForOilChange = nextOilChangeMileage.combine(currentVehicleMileage) { next, current ->
        if (next > 0) next - current else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Primary Vehicle Metrics (For Summary Screen) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val kmsRemainingPrimary = primaryVehicle.flatMapLatest { vehicle ->
        if (vehicle == null) flowOf(0.0)
        else {
            val fuelFlow = repository.getFuelEntriesForVehicle(vehicle.id)
            val maintFlow = repository.getMaintenanceEntriesForVehicle(vehicle.id)
            combine(fuelFlow, maintFlow, oilChangeInterval) { fuel, maint, interval ->
                val lastMaint = maint.filter { it.type == "OIL_CHANGE" }.maxByOrNull { it.date }?.mileage ?: 0.0
                val lastFuel = fuel.maxByOrNull { it.date }?.mileage ?: vehicle.currentMileage
                val current = maxOf(lastFuel, vehicle.currentMileage)
                val next = if (lastMaint > 0) lastMaint + interval else 0.0
                if (next > 0) next - current else 0.0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val nextOilChangePrimary = primaryVehicle.flatMapLatest { vehicle ->
        if (vehicle == null) flowOf(0.0)
        else {
            repository.getMaintenanceEntriesForVehicle(vehicle.id).combine(oilChangeInterval) { maint, interval ->
                val lastMaint = maint.filter { it.type == "OIL_CHANGE" }.maxByOrNull { it.date }?.mileage ?: 0.0
                val lastMaintMileage = lastMaint.toDouble()
                if (lastMaintMileage > 0) lastMaintMileage + interval else 0.0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _vehicleAlert = MutableStateFlow<String?>(null)
    val vehicleAlert: StateFlow<String?> = _vehicleAlert.asStateFlow()

    fun dismissVehicleAlert() { _vehicleAlert.value = null }

    val monthlyFuelCost = allFuelEntries.map { entries ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        entries.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyMaintenanceCost = allMaintenanceEntries.map { entries ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        entries.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.cost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val fuelEfficiencyTrend = allFuelEntries.map { entries ->
        if (entries.size < 2) emptyList<Float>()
        else {
            val trends = mutableListOf<Float>()
            for (i in 0 until entries.size - 1) {
                val current = entries[i]
                val previous = entries[i+1]
                val diffKm = (current.mileage - previous.mileage).absoluteValue
                if (current.liters > 0) {
                    trends.add((diffKm / current.liters).toFloat())
                }
            }
            trends.reversed()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Float>())

    fun addVehicle(name: String, plateNumber: String, type: String, mileage: Double, isPrimary: Boolean = false) {
        viewModelScope.launch {
            val vehicle = Vehicle(name = name, plateNumber = plateNumber, type = type, currentMileage = mileage, isPrimary = isPrimary)
            val id = repository.insertVehicle(vehicle)
            val saved = vehicle.copy(id = id.toInt())
            if (isPrimary) {
                setPrimaryVehicle(saved)
            }
            syncAfterWrite { syncManager.uploadVehicle(saved) }
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.insertVehicle(vehicle)
            if (vehicle.isPrimary) {
                setPrimaryVehicle(vehicle)
            }
            syncAfterWrite { syncManager.uploadVehicle(vehicle) }
        }
    }

    private suspend fun setPrimaryVehicle(primary: Vehicle) {
        allVehicles.value.forEach { v ->
            if (v.id != primary.id && v.isPrimary) {
                val updated = v.copy(isPrimary = false)
                repository.insertVehicle(updated)
                syncAfterWrite { syncManager.uploadVehicle(updated) }
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch { 
            repository.deleteVehicle(vehicle)
            syncManager.deleteVehicle(vehicle)
        }
    }

    fun addFuelEntry(mileage: Double, liters: Double, amount: Double, date: Long = System.currentTimeMillis()) {
        val vId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            val entry = FuelEntry(vehicleId = vId, mileage = mileage, liters = liters, amount = amount, date = date)
            repository.insertFuelEntry(entry)
            syncAfterWrite { syncManager.uploadFuelEntry(entry) }
            
            // Update vehicle current mileage if this is higher
            selectedVehicle.value?.let { v ->
                if (mileage > v.currentMileage) {
                    val updated = v.copy(currentMileage = mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncAfterWrite { syncManager.uploadVehicle(updated) }
                }
            }

            val next = nextOilChangeMileage.value
            if (next > 0) {
                val remaining = next - mileage
                if (remaining <= 0) {
                    _vehicleAlert.value = "CRITICAL: Oil change overdue by ${(-remaining).toInt()} km!"
                } else if (remaining < 500) {
                    _vehicleAlert.value = "Warning: Only ${remaining.toInt()} km left until oil change."
                }
            }
        }
    }

    fun updateFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            repository.insertFuelEntry(entry)
            syncAfterWrite { syncManager.uploadFuelEntry(entry) }
            
            // Check if vehicle mileage needs update
            selectedVehicle.value?.let { v ->
                if (entry.mileage > v.currentMileage) {
                    val updated = v.copy(currentMileage = entry.mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncAfterWrite { syncManager.uploadVehicle(updated) }
                }
            }
        }
    }

    fun addMaintenanceEntry(mileage: Double, cost: Double, description: String, isOilChange: Boolean, date: Long = System.currentTimeMillis()) {
        val vId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            val entry = MaintenanceEntry(
                vehicleId = vId,
                mileage = mileage, 
                cost = cost, 
                description = description, 
                type = if (isOilChange) "OIL_CHANGE" else "OTHER",
                date = date
            )
            repository.insertMaintenanceEntry(entry)
            syncAfterWrite { syncManager.uploadMaintenanceEntry(entry) }

            // Update vehicle current mileage if this is higher
            selectedVehicle.value?.let { v ->
                if (mileage > v.currentMileage) {
                    val updated = v.copy(currentMileage = mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncAfterWrite { syncManager.uploadVehicle(updated) }
                }
            }
        }
    }

    fun updateMaintenanceEntry(entry: MaintenanceEntry) {
        viewModelScope.launch {
            repository.insertMaintenanceEntry(entry)
            syncAfterWrite { syncManager.uploadMaintenanceEntry(entry) }
            
            // Check if vehicle mileage needs update
            selectedVehicle.value?.let { v ->
                if (entry.mileage > v.currentMileage) {
                    val updated = v.copy(currentMileage = entry.mileage, lastUpdated = System.currentTimeMillis())
                    repository.insertVehicle(updated)
                    syncAfterWrite { syncManager.uploadVehicle(updated) }
                }
            }
        }
    }
    
    private suspend fun recalculateVehicleMileage(vehicleId: Int) {
        val fuelEntries = repository.getAllFuelEntriesList().filter { it.vehicleId == vehicleId && !it.isDeleted }
        val maintEntries = repository.getAllMaintenanceEntriesList().filter { it.vehicleId == vehicleId && !it.isDeleted }
        
        val maxFuel = fuelEntries.maxOfOrNull { it.mileage } ?: 0.0
        val maxMaint = maintEntries.maxOfOrNull { it.mileage } ?: 0.0
        val highestEntryMileage = maxOf(maxFuel, maxMaint)
        
        repository.getVehicleById(vehicleId)?.let { vehicle ->
            // If there's an entry, and it's less than the currently cached max, we decrement it.
            // If they deleted everything, highestEntryMileage is 0.0, so we just preserve the last known.
            if (highestEntryMileage > 0 && highestEntryMileage < vehicle.currentMileage) {
                val updated = vehicle.copy(currentMileage = highestEntryMileage, lastUpdated = System.currentTimeMillis())
                repository.insertVehicle(updated)
                syncAfterWrite { syncManager.uploadVehicle(updated) }
            }
        }
    }
    
    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch { 
            repository.deleteFuelEntry(entry)
            syncManager.deleteFuelEntry(entry)
            recalculateVehicleMileage(entry.vehicleId)
        }
    }
    
    fun deleteMaintenanceEntry(entry: MaintenanceEntry) {
        viewModelScope.launch { 
            repository.deleteMaintenanceEntry(entry)
            syncManager.deleteMaintenanceEntry(entry)
            recalculateVehicleMileage(entry.vehicleId)
        }
    }

    val monthlyExpenses = allExpenses.map { expenses ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        expenses.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaysExpenses = allExpenses.map { expenses ->
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        expenses.filter {
            cal.timeInMillis = it.date
            cal.get(Calendar.DAY_OF_YEAR) == today && cal.get(Calendar.YEAR) == year
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    fun addExpense(amount: Double, category: ExpenseCategory, note: String?, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val expense = Expense(amount = amount, category = category, date = date, note = note)
            repository.insertExpense(expense)
            syncAfterWrite { syncManager.uploadExpense(expense) }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            syncManager.deleteExpense(expense)
        }
    }

    fun exportExpensesToNativePdf(context: Context, expenses: List<Expense>, title: String) {
        viewModelScope.launch {
            ExportUtils.generateExpensesPdf(
                context = context,
                expenses = expenses,
                title = title,
                businessName = businessName.value,
                ownerName = ownerName.value,
                logoUri = companyLogoUri.value?.toString(),
                signatureUri = signatureUri.value?.toString()
            )
        }
    }

    // --- Reminders ---
    val allReminders = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders = repository.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders = repository.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(title: String, note: String?, dueDate: Long?, priority: ReminderPriority, category: ReminderCategory) {
        viewModelScope.launch {
            // Ensure we have at least one list
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
            syncAfterWrite { syncManager.uploadReminder(saved) }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            // Double check listId exists (sanity)
            repository.insertReminder(reminder)
            if (reminder.dueDate != null && !reminder.isCompleted) {
                scheduler.schedule(reminder)
            } else {
                scheduler.cancel(reminder)
            }
            syncAfterWrite { syncManager.uploadReminder(reminder) }
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
            syncAfterWrite { syncManager.uploadReminder(updated) }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            scheduler.cancel(reminder)
            syncManager.deleteReminder(reminder)
        }
    }

    // --- Notes ---
    val allNotes = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(title: String, content: String, color: Int? = null, textColor: Int? = null, fontSize: Float? = null, bgImageId: Int? = null, isPinned: Boolean = false, folderId: Int? = null) {
        viewModelScope.launch {
            val note = Note(title = title, content = content, color = color, textColor = textColor, fontSize = fontSize, bgImageId = bgImageId, isPinned = isPinned, folderId = folderId)
            repository.insertNote(note)
            syncAfterWrite { syncManager.uploadNote(note) }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
            syncAfterWrite { syncManager.uploadNote(note) }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            syncManager.deleteNote(note)
        }
    }

    // --- Folders ---
    val allFolders = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFolder(name: String) {
        viewModelScope.launch {
            val folder = Folder(name = name)
            repository.insertFolder(folder)
            syncAfterWrite { syncManager.uploadFolder(folder) }
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            repository.insertFolder(folder)
            syncAfterWrite { syncManager.uploadFolder(folder) }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            syncManager.deleteFolder(folder)
        }
    }
}
