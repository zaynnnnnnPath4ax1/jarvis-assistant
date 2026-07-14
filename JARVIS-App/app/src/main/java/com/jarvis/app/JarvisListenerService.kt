package com.jarvis.app

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

// IMPORTANT — read this honestly before relying on it:
// This keeps a mic-listening loop alive as a "foreground service" (the
// persistent notification you see is REQUIRED by Android — it cannot be
// hidden, that's the OS telling the user "something is listening").
// - It restarts SpeechRecognizer every time it finishes a phrase, so it's
//   always listening again within ~1 second.
// - It drains battery meaningfully — this is real background listening,
//   not a free trick.
// - Some phone brands (Xiaomi/Vivo/Oppo/Samsung battery optimizers) kill
//   background services aggressively. You will likely need to manually
//   disable battery optimization for this app in phone Settings for it
//   to survive more than a few minutes.
// - When it hears "jarvis", it can trigger the AccessibilityService
//   (JarvisAccessibilityService.instance) to go home / open an app, etc.
class JarvisListenerService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val channelId = "jarvis_listener_channel"
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        startListeningLoop()
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "JARVIS Listener", NotificationManager.IMPORTANCE_LOW)
        mgr.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("JARVIS is listening")
            .setContentText("Kaho 'Jarvis' + command")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun startListeningLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase() ?: ""
                handleHeardText(text)
                restart()
            }
            override fun onError(error: Int) { restart() }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        restart()
    }

    private fun restart() {
        handler.postDelayed({
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            try { speechRecognizer?.startListening(intent) } catch (e: Exception) { /* will retry next loop */ }
        }, 400)
    }

    private fun handleHeardText(text: String) {
        if (!text.contains("jarvis")) return

        if (text.contains("voice") && text.contains("command") && text.contains("off")) {
            stopSelf()
            return
        }

        val svc = JarvisAccessibilityService.instance
        when {
            text.contains("home") -> svc?.goHome()
            text.contains("back") -> svc?.goBack()
            text.contains("recent") -> svc?.openRecents()
            text.contains("whatsapp") -> openRealApp("com.whatsapp")
            text.contains("youtube") -> openRealApp("com.google.android.youtube")
            text.contains("camera") -> openRealApp("com.android.camera")
        }
    }

    private fun openRealApp(pkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
