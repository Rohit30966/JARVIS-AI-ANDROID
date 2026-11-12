package com.jarvis.personalassistant

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jarvis.personalassistant.security.SecureApiKeyVault
import com.jarvis.personalassistant.ui.screens.JarvisMasterHud
import com.jarvis.personalassistant.ui.theme.JarvisTheme
import com.jarvis.personalassistant.voice.JarvisVoiceController

class MainActivity : ComponentActivity() {

    private lateinit var voiceController: JarvisVoiceController
    private lateinit var keyVault: SecureApiKeyVault

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        keyVault = (application as JarvisApplication).keyVault
        voiceController = JarvisVoiceController(this) { _ -> }

        requestEssentialRuntimePermissions()

        setContent {
            JarvisTheme {
                val isListening by voiceController.isListening.collectAsState()
                val isSpeaking by voiceController.isSpeaking.collectAsState()
                val rmsLevel by voiceController.rmsLevel.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    JarvisMasterHud(
                        keyVault = keyVault,
                        onStartVoice = { lang -> voiceController.startListening(lang) },
                        onStopVoice = { voiceController.stopListening() },
                        onSpeakText = { text, lang -> voiceController.speak(text, lang) },
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        rmsLevel = rmsLevel
                    )
                }
            }
        }
    }

    private fun requestEssentialRuntimePermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.release()
    }
}
