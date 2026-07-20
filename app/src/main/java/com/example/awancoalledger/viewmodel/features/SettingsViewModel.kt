package com.example.awancoalledger.viewmodel.features

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awancoalledger.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val repository: LedgerRepository,
    private val firebaseManager: FirebaseManager
) : ViewModel() {

    val isDarkMode = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isDarkMode() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isDarkMode())

    val accentColorHex = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getAccentColorHex() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getAccentColorHex())

    val isFrostedGlassEnabled = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isFrostedGlassEnabled() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isFrostedGlassEnabled())

    private val _isLocked = MutableStateFlow(settingsRepository.isAppLockEnabled())
    val isLocked = _isLocked.asStateFlow()

    val isAppLockEnabled = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isAppLockEnabled() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isAppLockEnabled())

    val ownerName = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getOwnerName() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getOwnerName())

    val businessName = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getBusinessName() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getBusinessName())

    val businessPhone = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getBusinessPhone() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getBusinessPhone())

    val businessAddress = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getBusinessAddress() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getBusinessAddress())

    val oilChangeInterval = settingsRepository.getSettingsFlow().map { settingsRepository.getOilChangeInterval() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getOilChangeInterval())
    val countryConfig = settingsRepository.getSettingsFlow()
        .map { 
            val code = settingsRepository.getDefaultCountryCode()
            SUPPORTED_COUNTRIES.find { it.code == code } ?: CountryConfig("Custom", code, 15)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SUPPORTED_COUNTRIES.find { it.code == settingsRepository.getDefaultCountryCode() } ?: CountryConfig("Custom", settingsRepository.getDefaultCountryCode(), 15))

    val isBiometricsEnabled = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isBiometricsEnabled() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isBiometricsEnabled())

    val companyLogoUri = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getCompanyLogoUri()?.let { Uri.parse(it) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getCompanyLogoUri()?.let { Uri.parse(it) })

    val signatureUri = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getSignatureUri()?.let { Uri.parse(it) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getSignatureUri()?.let { Uri.parse(it) })

    private val _isLogoUploading = MutableStateFlow(false)
    val isLogoUploading = _isLogoUploading.asStateFlow()

    private val _isSignatureUploading = MutableStateFlow(false)
    val isSignatureUploading = _isSignatureUploading.asStateFlow()

    val isGridView = settingsRepository.getSettingsFlow()
        .map { settingsRepository.isGridView() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.isGridView())

    val dockItems = settingsRepository.getSettingsFlow()
        .map { settingsRepository.getDockItems() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getDockItems())

    fun toggleDarkMode(enabled: Boolean) { settingsRepository.setDarkMode(enabled); syncAfterWrite() }
    fun toggleFrostedGlass(enabled: Boolean) { settingsRepository.setFrostedGlassEnabled(enabled); syncAfterWrite() }
    fun toggleAppLock(enabled: Boolean) { settingsRepository.setAppLockEnabled(enabled); if (!enabled) _isLocked.value = false; syncAfterWrite() }
    fun toggleBiometrics(enabled: Boolean) { settingsRepository.setBiometricsEnabled(enabled); syncAfterWrite() }
    fun toggleGridView(enabled: Boolean) { settingsRepository.setGridView(enabled) }
    fun setPinAndEnable(pin: String) { settingsRepository.setAppPin(pin); settingsRepository.setAppLockEnabled(true); _isLocked.value = true; syncAfterWrite() }
    fun unlock(pin: String): Boolean { if (pin == settingsRepository.getAppPin()) { _isLocked.value = false; return true }; return false }
    fun unlockWithBiometrics() { _isLocked.value = false }
    fun lock() { if (settingsRepository.isAppLockEnabled()) _isLocked.value = true }
    fun updateAccentColorHex(hex: String) { settingsRepository.setAccentColorHex(hex); syncAfterWrite() }
    fun updateOwnerName(name: String) { settingsRepository.setOwnerName(name); syncAfterWrite() }
    fun updateBusinessName(name: String) { settingsRepository.setBusinessName(name); syncAfterWrite() }
    fun updateBusinessPhone(phone: String) { settingsRepository.setBusinessPhone(phone); syncAfterWrite() }
    fun updateBusinessAddress(address: String) { settingsRepository.setBusinessAddress(address); syncAfterWrite() }
    fun updateCountryCode(code: String) { settingsRepository.setDefaultCountryCode(code); syncAfterWrite() }
    fun updateDockItems(items: List<String>) { settingsRepository.setDockItems(items); syncAfterWrite() }
    fun setOilChangeIntervalKm(interval: Int) { settingsRepository.setOilChangeInterval(interval); syncAfterWrite() }

    fun updateCompanyLogoUri(context: Context, uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                settingsRepository.setCompanyLogoUri(null)
                firebaseManager.deleteFile("company_logo.png")
                syncManager.uploadSettings()
            } else {
                _isLogoUploading.value = true
                val internalUri = com.example.awancoalledger.utils.DataExchangeUtils.copyImageToInternal(context, uri, "company_logo.png")
                settingsRepository.setCompanyLogoUri(internalUri?.toString())
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
                val internalUri = com.example.awancoalledger.utils.DataExchangeUtils.copyImageToInternal(context, uri, "signature.png")
                settingsRepository.setSignatureUri(internalUri?.toString())
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

    private fun syncAfterWrite() { viewModelScope.launch { syncManager.uploadSettings() } }
    
    // Backup & Restore
    private val _autoSnapshots = MutableStateFlow<List<java.io.File>>(emptyList())
    val autoSnapshots = _autoSnapshots.asStateFlow()

    private var lastManualSnapshotTime = 0L

    fun refreshSnapshots(context: Context) {
        val dir = java.io.File(context.filesDir, "backups")
        if (dir.exists()) {
            _autoSnapshots.value = dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }

    fun createManualSnapshot(context: Context, onResult: (Boolean) -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastManualSnapshotTime < 5000) { onResult(false); return }
        viewModelScope.launch {
            try {
                val data = repository.getBackupData()
                val json = com.example.awancoalledger.utils.DataExchangeUtils.serializeBackup(data)
                val dir = java.io.File(context.filesDir, "backups")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, "manual_${System.currentTimeMillis()}.json")
                file.writeText(json)
                com.example.awancoalledger.utils.DataExchangeUtils.cleanupBackups(context)
                lastManualSnapshotTime = System.currentTimeMillis()
                refreshSnapshots(context)
                onResult(true)
            } catch (e: Exception) { onResult(false) }
        }
    }
}
