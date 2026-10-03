package com.voicenotes.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.voicenotes.app.R;
import com.voicenotes.app.fragments.VoiceHelpDialog;
import com.voicenotes.app.utils.Constants;
import com.voicenotes.app.utils.ExportUtils;
import com.voicenotes.app.utils.ThemeHelper;
import com.voicenotes.app.viewmodel.NoteViewModel;
import com.voicenotes.app.viewmodel.NoteViewModelFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Settings Activity for configuring appearance, voice commands, TTS rate, notifications, sounds, and backups.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final List<String> SOUND_OPTIONS = Arrays.asList(
            "Default",
            "Classic",
            "Soft",
            "Attention",
            "Urgent",
            "Silent"
    );

    private NoteViewModel noteViewModel;
    private SharedPreferences prefs;

    private RadioGroup rgTheme;
    private RadioButton rbThemeLight;
    private RadioButton rbThemeDark;
    private RadioButton rbThemeSystem;

    private SwitchMaterial switchVoiceCommands;
    private SwitchMaterial switchReminders;
    private SwitchMaterial switchVibration;
    private Spinner spinnerReminderSound;

    private SeekBar sbTtsSpeed;
    private SeekBar sbTtsPitch;
    private TextView tvTtsSpeedLabel;
    private TextView tvTtsPitchLabel;
    private MaterialButton btnVoiceHelpGuide;
    private LinearLayout btnExportAllNotes;
    private LinearLayout btnClearAllNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        initToolbar();
        initViews();
        initViewModel();
        loadSettings();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rgTheme = findViewById(R.id.rgTheme);
        rbThemeLight = findViewById(R.id.rbThemeLight);
        rbThemeDark = findViewById(R.id.rbThemeDark);
        rbThemeSystem = findViewById(R.id.rbThemeSystem);

        switchVoiceCommands = findViewById(R.id.switchVoiceCommands);
        switchReminders = findViewById(R.id.switchReminders);
        switchVibration = findViewById(R.id.switchVibration);
        spinnerReminderSound = findViewById(R.id.spinnerReminderSound);

        sbTtsSpeed = findViewById(R.id.sbTtsSpeed);
        sbTtsPitch = findViewById(R.id.sbTtsPitch);
        tvTtsSpeedLabel = findViewById(R.id.tvTtsSpeedLabel);
        tvTtsPitchLabel = findViewById(R.id.tvTtsPitchLabel);
        btnVoiceHelpGuide = findViewById(R.id.btnVoiceHelpGuide);
        btnExportAllNotes = findViewById(R.id.btnExportAllNotes);
        btnClearAllNotes = findViewById(R.id.btnClearAllNotes);

        // Sound Spinner Adapter
        ArrayAdapter<String> soundAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                SOUND_OPTIONS
        );
        spinnerReminderSound.setAdapter(soundAdapter);

        spinnerReminderSound.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String choice = SOUND_OPTIONS.get(position);
                prefs.edit().putString(Constants.KEY_REMINDER_SOUND, choice).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Theme Selection
        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbThemeLight) {
                ThemeHelper.setThemeMode(this, Constants.THEME_LIGHT);
            } else if (checkedId == R.id.rbThemeDark) {
                ThemeHelper.setThemeMode(this, Constants.THEME_DARK);
            } else if (checkedId == R.id.rbThemeSystem) {
                ThemeHelper.setThemeMode(this, Constants.THEME_SYSTEM);
            }
        });

        // Switches
        switchVoiceCommands.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.KEY_VOICE_COMMANDS_ENABLED, isChecked).apply();
        });

        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.KEY_REMINDERS_ENABLED, isChecked).apply();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.KEY_VIBRATION_ENABLED, isChecked).apply();
        });

        // TTS Speed
        sbTtsSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.5f + (progress / 20.0f) * 1.5f;
                tvTtsSpeedLabel.setText(String.format("Text-to-Speech Speed (%.1fx)", speed));
                if (fromUser) {
                    prefs.edit().putFloat(Constants.KEY_TTS_SPEED, speed).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // TTS Pitch
        sbTtsPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = 0.5f + (progress / 20.0f) * 1.5f;
                tvTtsPitchLabel.setText(String.format("Text-to-Speech Pitch (%.1fx)", pitch));
                if (fromUser) {
                    prefs.edit().putFloat(Constants.KEY_TTS_PITCH, pitch).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnVoiceHelpGuide.setOnClickListener(v -> {
            VoiceHelpDialog.newInstance().show(getSupportFragmentManager(), "VoiceHelpGuideDialog");
        });

        btnExportAllNotes.setOnClickListener(v -> {
            noteViewModel.getAllNotesSync(notes -> runOnUiThread(() -> {
                ExportUtils.exportAllNotes(SettingsActivity.this, notes);
            }));
        });

        btnClearAllNotes.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_delete_all_confirm_title)
                    .setMessage(R.string.msg_delete_all_confirm_body)
                    .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                        noteViewModel.deleteAllNotes();
                        Toast.makeText(SettingsActivity.this, R.string.msg_all_notes_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        });
    }

    private void initViewModel() {
        NoteViewModelFactory factory = new NoteViewModelFactory(getApplication());
        noteViewModel = new ViewModelProvider(this, factory).get(NoteViewModel.class);
    }

    private void loadSettings() {
        int currentTheme = ThemeHelper.getThemeMode(this);
        if (currentTheme == Constants.THEME_LIGHT) {
            rbThemeLight.setChecked(true);
        } else if (currentTheme == Constants.THEME_DARK) {
            rbThemeDark.setChecked(true);
        } else {
            rbThemeSystem.setChecked(true);
        }

        boolean voiceCmdEnabled = prefs.getBoolean(Constants.KEY_VOICE_COMMANDS_ENABLED, true);
        switchVoiceCommands.setChecked(voiceCmdEnabled);

        boolean remindersEnabled = prefs.getBoolean(Constants.KEY_REMINDERS_ENABLED, true);
        switchReminders.setChecked(remindersEnabled);

        boolean vibrationEnabled = prefs.getBoolean(Constants.KEY_VIBRATION_ENABLED, true);
        switchVibration.setChecked(vibrationEnabled);

        String soundChoice = prefs.getString(Constants.KEY_REMINDER_SOUND, "Default");
        for (int i = 0; i < SOUND_OPTIONS.size(); i++) {
            if (SOUND_OPTIONS.get(i).equalsIgnoreCase(soundChoice)) {
                spinnerReminderSound.setSelection(i);
                break;
            }
        }

        float ttsSpeed = prefs.getFloat(Constants.KEY_TTS_SPEED, 1.0f);
        int speedProgress = (int) (((ttsSpeed - 0.5f) / 1.5f) * 20);
        sbTtsSpeed.setProgress(Math.max(0, Math.min(20, speedProgress)));
        tvTtsSpeedLabel.setText(String.format("Text-to-Speech Speed (%.1fx)", ttsSpeed));

        float ttsPitch = prefs.getFloat(Constants.KEY_TTS_PITCH, 1.0f);
        int pitchProgress = (int) (((ttsPitch - 0.5f) / 1.5f) * 20);
        sbTtsPitch.setProgress(Math.max(0, Math.min(20, pitchProgress)));
        tvTtsPitchLabel.setText(String.format("Text-to-Speech Pitch (%.1fx)", ttsPitch));
    }
}
