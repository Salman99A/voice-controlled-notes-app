package com.voicenotes.app.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Manages Android SpeechRecognizer lifecycle, audio levels, and callbacks for VoiceNotes.
 */
public class VoiceRecognitionManager {

    private static final String TAG = "VoiceNotes";

    public interface VoiceCallback {
        void onListeningStarted();
        void onRmsChanged(float rmsdB);
        void onPartialResult(String text);
        void onFinalResult(String recognizedText);
        void onError(String errorMessage, int errorCode);
        void onListeningStopped();
    }

    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private VoiceCallback callback;
    private boolean isListening = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VoiceRecognitionManager(Context context) {
        this.context = context;
    }

    public void setCallback(VoiceCallback callback) {
        this.callback = callback;
    }

    public static boolean isSpeechAvailable(Context context) {
        return context != null && SpeechRecognizer.isRecognitionAvailable(context);
    }

    public void startListening() {
        Log.d(TAG, "VoiceRecognitionManager: startListening requested");

        if (!isSpeechAvailable(context)) {
            Log.e(TAG, "VoiceRecognitionManager: Speech recognition is not available on this device");
            if (callback != null) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Speech recognition is not available on this device", -1);
                    }
                });
            }
            return;
        }

        // Clean up any existing active session first
        stopListening();

        mainHandler.post(() -> {
            try {
                Log.d(TAG, "VoiceRecognitionManager: Initializing SpeechRecognizer");
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

                if (speechRecognizer == null) {
                    Log.e(TAG, "VoiceRecognitionManager: SpeechRecognizer.createSpeechRecognizer returned null");
                    if (callback != null) {
                        callback.onError("Speech recognition service is unavailable on this device", -1);
                    }
                    return;
                }

                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        Log.d(TAG, "VoiceRecognitionManager: onReadyForSpeech");
                        isListening = true;
                        if (callback != null) {
                            callback.onListeningStarted();
                        }
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        Log.d(TAG, "VoiceRecognitionManager: onBeginningOfSpeech");
                        isListening = true;
                        if (callback != null) {
                            callback.onListeningStarted();
                        }
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                        if (callback != null) {
                            callback.onRmsChanged(rmsdB);
                        }
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                        Log.d(TAG, "VoiceRecognitionManager: onEndOfSpeech");
                        isListening = false;
                        if (callback != null) {
                            callback.onListeningStopped();
                        }
                    }

                    @Override
                    public void onError(int error) {
                        Log.e(TAG, "VoiceRecognitionManager: onError code=" + error + " (" + getErrorMessage(error) + ")");
                        isListening = false;
                        String errorMsg = getErrorMessage(error);
                        if (callback != null) {
                            callback.onError(errorMsg, error);
                            callback.onListeningStopped();
                        }
                    }

                    @Override
                    public void onResults(Bundle results) {
                        Log.d(TAG, "VoiceRecognitionManager: onResults received");
                        isListening = false;
                        ArrayList<String> matches = (results != null) ?
                                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) : null;

                        if (matches != null && !matches.isEmpty() && !matches.get(0).trim().isEmpty()) {
                            String recognizedText = matches.get(0).trim();
                            Log.d(TAG, "VoiceRecognitionManager: Recognized text -> " + recognizedText);
                            if (callback != null) {
                                callback.onFinalResult(recognizedText);
                            }
                        } else {
                            Log.w(TAG, "VoiceRecognitionManager: No speech matches found");
                            if (callback != null) {
                                callback.onError("No speech detected. Please try again.", SpeechRecognizer.ERROR_NO_MATCH);
                            }
                        }
                        if (callback != null) {
                            callback.onListeningStopped();
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                        if (partialResults != null) {
                            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty() && !matches.get(0).trim().isEmpty()) {
                                String partial = matches.get(0).trim();
                                Log.d(TAG, "VoiceRecognitionManager: Partial result -> " + partial);
                                if (callback != null) {
                                    callback.onPartialResult(partial);
                                }
                            }
                        }
                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {
                    }
                });

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                if (context != null) {
                    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
                }

                Log.d(TAG, "VoiceRecognitionManager: Starting speech recognition intent");
                speechRecognizer.startListening(intent);

            } catch (Exception e) {
                Log.e(TAG, "VoiceRecognitionManager: Exception during startListening", e);
                isListening = false;
                if (callback != null) {
                    callback.onError("Speech recognition error: " + e.getLocalizedMessage(), -1);
                }
            }
        });
    }

    public void stopListening() {
        Log.d(TAG, "VoiceRecognitionManager: stopListening");
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception e) {
                Log.e(TAG, "VoiceRecognitionManager: Error stopping SpeechRecognizer", e);
            }
            speechRecognizer = null;
        }
    }

    public void destroy() {
        stopListening();
        callback = null;
    }

    public boolean isListening() {
        return isListening;
    }

    public String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error. Please check your microphone.";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Speech recognition error. Please try again.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission is required for voice input.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network connection error for speech recognition.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Speech recognition network request timed out.";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech detected. Please try again.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Speech recognizer is busy. Please try again in a moment.";
            case SpeechRecognizer.ERROR_SERVER:
                return "Speech recognition server error. Please try again.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech detected within timeout. Please try again.";
            default:
                return "Speech recognition error (" + errorCode + "). Please try again.";
        }
    }
}
