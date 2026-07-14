package com.jarvis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

// NOTE: Android WebView's built-in JS "webkitSpeechRecognition" does NOT work
// reliably (it needs a Google-hosted service not available inside an embedded
// WebView). So voice recognition is done natively here in Kotlin using
// SpeechRecognizer, and the recognized text is pushed into the web page.

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // Voice command -> real Android package name.
    // Add more apps here as you like (find package name from Play Store URL).
    private val appMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "camera" to "com.android.camera",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "instagram" to "com.instagram.android",
        "maps" to "com.google.android.apps.maps",
        "settings" to "com.android.settings"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
            100
        )

        tts = TextToSpeech(this, this)

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
        }

        webView.addJavascriptInterface(JarvisBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    inner class JarvisBridge {

        // Called from JS when a voice command matches "open <app>"
        @JavascriptInterface
        fun openApp(keyword: String) {
            runOnUiThread {
                val pkg = appMap[keyword.lowercase()]
                if (pkg == null) {
                    Toast.makeText(this@MainActivity, "'$keyword' app map me nahi hai", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this@MainActivity, "$keyword phone me install nahi hai", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Called from JS to make JARVIS speak using Android's native TTS
        // (swap this later for ElevenLabs API if you want a more "deep AI" voice)
        @JavascriptInterface
        fun speak(text: String) {
            runOnUiThread {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
            }
        }

        // Called from JS (mic button) to start REAL native voice recognition.
        // Result is pushed back into the page via onVoiceResult(text) in JS.
        @JavascriptInterface
        fun startListening() {
            runOnUiThread { beginNativeListening() }
        }

        // Opens Android's Accessibility settings so the user can manually
        // turn JARVIS's touchless control ON. Android REQUIRES this to be
        // a manual human action — no app can enable it silently.
        @JavascriptInterface
        fun openAccessibilitySettings() {
            runOnUiThread {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        // Starts the foreground service that keeps listening for "Jarvis"
        // even when this app is in the background.
        @JavascriptInterface
        fun startBackgroundListening() {
            runOnUiThread {
                val svcIntent = Intent(this@MainActivity, JarvisListenerService::class.java)
                startForegroundService(svcIntent)
                Toast.makeText(this@MainActivity, "Background listening ON", Toast.LENGTH_SHORT).show()
            }
        }

        // Stops it again — voice command "Jarvis voice command off" routes here.
        @JavascriptInterface
        fun stopBackgroundListening() {
            runOnUiThread {
                stopService(Intent(this@MainActivity, JarvisListenerService::class.java))
                Toast.makeText(this@MainActivity, "Background listening OFF", Toast.LENGTH_SHORT).show()
            }
        }

        // ---- Real phone control, used by both hand-gestures and voice ----
        // All of these need JarvisAccessibilityService to be manually turned
        // ON once (Settings > Accessibility > JARVIS). If it's off, these
        // silently do nothing — that's Android's rule, not a bug.

        @JavascriptInterface
        fun goHome() {
            runOnUiThread { JarvisAccessibilityService.instance?.goHome() }
        }

        @JavascriptInterface
        fun goBack() {
            runOnUiThread { JarvisAccessibilityService.instance?.goBack() }
        }

        @JavascriptInterface
        fun openRecents() {
            runOnUiThread { JarvisAccessibilityService.instance?.openRecents() }
        }

        // Taps the exact center of the screen — used by the POINT gesture.
        @JavascriptInterface
        fun tapCenter() {
            runOnUiThread {
                val metrics = resources.displayMetrics
                JarvisAccessibilityService.instance?.tapAt(
                    metrics.widthPixels / 2f,
                    metrics.heightPixels / 2f
                )
            }
        }

        @JavascriptInterface
        fun volumeUp() {
            runOnUiThread {
                val am = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
            }
        }

        @JavascriptInterface
        fun volumeDown() {
            runOnUiThread {
                val am = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
            }
        }

        // Lets the UI show a real ON/OFF status instead of guessing.
        @JavascriptInterface
        fun isAccessibilityEnabled(): Boolean {
            val expected = "$packageName/${JarvisAccessibilityService::class.java.name}"
            val enabledServices = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any { it.equals(expected, ignoreCase = true) }
        }
    }

    private fun beginNativeListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognizer available nahi hai", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                pushVoiceResultToWeb(text)
            }
            override fun onError(error: Int) {
                pushVoiceResultToWeb("")
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    private fun pushVoiceResultToWeb(text: String) {
        val safeText = text.replace("\\", "\\\\").replace("'", "\\'")
        webView.evaluateJavascript("onVoiceResult('$safeText')", null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
