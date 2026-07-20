package com.example.awancoalledger.viewmodel.features

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import com.example.awancoalledger.viewmodel.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.Intent
import com.example.awancoalledger.utils.BackupData
import com.example.awancoalledger.utils.DataExchangeUtils

class AuthViewModel(
    private val firebaseManager: FirebaseManager,
    private val syncManager: SyncManager,
    private val repository: LedgerRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currentUser = firebaseManager.currentUser
    val isAnonymous = currentUser.map { it?.isAnonymous == true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseManager.isAnonymous())

    private var isPendingLoginConfirmation = false
    private var pendingCredential: com.google.firebase.auth.AuthCredential? = null

    private val _showConflictDialog = MutableStateFlow(false)
    val showConflictDialog: StateFlow<Boolean> = _showConflictDialog.asStateFlow()

    private val _needsRestart = MutableStateFlow(false)
    val needsRestart: StateFlow<Boolean> = _needsRestart.asStateFlow()

    init {
        if (firebaseManager.getUserId() == null) {
            viewModelScope.launch {
                firebaseManager.signInAnonymously()
            }
        }
    }

    fun linkAccount(credential: com.google.firebase.auth.AuthCredential, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseManager.linkWithCredential(credential)
            if (result.isSuccess) {
                syncManager.startSync()
                syncManager.uploadAll()
                onResult(true, null)
            } else {
                val exception = result.exceptionOrNull()
                if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
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
            
            syncManager.stopSync()
            repository.clearAllData()
            settingsRepository.clearImageUris()
            
            val result = firebaseManager.signInWithCredential(credential)
            if (result.isSuccess) {
                syncManager.startSync()
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
                syncManager.uploadAll()
            } else {
                syncManager.stopSync()
                repository.clearAllData()
                settingsRepository.clearImageUris()
                syncManager.startSync()
                _needsRestart.value = true
            }
            isPendingLoginConfirmation = false
        }
    }

    fun cancelLogin(context: Context) {
        firebaseManager.signOut(context) {
            isPendingLoginConfirmation = false
            syncManager.stopSync()
        }
    }

    fun signOut(context: Context, clearData: Boolean = true) {
        firebaseManager.signOut(context) {
            if (clearData) {
                viewModelScope.launch {
                    syncManager.stopSync()
                    repository.clearAllData()
                    settingsRepository.clearAll()
                    try {
                        val logoFile = java.io.File(context.filesDir, "company_logo.png")
                        if (logoFile.exists()) logoFile.delete()
                        val sigFile = java.io.File(context.filesDir, "signature.png")
                        if (sigFile.exists()) sigFile.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                syncManager.stopSync()
            }
        }
    }

    fun getGoogleSignInClient(context: Context): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        return firebaseManager.getGoogleSignInClient(context)
    }
    fun shareBackup(context: Context, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val data: com.example.awancoalledger.utils.BackupData = repository.getBackupData()
                
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
                        settings["darkMode"]?.toBoolean()?.let { settingsRepository.setDarkMode(it) }
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

}
