package com.voicenotes.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.voicenotes.app.R;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.fragments.ReminderPickerDialog;
import com.voicenotes.app.reminders.ReminderManager;
import com.voicenotes.app.utils.Constants;
import com.voicenotes.app.utils.DateUtils;
import com.voicenotes.app.utils.ExportUtils;
import com.voicenotes.app.viewmodel.NoteViewModel;
import com.voicenotes.app.viewmodel.NoteViewModelFactory;
import com.voicenotes.app.voice.TTSManager;
import com.voicenotes.app.voice.VoiceRecognitionManager;

import java.util.Arrays;
import java.util.List;

/**
 * Activity for creating, viewing, editing, dictating, and managing notes and To-Do tasks.
 */
public class NoteEditActivity extends AppCompatActivity {

    private static final String TAG = "VoiceNotes";
    private static final int REQUEST_MIC_PERMISSION = 201;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 202;

    private static final List<String> REPEAT_OPTIONS = Arrays.asList(
            "Once",
            "Every 5 minutes",
            "Every 10 minutes",
            "Every 15 minutes",
            "Every 30 minutes",
            "Every 1 hour"
    );

    private static final List<Long> REPEAT_INTERVALS = Arrays.asList(
            0L,
            5 * 60 * 1000L,
            10 * 60 * 1000L,
            15 * 60 * 1000L,
            30 * 60 * 1000L,
            60 * 60 * 1000L
    );

    private NoteViewModel noteViewModel;
    private TTSManager ttsManager;
    private VoiceRecognitionManager voiceRecognitionManager;

    private RadioGroup rgNoteType;
    private RadioButton rbTypeNote;
    private RadioButton rbTypeTodo;
    private Spinner spinnerCategory;
    private Spinner spinnerRepeatInterval;
    private LinearLayout layoutRepeatInterval;

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private MaterialButton btnSetReminder;
    private MaterialCardView cardActiveReminder;
    private TextView tvActiveReminderTime;
    private ImageView btnRemoveReminder;
    private TextView tvEditModifiedDate;
    private MaterialButton btnDictate;
    private ExtendedFloatingActionButton btnSaveNote;

    private long noteId = -1;
    private Note currentNote;
    private boolean isPinned = false;
    private boolean isReminderEnabled = false;
    private long reminderTime = 0;
    private boolean isDictating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        initToolbar();
        initViews();
        initViewModel();
        initTTS();
        initVoiceRecognition();
        handleIntentData();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        rgNoteType = findViewById(R.id.rgNoteType);
        rbTypeNote = findViewById(R.id.rbTypeNote);
        rbTypeTodo = findViewById(R.id.rbTypeTodo);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerRepeatInterval = findViewById(R.id.spinnerRepeatInterval);
        layoutRepeatInterval = findViewById(R.id.layoutRepeatInterval);

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        btnSetReminder = findViewById(R.id.btnSetReminder);
        cardActiveReminder = findViewById(R.id.cardActiveReminder);
        tvActiveReminderTime = findViewById(R.id.tvActiveReminderTime);
        btnRemoveReminder = findViewById(R.id.btnRemoveReminder);
        tvEditModifiedDate = findViewById(R.id.tvEditModifiedDate);
        btnDictate = findViewById(R.id.btnDictate);
        btnSaveNote = findViewById(R.id.btnSaveNote);

