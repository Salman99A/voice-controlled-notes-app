package com.voicenotes.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.voicenotes.app.R;

/**
 * BottomSheet Dialog displaying reference guide of supported voice commands.
 */
public class VoiceHelpDialog extends BottomSheetDialogFragment {

    public static VoiceHelpDialog newInstance() {
        return new VoiceHelpDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_voice_help, container, false);
        MaterialButton btnGotIt = view.findViewById(R.id.btnGotIt);
        btnGotIt.setOnClickListener(v -> dismiss());
        return view;
    }
}
