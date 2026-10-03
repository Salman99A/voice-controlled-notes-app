package com.voicenotes.app.fragments;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.voicenotes.app.R;
import com.voicenotes.app.voice.VoiceCommandProcessor;
import com.voicenotes.app.voice.VoiceRecognitionManager;

/**
 * Interactive Voice Listening Bottom Sheet Dialog with dynamic sound wave animation,
 * live transcription, and smart intent detection.
 */
public class VoiceBottomSheetDialog extends BottomSheetDialogFragment {

    public interface VoiceDialogListener {
        void onCommandExecuted(VoiceCommandProcessor.ParsedCommand command);
        void onDictatedContent(String text);
    }

    private VoiceRecognitionManager voiceRecognitionManager;
    private VoiceDialogListener listener;

    private TextView tvVoiceSheetState;
    private TextView tvDetectedCommand;
    private TextView tvTranscribedResult;
    private MaterialButton btnSheetCancel;
    private MaterialButton btnSheetAction;
    private ImageView ivSheetMicIcon;

    private View waveBar1, waveBar2, waveBar3, waveBar4, waveBar5, waveBar6;
    private String recognizedSpeech = "";
    private VoiceCommandProcessor.ParsedCommand detectedCommand;
    private Runnable autoExecuteRunnable;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static VoiceBottomSheetDialog newInstance() {
        return new VoiceBottomSheetDialog();
    }

    public void setListener(VoiceDialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_voice_bottom_sheet, container, false);

        tvVoiceSheetState = view.findViewById(R.id.tvVoiceSheetState);
        tvDetectedCommand = view.findViewById(R.id.tvDetectedCommand);
        tvTranscribedResult = view.findViewById(R.id.tvTranscribedResult);
        btnSheetCancel = view.findViewById(R.id.btnSheetCancel);
        btnSheetAction = view.findViewById(R.id.btnSheetAction);
        ivSheetMicIcon = view.findViewById(R.id.ivSheetMicIcon);

        waveBar1 = view.findViewById(R.id.waveBar1);
        waveBar2 = view.findViewById(R.id.waveBar2);
        waveBar3 = view.findViewById(R.id.waveBar3);
        waveBar4 = view.findViewById(R.id.waveBar4);
        waveBar5 = view.findViewById(R.id.waveBar5);
        waveBar6 = view.findViewById(R.id.waveBar6);

        btnSheetCancel.setOnClickListener(v -> {
            if (autoExecuteRunnable != null) {
                mainHandler.removeCallbacks(autoExecuteRunnable);
            }
            dismiss();
        });

        btnSheetAction.setOnClickListener(v -> {
            if (autoExecuteRunnable != null) {
                mainHandler.removeCallbacks(autoExecuteRunnable);
            }
            if (voiceRecognitionManager != null && voiceRecognitionManager.isListening()) {
                voiceRecognitionManager.stopListening();
            }

            if (detectedCommand != null) {
                if (listener != null) {
                    if (detectedCommand.getType() == VoiceCommandProcessor.CommandType.DICTATE_CONTENT) {
                        listener.onDictatedContent(recognizedSpeech);
                    } else {
                        listener.onCommandExecuted(detectedCommand);
                    }
                }
                dismiss();
            } else if (!recognizedSpeech.isEmpty()) {
                if (listener != null) {
                    listener.onDictatedContent(recognizedSpeech);
                }
                dismiss();
            } else {
                // Restart listening
                startVoiceListening();
            }
        });

