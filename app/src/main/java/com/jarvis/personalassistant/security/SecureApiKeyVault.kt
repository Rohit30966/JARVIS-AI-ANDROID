package com.jarvis.personalassistant.security

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-backed AES-256 key vault for user-supplied API keys and local model paths.
 * Zero API secrets are ever hardcoded in the source repository or APK.
 */
class SecureApiKeyVault(private val context: Context) {

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveGeminiApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API, apiKey.trim()).apply()
    }

    fun getGeminiApiKey(): String? {
        return encryptedPrefs.getString(KEY_GEMINI_API, null)?.takeIf { it.isNotBlank() }
    }

    fun saveLocalModelPath(modelId: String, fileUriOrPath: String) {
        encryptedPrefs.edit().putString("local_model_$modelId", fileUriOrPath.trim()).apply()
    }

    fun getLocalModelPath(modelId: String): String? {
        return encryptedPrefs.getString("local_model_$modelId", null)?.takeIf { it.isNotBlank() }
    }

    fun setStrictLocalPrivacyMode(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_STRICT_PRIVACY, enabled).apply()
    }

    fun isStrictLocalPrivacyMode(): Boolean {
        return encryptedPrefs.getBoolean(KEY_STRICT_PRIVACY, false)
    }

    fun wipeAllSecrets() {
        encryptedPrefs.edit().clear().apply()
    }

    fun isMicrophoneGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(context.packageName)
    }

    fun isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
        val expected = ComponentName(context, serviceClass).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expected)
    }

    companion object {
        private const val PREFS_FILE_NAME = "jarvis_encrypted_key_vault"
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_STRICT_PRIVACY = "strict_local_privacy_mode"
    }
}
