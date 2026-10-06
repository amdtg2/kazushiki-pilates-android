package com.kazushiki.pilates.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Speaks prompts aloud, ducking any music that's playing.
 * Each new prompt interrupts the previous one. Prompts asked for before the speech engine is
 * ready are held and the latest one is spoken once it is.
 */
class VoiceCoach(context: Context) {
    private val appContext: Context = context.applicationContext ?: context
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager: AudioManager? = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { }
        .build()

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isShutDown = false
    private var hasFocus = false
    private var pendingText: String? = null
    private var utteranceCount = 0
    private var lastUtteranceID: String? = null

    init {
        tts = try {
            TextToSpeech(appContext) { status -> mainHandler.post { onInit(status) } }
        } catch (e: Exception) {
            null
        }
    }

    fun speak(text: String) {
        onMain { speakNow(text) }
    }

    fun stop() {
        onMain {
            pendingText = null
            try {
                tts?.stop()
            } catch (e: Exception) {
                // Ignore engine errors.
            }
            abandonFocus()
        }
    }

    /** Releases the speech engine. The coach can't speak after this. */
    fun shutdown() {
        onMain {
            pendingText = null
            isShutDown = true
            isReady = false
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                // Ignore engine errors.
            }
            tts = null
            abandonFocus()
        }
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (isShutDown || engine == null) return
        if (status != TextToSpeech.SUCCESS) return
        try {
            val result = engine.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.getDefault())
            }
            engine.setSpeechRate(0.95f)
            engine.setAudioAttributes(attributes)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    mainHandler.post { finished(utteranceId) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { finished(utteranceId) }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post { finished(utteranceId) }
                }
            })
        } catch (e: Exception) {
            // Speak with the engine's defaults.
        }
        isReady = true
        val waiting = pendingText
        pendingText = null
        if (waiting != null) speakNow(waiting)
    }

    private fun speakNow(text: String) {
        if (isShutDown) return
        val engine = tts
        if (!isReady || engine == null) {
            pendingText = text
            return
        }
        requestFocus()
        utteranceCount += 1
        val id = "kp-voice-$utteranceCount"
        lastUtteranceID = id
        val result = try {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (e: Exception) {
            TextToSpeech.ERROR
        }
        if (result == TextToSpeech.ERROR) abandonFocus()
    }

    /** Music comes back up once the latest prompt has been said. */
    private fun finished(utteranceID: String?) {
        if (utteranceID != null && utteranceID == lastUtteranceID) abandonFocus()
    }

    private fun requestFocus() {
        if (hasFocus) return
        try {
            audioManager?.requestAudioFocus(focusRequest)
            hasFocus = true
        } catch (e: Exception) {
            // Speak without focus.
        }
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        try {
            audioManager?.abandonAudioFocusRequest(focusRequest)
        } catch (e: Exception) {
            // Ignore.
        }
        hasFocus = false
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }
}
