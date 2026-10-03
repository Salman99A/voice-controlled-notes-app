package com.voicenotes.app.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * Text-to-Speech manager for reading notes aloud.
 */
public class TTSManager {

    public interface TTSCallback {
        void onTTSInitialized(boolean success);
        void onSpeechStarted();
        void onSpeechCompleted();
        void onSpeechError(String errorMessage);
    }

    private final Context context;
    private TextToSpeech tts;
    private boolean isInitialized = false;
    private TTSCallback callback;
    private float speechRate = 1.0f;
    private float speechPitch = 1.0f;

    public TTSManager(Context context, TTSCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        initTTS();
    }

    private void initTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                tts.setSpeechRate(speechRate);
                tts.setPitch(speechPitch);
                isInitialized = true;

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        if (callback != null) {
                            callback.onSpeechStarted();
                        }
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (callback != null) {
                            callback.onSpeechCompleted();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if (callback != null) {
                            callback.onSpeechError("Text-to-speech error occurred");
                        }
                    }
                });

                if (callback != null) {
                    callback.onTTSInitialized(true);
                }
            } else {
                isInitialized = false;
                if (callback != null) {
                    callback.onTTSInitialized(false);
                }
            }
        });
    }

    public void setSpeechRate(float rate) {
        this.speechRate = rate;
        if (tts != null && isInitialized) {
            tts.setSpeechRate(rate);
        }
    }

    public void setPitch(float pitch) {
        this.speechPitch = pitch;
        if (tts != null && isInitialized) {
            tts.setPitch(pitch);
        }
    }

    public void speak(String text) {
        if (tts != null && isInitialized && text != null && !text.trim().isEmpty()) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VoiceNotesTTS_" + System.currentTimeMillis());
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "VoiceNotesTTS");
        }
    }

    public void stop() {
        if (tts != null && isInitialized) {
            tts.stop();
        }
    }

    public boolean isSpeaking() {
        return tts != null && isInitialized && tts.isSpeaking();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        isInitialized = false;
    }
}
