package com.example.aichat.ui

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import java.util.Locale

/**
 * Simple helper wrapping Android's SpeechRecognizer and TextToSpeech APIs.
 * Not currently integrated into the UI; reserved for future voice mode support.
 */
class VoiceControls(private val context: Context) : RecognitionListener, TextToSpeech.OnInitListener {
    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val textToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var onResult: ((String) -> Unit)? = null

    init {
        speechRecognizer.setRecognitionListener(this)
    }

    fun startListening(onResult: (String) -> Unit) {
        this.onResult = onResult
        val intent = RecognizerIntent.getVoiceDetailsIntent(context).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.startListening(intent)
    }

    fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "uttId")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {}
    override fun onResults(results: Bundle?) {
        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        onResult?.invoke(data?.firstOrNull().orEmpty())
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
}