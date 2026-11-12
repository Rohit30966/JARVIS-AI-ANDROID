# JARVIS AI (`com.jarvis.personalassistant`) — Android 15 Personal AI Assistant

An Iron Man JARVIS-inspired, privacy-first, multilingual (**English, हिंदी, and Hinglish**) Android 15 project structured for **GitHub Actions cloud APK compilation**.

---

## 1. Honest Component Implementation Status

| Component / Feature | Status in This Repository | Technical Details |
| :--- | :--- | :--- |
| **Android Gradle Project Structure** | **IMPLEMENTED** | Standard root `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `gradle/libs.versions.toml`, and `app/build.gradle.kts` (`compileSdk = 35`, `targetSdk = 35`). |
| **GitHub Actions Cloud Build Workflow** | **IMPLEMENTED** | `.github/workflows/build-apk.yml` configured with JDK 17 (`actions/setup-java@v4`) and Gradle 8.10.2 (`gradle/actions/setup-gradle@v4`). |
| **11 Jetpack Compose HUD Modules** | **IMPLEMENTED** | `JarvisHudScreens.kt`, `JarvisOrbView.kt`, `GlassPanel.kt`, `JarvisTheme.kt` (`app/src/main/java/com/jarvis/personalassistant/ui/...`). |
| **Gemini 2.5 Flash / Flash-Lite (Cloud AI)** | **IMPLEMENTED** | Real HTTPS REST client via `OkHttp` in `ModelRouter.kt` (`callGeminiRestApi`). Requires user's free-tier Gemini API key stored in `SecureApiKeyVault.kt`. |
| **Offline Bilingual Intent & Rule Engine** | **IMPLEMENTED** | Deterministic English/Hindi/Hinglish parser in `LocalizationManager.kt` for alarms, app launching, system settings, dialer, and WhatsApp summaries. |
| **Local Gemma 3 & Qwen 3.5 (LiteRT-LM)** | **NOT IMPLEMENTED (Adapter Stub Only)** | `LiteRtLmBridge.kt` is an architectural placeholder/bridge only. Native LiteRT-LM JNI libraries and multi-hundred-MB `.litertlm` model weights are **not** bundled or active. When offline or without a Gemini API key, `ModelRouter` uses the deterministic `OFFLINE_BILINGUAL_CORE` rule engine. |
| **WhatsApp Notification & Export Intelligence** | **IMPLEMENTED** | `JarvisNotificationListenerService.kt` + `WhatsAppIntelligenceEngine.kt` (local priority classification, draft reply staging with user confirmation gate, and `.txt` chat export parser). |
| **Room Database, WorkManager & Key Vault** | **IMPLEMENTED** | `JarvisDatabase.kt`, `AutomationCenterManager.kt`, `JarvisBriefingWorker.kt`, and `SecureApiKeyVault.kt` (AES256_GCM). |

---

## 2. Exact Project Directory Structure

```text
.
├── .gitignore
├── .github/
│   └── workflows/
│       └── build-apk.yml
├── README.md
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── docs/
│   ├── BUILD_VERIFICATION_LOG.txt
│   ├── RELEASE_NOTES.md
│   └── SECURITY_AND_PRIVACY.md
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/jarvis/personalassistant/
        │   │   ├── JarvisApplication.kt
        │   │   ├── MainActivity.kt
        │   │   ├── ai/
        │   │   │   ├── LiteRtLmBridge.kt
        │   │   │   └── ModelRouter.kt
        │   │   ├── automation/
        │   │   │   ├── AutomationCenterManager.kt
        │   │   │   └── JarvisBriefingWorker.kt
        │   │   ├── control/
        │   │   │   ├── AndroidPhoneController.kt
        │   │   │   └── JarvisAccessibilityService.kt
        │   │   ├── data/local/
        │   │   │   └── JarvisDatabase.kt
        │   │   ├── files/
        │   │   │   └── FileIntelligenceAnalyzer.kt
        │   │   ├── i18n/
        │   │   │   └── LocalizationManager.kt
        │   │   ├── research/
        │   │   │   └── WebResearchAgent.kt
        │   │   ├── security/
        │   │   │   └── SecureApiKeyVault.kt
        │   │   ├── ui/
        │   │   │   ├── components/
        │   │   │   │   ├── GlassPanel.kt
        │   │   │   │   └── JarvisOrbView.kt
        │   │   │   ├── screens/
        │   │   │   │   └── JarvisHudScreens.kt
        │   │   │   └── theme/
        │   │   │       └── JarvisTheme.kt
        │   │   ├── voice/
        │   │   │   ├── JarvisVoiceController.kt
        │   │   │   └── JarvisVoiceForegroundService.kt
        │   │   └── whatsapp/
        │   │       ├── JarvisNotificationListenerService.kt
        │   │       └── WhatsAppIntelligenceEngine.kt
        │   └── res/
        │       ├── values/
        │       │   ├── strings.xml
        │       │   └── themes.xml
        │       ├── values-hi/
        │       │   └── strings.xml
        │       └── xml/
        │           └── accessibility_service_config.xml
        └── test/
            └── java/com/jarvis/personalassistant/
                └── JarvisCoreUnitTest.kt
```
