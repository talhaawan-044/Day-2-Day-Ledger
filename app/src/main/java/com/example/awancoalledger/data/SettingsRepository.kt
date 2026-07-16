package com.example.awancoalledger.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("awan_ledger_settings", Context.MODE_PRIVATE)

    fun getBusinessName(): String = prefs.getString("biz_name", "Awan Coal Traders") ?: "Awan Coal Traders"
    fun setBusinessName(value: String) = prefs.edit().putString("biz_name", value).apply()

    fun getOwnerName(): String = prefs.getString("owner_name", "Talha Awan") ?: "Talha Awan"
    fun setOwnerName(value: String) = prefs.edit().putString("owner_name", value).apply()

    fun getBusinessPhone(): String = prefs.getString("biz_phone", "+92 300 1234567") ?: "+92 300 1234567"
    fun setBusinessPhone(value: String) = prefs.edit().putString("biz_phone", value).apply()

    fun getBusinessAddress(): String = prefs.getString("biz_address", "Quetta, Baluchistan") ?: "Quetta, Baluchistan"
    fun setBusinessAddress(value: String) = prefs.edit().putString("biz_address", value).apply()

    fun getDefaultCountryCode(): String = prefs.getString("default_country_code", "+92") ?: "+92"
    fun setDefaultCountryCode(value: String) = prefs.edit().putString("default_country_code", value).apply()

    fun isAppLockEnabled(): Boolean = prefs.getBoolean("app_lock", false)
    fun setAppLockEnabled(value: Boolean) = prefs.edit().putBoolean("app_lock", value).apply()

    fun isBiometricsEnabled(): Boolean = prefs.getBoolean("biometrics", false)
    fun setBiometricsEnabled(value: Boolean) = prefs.edit().putBoolean("biometrics", value).apply()

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)
    fun setDarkMode(value: Boolean) = prefs.edit().putBoolean("dark_mode", value).apply()

    fun getAppPin(): String = prefs.getString("app_pin", "1234") ?: "1234"
    fun setAppPin(value: String) = prefs.edit().putString("app_pin", value).apply()

    fun getCompanyLogoUri(): String? = prefs.getString("company_logo_uri", null)
    fun setCompanyLogoUri(value: String?) = prefs.edit().putString("company_logo_uri", value).apply()

    fun getSignatureUri(): String? = prefs.getString("signature_uri", null)
    fun setSignatureUri(value: String?) = prefs.edit().putString("signature_uri", value).apply()

    fun getOilChangeInterval(): Int = prefs.getInt("oil_change_interval", 3000)
    fun setOilChangeInterval(value: Int) = prefs.edit().putInt("oil_change_interval", value).apply()

    fun isGridView(): Boolean = prefs.getBoolean("is_grid_view", false)
    fun setGridView(value: Boolean) = prefs.edit().putBoolean("is_grid_view", value).apply()

    fun clearImageUris() {
        prefs.edit()
            .remove("company_logo_uri")
            .remove("signature_uri")
            .apply()
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
