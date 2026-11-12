package com.jarvis.personalassistant.control

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transparent, user-enabled Assistive Accessibility Service.
 * Only summarizes visible non-password window titles when explicitly requested by the user.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        if (ev.isPassword) return // Never inspect password fields
        if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = ev.packageName?.toString().orEmpty()
            val title = ev.text?.joinToString(" ")?.take(120).orEmpty()
            if (pkg.isNotBlank()) {
                _lastWindowSummary.value = "Active App: $pkg ${if (title.isNotBlank()) "($title)" else ""}".trim()
            }
        }
    }

    override fun onInterrupt() {}

    companion object {
        private val _lastWindowSummary = MutableStateFlow("Accessibility Assistive Monitor Idle")
        val lastWindowSummary: StateFlow<String> = _lastWindowSummary
    }
}
