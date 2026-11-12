# Security, Privacy & Legal Compliance Architecture

1. **Zero Embedded Secrets**: No API keys, OAuth tokens, or signing keystores are stored in the repository or compiled APK. Users enter their personal free-tier Gemini API key at runtime, which is encrypted using Android's hardware-backed `MasterKey` (`AES256_GCM`) via `EncryptedSharedPreferences`.
2. **WhatsApp Legal & Technical Boundaries**:
   - JARVIS never attempts to bypass WhatsApp end-to-end encryption, SQLite databases, or authentication.
   - Real-time message awareness relies strictly on `NotificationListenerService` after explicit user opt-in.
   - Historical chat analysis operates strictly on user-exported `.txt` files via Android's Storage Access Framework.
   - Replies are staged as drafts and require explicit user interaction in WhatsApp before sending.
3. **Strict Local Privacy Mode**: When enabled in Module 11, `ModelRouter` forces all WhatsApp notification summaries and local file analyses to run 100% on-device.
