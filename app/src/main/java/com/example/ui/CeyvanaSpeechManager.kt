package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class CeyvanaSpeechManager private constructor(context: Context) {
    private var tts: TextToSpeech? = null
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var speechTextToPlayOnReady: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("si", "LK")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("CeyvanaSpeech", "Sinhala language is not supported or missing data on this device, falling back to English/Default.")
                    tts?.setLanguage(Locale.US)
                }
                _isReady.value = true
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })

                speechTextToPlayOnReady?.let {
                    speakNow(it)
                    speechTextToPlayOnReady = null
                }
            } else {
                Log.e("CeyvanaSpeech", "TextToSpeech Initialization failed with status: $status")
            }
        }
    }

    fun speak(text: String) {
        if (_isReady.value) {
            speakNow(text)
        } else {
            speechTextToPlayOnReady = text
        }
    }

    private fun speakNow(text: String) {
        try {
            _isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CeyvanaIntroUtteranceId")
        } catch (e: Exception) {
            Log.e("CeyvanaSpeech", "Failed to speak", e)
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e("CeyvanaSpeech", "Failed to stop", e)
        }
    }

    fun shutdown() {
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("CeyvanaSpeech", "Failed to shutdown", e)
        }
        instance = null
    }

    companion object {
        @Volatile
        private var instance: CeyvanaSpeechManager? = null

        fun getInstance(context: Context): CeyvanaSpeechManager {
            return instance ?: synchronized(this) {
                instance ?: CeyvanaSpeechManager(context).also { instance = it }
            }
        }
    }
}
