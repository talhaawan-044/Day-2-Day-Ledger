package com.example.awancoalledger.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("awan_ledger_settings", Context.MODE_PRIVATE)

    fun getBusinessName(): String = prefs.getString("biz_name", "") ?: ""
    fun setBusinessName(value: String) = prefs.edit().putString("biz_name", value).apply()

    fun getOwnerName(): String = prefs.getString("owner_name", "") ?: ""
    fun setOwnerName(value: String) = prefs.edit().putString("owner_name", value).apply()

    fun getBusinessPhone(): String = prefs.getString("biz_phone", "") ?: ""
    fun setBusinessPhone(value: String) = prefs.edit().putString("biz_phone", value).apply()

    fun getBusinessAddress(): String = prefs.getString("biz_address", "") ?: ""
    fun setBusinessAddress(value: String) = prefs.edit().putString("biz_address", value).apply()

    fun getDefaultCountryCode(): String = prefs.getString("default_country_code", "+92") ?: "+92"
    fun setDefaultCountryCode(value: String) = prefs.edit().putString("default_country_code", value).apply()

    fun isAppLockEnabled(): Boolean = prefs.getBoolean("app_lock", false)
    fun setAppLockEnabled(value: Boolean) = prefs.edit().putBoolean("app_lock", value).apply()

    fun isBiometricsEnabled(): Boolean = prefs.getBoolean("biometrics", false)
    fun setBiometricsEnabled(value: Boolean) = prefs.edit().putBoolean("biometrics", value).apply()

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)
    fun setDarkMode(value: Boolean) = prefs.edit().putBoolean("dark_mode", value).apply()

    fun isFrostedGlassEnabled(): Boolean = prefs.getBoolean("frosted_glass", true)
    fun setFrostedGlassEnabled(value: Boolean) = prefs.edit().putBoolean("frosted_glass", value).apply()

    fun getAppPin(): String = prefs.getString("app_pin", "1234") ?: "1234"
    fun setAppPin(value: String) = prefs.edit().putString("app_pin", value).apply()

    fun getAccentColorHex(): String = prefs.getString("accent_color_hex", "#007AFF") ?: "#007AFF"
    fun setAccentColorHex(value: String) = prefs.edit().putString("accent_color_hex", value).apply()

    fun getCompanyLogoUri(): String? = prefs.getString("company_logo_uri", null)
    fun setCompanyLogoUri(value: String?) = prefs.edit().putString("company_logo_uri", value).apply()

    fun getSignatureUri(): String? = prefs.getString("signature_uri", null)
    fun setSignatureUri(value: String?) = prefs.edit().putString("signature_uri", value).apply()

    fun getOilChangeInterval(): Int = prefs.getInt("oil_change_interval", 3000)
    fun setOilChangeInterval(value: Int) = prefs.edit().putInt("oil_change_interval", value).apply()

    fun isGridView(): Boolean = prefs.getBoolean("is_grid_view", false)
    fun setGridView(value: Boolean) = prefs.edit().putBoolean("is_grid_view", value).apply()

    
    fun isPartiesGridView(): Boolean = prefs.getBoolean("parties_grid_view", true)
    fun setPartiesGridView(value: Boolean) = prefs.edit().putBoolean("parties_grid_view", value).apply()

    fun isNotesGridView(): Boolean = prefs.getBoolean("notes_grid_view", true)
    fun setNotesGridView(value: Boolean) = prefs.edit().putBoolean("notes_grid_view", value).apply()

    fun isStockGridView(): Boolean = prefs.getBoolean("stock_grid_view", false)
    fun setStockGridView(value: Boolean) = prefs.edit().putBoolean("stock_grid_view", value).apply()

    fun clearImageUris() {
        prefs.edit()
            .remove("company_logo_uri")
            .remove("signature_uri")
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun getDockItems(): List<String> {
        val saved = prefs.getString("dock_items", null)
        if (saved != null) {
            return saved.split(",")
        }
        return listOf("parties", "expenses", "inventory")
    }

    fun setDockItems(items: List<String>) {
        prefs.edit().putString("dock_items", items.joinToString(",")).apply()
    }

    fun getSettingsFlow(): kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(Unit)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
