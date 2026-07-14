package com.jarvis.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

// This service is what actually gives "touchless full phone control".
// It CANNOT be turned on by the app itself — Android requires the human
// to manually enable it once at: Settings > Accessibility > JARVIS > On.
// This is a deliberate OS security rule (same friction Google's own
// "Voice Access" app has) so that no app can silently take control.
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        // Lets MainActivity (or a future background service) reach the
        // running instance to trigger actions, once the user enabled it.
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun goHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    // Taps at a screen coordinate anywhere on the phone, in any app.
    fun tapAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // Swipes anywhere on the phone, in any app — this is what a
    // hand-gesture (open palm = swipe up, fist = tap, etc.) would call.
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 250) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