        // Populate Category Spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Constants.CATEGORIES
        );
        spinnerCategory.setAdapter(categoryAdapter);

        // Populate Repeat Interval Spinner
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                REPEAT_OPTIONS
        );
        spinnerRepeatInterval.setAdapter(repeatAdapter);

        // Radio Group Type Toggle Listener
        rgNoteType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isTodo = (checkedId == R.id.rbTypeTodo);
            if (isTodo) {
                // Preselect "To-Do" category if General
                if ("General".equalsIgnoreCase(spinnerCategory.getSelectedItem().toString())) {
                    for (int i = 0; i < Constants.CATEGORIES.size(); i++) {
                        if ("To-Do".equalsIgnoreCase(Constants.CATEGORIES.get(i))) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                }
            }
            updateReminderUI();
        });

        // Reminder Actions
        btnSetReminder.setOnClickListener(v -> {
            checkNotificationPermission();
            ReminderPickerDialog dialog = ReminderPickerDialog.newInstance(reminderTime);
            dialog.setListener(timeMillis -> {
                isReminderEnabled = true;
                reminderTime = timeMillis;
                updateReminderUI();

                if (currentNote != null && noteId != -1) {
                    currentNote.setReminderEnabled(true);
                    currentNote.setReminderTime(timeMillis);
                    currentNote.setReminderRepeatInterval(getSelectedRepeatInterval());
                    currentNote.setUpdatedAt(System.currentTimeMillis());
                    noteViewModel.update(currentNote);
                    ReminderManager.scheduleReminder(NoteEditActivity.this, currentNote);
                }

                Toast.makeText(NoteEditActivity.this, "Reminder set for " + DateUtils.formatDateTime(timeMillis), Toast.LENGTH_SHORT).show();
            });
            dialog.show(getSupportFragmentManager(), "ReminderPicker");
        });

        btnRemoveReminder.setOnClickListener(v -> {
            isReminderEnabled = false;
            reminderTime = 0;
            updateReminderUI();
            Toast.makeText(NoteEditActivity.this, R.string.msg_reminder_cancelled, Toast.LENGTH_SHORT).show();
        });

        // Dictate button
        btnDictate.setOnClickListener(v -> toggleDictation());

        // Save button
        btnSaveNote.setOnClickListener(v -> saveNoteAndFinish());

        // Text watcher to update word / character count
        TextWatcher countWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCount();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etNoteTitle.addTextChangedListener(countWatcher);
        etNoteContent.addTextChangedListener(countWatcher);
    }

    private void initViewModel() {
        NoteViewModelFactory factory = new NoteViewModelFactory(getApplication());
        noteViewModel = new ViewModelProvider(this, factory).get(NoteViewModel.class);
    }

    private void initTTS() {
        ttsManager = new TTSManager(this, null);
    }

    private void initVoiceRecognition() {
        voiceRecognitionManager = new VoiceRecognitionManager(this);
        voiceRecognitionManager.setCallback(new VoiceRecognitionManager.VoiceCallback() {
            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    Log.d(TAG, "NoteEditActivity: onListeningStarted -> SpeechRecognizer ready");
                    isDictating = true;
                    btnDictate.setText("🔴 Listening...");
                    btnDictate.setIconResource(R.drawable.ic_mic);
                    btnDictate.setStrokeColorResource(R.color.status_red);
                    btnDictate.setTextColor(ContextCompat.getColor(NoteEditActivity.this, R.color.status_red));
                    btnDictate.setIconTintResource(R.color.status_red);
                });
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onPartialResult(String text) {
                runOnUiThread(() -> {
                    if (text != null && !text.trim().isEmpty()) {
                        btnDictate.setText("🔴 " + text);
                    }
                });
            }

            @Override
            public void onFinalResult(String recognizedText) {
                runOnUiThread(() -> {
                    Log.d(TAG, "NoteEditActivity: onFinalResult received -> " + recognizedText);
                    if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                        String textToInsert = recognizedText.trim();
                        String current = etNoteContent.getText().toString();
                        if (current.trim().isEmpty()) {
                            etNoteContent.setText(textToInsert);
                        } else if (current.endsWith("\n") || current.endsWith(" ")) {
                            etNoteContent.setText(current + textToInsert);
                        } else {
                            etNoteContent.setText(current + "\n" + textToInsert);
                        }
                        etNoteContent.setSelection(etNoteContent.getText().length());
                        Toast.makeText(NoteEditActivity.this, "Voice text inserted into note", Toast.LENGTH_SHORT).show();
                    }
                    stopDictation();
                });
            }

            @Override
            public void onError(String errorMessage, int errorCode) {
                runOnUiThread(() -> {
                    Log.e(TAG, "NoteEditActivity: onError -> " + errorMessage + " (code: " + errorCode + ")");
                    btnDictate.setText("⚠ Try Again");
                    btnDictate.setIconResource(R.drawable.ic_mic);
                    btnDictate.setStrokeColorResource(R.color.status_red);
                    Toast.makeText(NoteEditActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!isDictating && !isFinishing()) {
                            resetDictateButtonUI();
                        }
                    }, 2500);

                    stopDictation();
                });
            }

            @Override
            public void onListeningStopped() {
                runOnUiThread(() -> {
                    if (!isDictating) {
                        resetDictateButtonUI();
                    }
                });
            }
        });
    }

    private void toggleDictation() {
        if (isDictating) {
            stopDictation();
        } else {
            startDictation();
        }
    }

    private void startDictation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
            return;
        }

        if (!VoiceRecognitionManager.isSpeechAvailable(this)) {
            Toast.makeText(this, "Speech recognition is currently unavailable on this device.", Toast.LENGTH_LONG).show();
            return;
        }

        isDictating = true;
        btnDictate.setText("⏳ Starting...");
        btnDictate.setIconResource(R.drawable.ic_mic);
        btnDictate.setStrokeColorResource(R.color.accent);

        voiceRecognitionManager.startListening();
    }

    private void stopDictation() {
        if (voiceRecognitionManager != null) {
            voiceRecognitionManager.stopListening();
        }
        isDictating = false;
        resetDictateButtonUI();
    }

    private void resetDictateButtonUI() {
        btnDictate.setText(R.string.mic_tap_to_speak);
        btnDictate.setIconResource(R.drawable.ic_mic);
        btnDictate.setStrokeColorResource(R.color.primary_light);
        btnDictate.setTextColor(ContextCompat.getColor(this, R.color.primary_light));
        btnDictate.setIconTintResource(R.color.primary_light);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show();
                startDictation();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice input. Please allow microphone access in Settings.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted for reminders", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission is required to receive reminder alerts", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            noteId = intent.getLongExtra(Constants.EXTRA_NOTE_ID, -1);

            if (noteId != -1) {
                // Existing Note / To-Do
                noteViewModel.getNoteById(noteId).observe(this, note -> {
                    if (note != null) {
                        currentNote = note;
                        populateFields(note);
                    }
                });
            } else {
                // New note potentially with prefilled voice data
                String voiceTitle = intent.getStringExtra(Constants.EXTRA_VOICE_TITLE);
                String voiceContent = intent.getStringExtra(Constants.EXTRA_VOICE_CONTENT);
                String noteTypeExtra = intent.getStringExtra("extra_note_type");

                if (Note.TYPE_TODO.equalsIgnoreCase(noteTypeExtra)) {
                    rbTypeTodo.setChecked(true);
                } else {
                    rbTypeNote.setChecked(true);
                }

                if (voiceTitle != null && !voiceTitle.isEmpty()) {
                    etNoteTitle.setText(voiceTitle);
                }
                if (voiceContent != null && !voiceContent.isEmpty()) {
                    etNoteContent.setText(voiceContent);
                }
                updateWordCount();
            }
        }
    }

    private void populateFields(Note note) {
        etNoteTitle.setText(note.getTitle());
        etNoteContent.setText(note.getContent());

        if (note.isTodo()) {
            rbTypeTodo.setChecked(true);
        } else {
            rbTypeNote.setChecked(true);
        }

        isPinned = note.isPinned();
        isReminderEnabled = note.isReminderEnabled();
        reminderTime = note.getReminderTime();

        // Select Category in Spinner
        if (note.getCategory() != null) {
            for (int i = 0; i < Constants.CATEGORIES.size(); i++) {
                if (Constants.CATEGORIES.get(i).equalsIgnoreCase(note.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Select Repeat Interval in Spinner
        long interval = note.getReminderRepeatInterval();
        for (int i = 0; i < REPEAT_INTERVALS.size(); i++) {
            if (REPEAT_INTERVALS.get(i).equals(interval)) {
                spinnerRepeatInterval.setSelection(i);
                break;
            }
        }

        updateReminderUI();
        updateWordCount();
        invalidateOptionsMenu();
    }

    private long getSelectedRepeatInterval() {
        int pos = spinnerRepeatInterval.getSelectedItemPosition();
        if (pos >= 0 && pos < REPEAT_INTERVALS.size()) {
            return REPEAT_INTERVALS.get(pos);
        }
        return 0L;
    }

    private void updateReminderUI() {
        boolean isTodo = rbTypeTodo.isChecked();
        if (isReminderEnabled && reminderTime > 0) {
            cardActiveReminder.setVisibility(View.VISIBLE);
            tvActiveReminderTime.setText("Reminder: " + DateUtils.formatDateTime(reminderTime));
            btnSetReminder.setText("Edit Reminder");
            layoutRepeatInterval.setVisibility(isTodo ? View.VISIBLE : View.GONE);
        } else {
            cardActiveReminder.setVisibility(View.GONE);
            btnSetReminder.setText(R.string.btn_set_reminder);
            layoutRepeatInterval.setVisibility(View.GONE);
        }
    }

    private void updateWordCount() {
        String content = etNoteContent.getText().toString().trim();
        int words = content.isEmpty() ? 0 : content.split("\\s+").length;
        int chars = content.length();

        String modifiedText = (currentNote != null) ? "Edited: " + DateUtils.formatRelative(currentNote.getUpdatedAt()) : "New Note";
        tvEditModifiedDate.setText(modifiedText + " • " + words + " words • " + chars + " chars");
    }

    private void saveNoteAndFinish() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String type = rbTypeTodo.isChecked() ? Note.TYPE_TODO : Note.TYPE_NOTE;
        long repeatInterval = getSelectedRepeatInterval();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, R.string.msg_empty_note_warning, Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
        }

        if (currentNote == null) {
            currentNote = new Note();
            currentNote.setCreatedAt(System.currentTimeMillis());
            currentNote.setTaskStatus(Note.STATUS_PENDING);
        }

        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setCategory(category);
        currentNote.setNoteType(type);
        currentNote.setPinned(isPinned);
        currentNote.setReminderEnabled(isReminderEnabled);
        currentNote.setReminderTime(reminderTime);
        currentNote.setReminderRepeatInterval(repeatInterval);
        currentNote.setUpdatedAt(System.currentTimeMillis());

        if (noteId == -1) {
            noteViewModel.insert(currentNote, newId -> {
                currentNote.setId(newId);
                if (isReminderEnabled && reminderTime > System.currentTimeMillis()) {
                    ReminderManager.scheduleReminder(NoteEditActivity.this, currentNote);
                }
            });
        } else {
            noteViewModel.update(currentNote);
            if (isReminderEnabled && reminderTime > System.currentTimeMillis() && !currentNote.isCompleted()) {
                ReminderManager.scheduleReminder(this, currentNote);
            } else if (!isReminderEnabled || currentNote.isCompleted()) {
                ReminderManager.cancelReminder(this, currentNote.getId());
            }
        }

        Toast.makeText(this, R.string.msg_note_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        MenuItem pinItem = menu.findItem(R.id.action_pin);
        if (pinItem != null) {
            pinItem.setIcon(isPinned ? R.drawable.ic_pin_filled : R.drawable.ic_pin);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_read_aloud) {
            readNoteAloud();
            return true;
        } else if (id == R.id.action_pin) {
            isPinned = !isPinned;
            item.setIcon(isPinned ? R.drawable.ic_pin_filled : R.drawable.ic_pin);
            Toast.makeText(this, isPinned ? "Note pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_share) {
            Note temp = new Note(etNoteTitle.getText().toString(), etNoteContent.getText().toString(), spinnerCategory.getSelectedItem().toString());
            temp.setNoteType(rbTypeTodo.isChecked() ? Note.TYPE_TODO : Note.TYPE_NOTE);
            temp.setReminderEnabled(isReminderEnabled);
            temp.setReminderTime(reminderTime);
            ExportUtils.exportSingleNote(this, temp);
            return true;
        } else if (id == R.id.action_delete) {
            deleteCurrentNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void readNoteAloud() {
        if (ttsManager != null) {
            String title = etNoteTitle.getText().toString().trim();
            String content = etNoteContent.getText().toString().trim();
            String speech = (title.isEmpty() ? "" : title + ". ") + (content.isEmpty() ? "Empty note" : content);
            ttsManager.speak(speech);
            Toast.makeText(this, "Reading note aloud...", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCurrentNote() {
        if (currentNote != null && noteId != -1) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.msg_delete_confirm_title)
                    .setMessage(R.string.msg_delete_confirm_body)
                    .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                        if (currentNote.isReminderEnabled()) {
                            ReminderManager.cancelReminder(this, currentNote.getId());
                        }
                        noteViewModel.delete(currentNote);
                        Toast.makeText(NoteEditActivity.this, R.string.msg_note_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (voiceRecognitionManager != null && voiceRecognitionManager.isListening()) {
            stopDictation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceRecognitionManager != null) {
            voiceRecognitionManager.destroy();
            voiceRecognitionManager = null;
        }
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