        ivSheetMicIcon.setOnClickListener(v -> {
            if (autoExecuteRunnable != null) {
                mainHandler.removeCallbacks(autoExecuteRunnable);
            }
            if (voiceRecognitionManager != null && voiceRecognitionManager.isListening()) {
                voiceRecognitionManager.stopListening();
                tvVoiceSheetState.setText("Paused");
            } else {
                startVoiceListening();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startVoiceListening();
    }

    private void startVoiceListening() {
        if (autoExecuteRunnable != null) {
            mainHandler.removeCallbacks(autoExecuteRunnable);
        }
        tvVoiceSheetState.setText(R.string.mic_listening);
        tvDetectedCommand.setVisibility(View.GONE);
        btnSheetAction.setText("Stop & Save");
        recognizedSpeech = "";

        if (voiceRecognitionManager == null) {
            voiceRecognitionManager = new VoiceRecognitionManager(requireContext());
        }

        voiceRecognitionManager.setCallback(new VoiceRecognitionManager.VoiceCallback() {
            @Override
            public void onListeningStarted() {
                mainHandler.post(() -> {
                    tvVoiceSheetState.setText(R.string.mic_listening);
                    ivSheetMicIcon.setBackgroundResource(R.drawable.bg_mic_active);
                });
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                mainHandler.post(() -> animateWaveBars(rmsdB));
            }

            @Override
            public void onPartialResult(String text) {
                mainHandler.post(() -> {
                    recognizedSpeech = text;
                    tvTranscribedResult.setText(text);
                });
            }

            @Override
            public void onFinalResult(String recognizedText) {
                mainHandler.post(() -> {
                    recognizedSpeech = recognizedText;
                    tvTranscribedResult.setText(recognizedText);
                    tvVoiceSheetState.setText("Executing...");
                    ivSheetMicIcon.setBackgroundResource(R.drawable.bg_mic_hero);

                    // Parse the command
                    detectedCommand = VoiceCommandProcessor.parse(recognizedText);
                    if (detectedCommand != null) {
                        tvDetectedCommand.setVisibility(View.VISIBLE);
                        tvDetectedCommand.setText("Detected: " + detectedCommand.getSummary());

                        if (detectedCommand.getType() == VoiceCommandProcessor.CommandType.DICTATE_CONTENT) {
                            btnSheetAction.setText(R.string.voice_action_save_note);
                        } else {
                            btnSheetAction.setText(R.string.voice_action_run_command);
                        }
                    }

                    // Auto-execute command/saving after 500ms delay without requiring user button click
                    if (autoExecuteRunnable != null) {
                        mainHandler.removeCallbacks(autoExecuteRunnable);
                    }
                    autoExecuteRunnable = () -> {
                        if (isAdded() && listener != null) {
                            if (detectedCommand != null && detectedCommand.getType() != VoiceCommandProcessor.CommandType.DICTATE_CONTENT) {
                                listener.onCommandExecuted(detectedCommand);
                            } else if (recognizedSpeech != null && !recognizedSpeech.trim().isEmpty()) {
                                listener.onDictatedContent(recognizedSpeech);
                            }
                            dismissAllowingStateLoss();
                        }
                    };
                    mainHandler.postDelayed(autoExecuteRunnable, 500);
                });
            }

            @Override
            public void onError(String errorMessage, int errorCode) {
                mainHandler.post(() -> {
                    if (autoExecuteRunnable != null) {
                        mainHandler.removeCallbacks(autoExecuteRunnable);
                    }
                    tvVoiceSheetState.setText("Recognition Error");
                    tvTranscribedResult.setText(errorMessage);
                    ivSheetMicIcon.setBackgroundResource(R.drawable.bg_mic_hero);
                    btnSheetAction.setText(R.string.voice_action_try_again);
                    resetWaveBars();
                });
            }

            @Override
            public void onListeningStopped() {
                mainHandler.post(() -> {
                    ivSheetMicIcon.setBackgroundResource(R.drawable.bg_mic_hero);
                    resetWaveBars();
                });
            }
        });

        voiceRecognitionManager.startListening();
    }

    private void animateWaveBars(float rmsdB) {
        float normalized = Math.max(0.1f, Math.min(1.0f, (rmsdB + 2.0f) / 12.0f));
        setBarHeight(waveBar1, 15 + (int) (normalized * 25));
        setBarHeight(waveBar2, 25 + (int) (normalized * 45));
        setBarHeight(waveBar3, 35 + (int) (normalized * 60));
        setBarHeight(waveBar4, 35 + (int) (normalized * 60));
        setBarHeight(waveBar5, 25 + (int) (normalized * 45));
        setBarHeight(waveBar6, 15 + (int) (normalized * 25));
    }

    private void setBarHeight(View bar, int dpHeight) {
        if (bar != null && isAdded()) {
            float density = getResources().getDisplayMetrics().density;
            ViewGroup.LayoutParams params = bar.getLayoutParams();
            params.height = (int) (dpHeight * density);
            bar.setLayoutParams(params);
        }
    }

    private void resetWaveBars() {
        setBarHeight(waveBar1, 15);
        setBarHeight(waveBar2, 25);
        setBarHeight(waveBar3, 35);
        setBarHeight(waveBar4, 35);
        setBarHeight(waveBar5, 25);
        setBarHeight(waveBar6, 15);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autoExecuteRunnable != null) {
            mainHandler.removeCallbacks(autoExecuteRunnable);
        }
        if (voiceRecognitionManager != null) {
            voiceRecognitionManager.stopListening();
            voiceRecognitionManager = null;
        }
    }
}
