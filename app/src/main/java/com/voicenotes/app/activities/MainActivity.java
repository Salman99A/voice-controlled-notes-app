package com.voicenotes.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.voicenotes.app.R;
import com.voicenotes.app.adapters.CategoryAdapter;
import com.voicenotes.app.adapters.NoteAdapter;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.fragments.VoiceBottomSheetDialog;
import com.voicenotes.app.fragments.VoiceHelpDialog;
import com.voicenotes.app.reminders.ReminderManager;
import com.voicenotes.app.utils.Constants;
import com.voicenotes.app.utils.DateUtils;
import com.voicenotes.app.utils.ExportUtils;
import com.voicenotes.app.utils.ThemeHelper;
import com.voicenotes.app.viewmodel.NoteViewModel;
import com.voicenotes.app.viewmodel.NoteViewModelFactory;
import com.voicenotes.app.voice.TTSManager;
import com.voicenotes.app.voice.VoiceCommandProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Dashboard Activity for VoiceNotes supporting Notes, Active To-Dos, and Completed History.
 */
public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener, VoiceBottomSheetDialog.VoiceDialogListener {

    private static final int REQUEST_PERMISSIONS_CODE = 101;

    private NoteViewModel noteViewModel;
    private NoteAdapter noteAdapter;
    private CategoryAdapter categoryAdapter;
    private TTSManager ttsManager;

    private RecyclerView rvNotes;
    private RecyclerView rvCategories;
    private MaterialButtonToggleGroup toggleViewFilter;
    private LinearLayout layoutEmptyState;
    private TextView tvNotesCount;
    private ExtendedFloatingActionButton fabCreateNote;
    private FloatingActionButton btnHeroMic;
    private MaterialCardView cardSearchTrigger;
    private ImageView btnQuickVoiceSearch;

    private Note lastDeletedNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initViewModel();
        initViews();
        initTTS();
        checkPermissions();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void initViews() {
        rvNotes = findViewById(R.id.rvNotes);
        rvCategories = findViewById(R.id.rvCategories);
        toggleViewFilter = findViewById(R.id.toggleViewFilter);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvNotesCount = findViewById(R.id.tvNotesCount);
        fabCreateNote = findViewById(R.id.fabCreateNote);
        btnHeroMic = findViewById(R.id.btnHeroMic);
        cardSearchTrigger = findViewById(R.id.cardSearchTrigger);
        btnQuickVoiceSearch = findViewById(R.id.btnQuickVoiceSearch);

        // View Filter Toggle Group (All / Notes / Active To-Dos / Completed)
        toggleViewFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || noteViewModel == null) return;
            if (checkedId == R.id.btnFilterNotes) {
                noteViewModel.setSelectedViewFilter(NoteViewModel.FILTER_NOTES);
            } else if (checkedId == R.id.btnFilterTodos) {
                noteViewModel.setSelectedViewFilter(NoteViewModel.FILTER_TODOS);
            } else if (checkedId == R.id.btnFilterCompleted) {
                noteViewModel.setSelectedViewFilter(NoteViewModel.FILTER_COMPLETED);
            } else {
                noteViewModel.setSelectedViewFilter(NoteViewModel.FILTER_ALL);
            }
        });
        toggleViewFilter.check(R.id.btnFilterAll);

        // Notes RecyclerView
        noteAdapter = new NoteAdapter(this);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(noteAdapter);

        // Category Filter Chips
        categoryAdapter = new CategoryAdapter(Constants.CATEGORY_FILTERS, category -> {
            noteViewModel.setSelectedCategory(category);
        });
        rvCategories.setAdapter(categoryAdapter);

        // Listeners
        fabCreateNote.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            startActivity(intent);
        });

        btnHeroMic.setOnClickListener(v -> openVoiceDialog());

        cardSearchTrigger.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        btnQuickVoiceSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            intent.putExtra(Constants.EXTRA_VOICE_TRIGGER, true);
            startActivity(intent);
        });
    }

    private void initViewModel() {
        NoteViewModelFactory factory = new NoteViewModelFactory(getApplication());
        noteViewModel = new ViewModelProvider(this, factory).get(NoteViewModel.class);

        noteViewModel.getFilteredNotes().observe(this, notes -> {
            if (notes != null) {
                noteAdapter.setNotes(notes);
                int count = notes.size();
                tvNotesCount.setText(count == 1 ? "1 item" : count + " items");

                if (count == 0) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvNotes.setVisibility(View.GONE);
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvNotes.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void initTTS() {
        ttsManager = new TTSManager(this, new TTSManager.TTSCallback() {
            @Override
            public void onTTSInitialized(boolean success) {}

            @Override
            public void onSpeechStarted() {}

            @Override
            public void onSpeechCompleted() {}

            @Override
            public void onSpeechError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    private void openVoiceDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS_CODE);
            return;
        }

        VoiceBottomSheetDialog dialog = VoiceBottomSheetDialog.newInstance();
        dialog.setListener(this);
        dialog.show(getSupportFragmentManager(), "VoiceDialog");
    }

    @Override
    public void onCommandExecuted(VoiceCommandProcessor.ParsedCommand command) {
        if (command == null) return;

        switch (command.getType()) {
            case CREATE_NOTE: {
                String noteTitle = command.getTitle();
                String noteContent = command.getContent();

                if (noteTitle.isEmpty()) {
                    noteTitle = "New Note";
                }

                Note newNote = new Note(noteTitle, noteContent, "General", Note.TYPE_NOTE);
                newNote.setCreatedAt(System.currentTimeMillis());
                newNote.setUpdatedAt(System.currentTimeMillis());

                final String createdTitle = noteTitle;
                noteViewModel.insert(newNote, noteId -> runOnUiThread(() -> {
                    String msg = "Note '" + createdTitle + "' created!";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    if (ttsManager != null) {
                        ttsManager.speak("Note titled " + createdTitle + " created");
                    }
                }));
                break;
            }

            case CREATE_TODO: {
                String todoTitle = command.getTitle();
                if (todoTitle.isEmpty()) {
                    todoTitle = "New Task";
                }

                Note newTodo = new Note(todoTitle, command.getContent(), "To-Do", Note.TYPE_TODO);
                newTodo.setTaskStatus(Note.STATUS_PENDING);
                newTodo.setCreatedAt(System.currentTimeMillis());
                newTodo.setUpdatedAt(System.currentTimeMillis());

                final String createdTitle = todoTitle;
                noteViewModel.insert(newTodo, noteId -> runOnUiThread(() -> {
                    String msg = "To-Do '" + createdTitle + "' created!";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    if (ttsManager != null) {
                        ttsManager.speak("Task " + createdTitle + " created");
                    }
                }));
                break;
            }

            case COMPLETE_TASK: {
                String taskTitle = command.getTitle();
                noteViewModel.getNoteByTitleSync(taskTitle, note -> runOnUiThread(() -> {
                    if (note != null) {
                        note.setTaskStatus(Note.STATUS_COMPLETED);
                        note.setCompletedAt(System.currentTimeMillis());
                        note.setReminderEnabled(false);
                        note.setUpdatedAt(System.currentTimeMillis());
                        noteViewModel.update(note);
                        ReminderManager.cancelReminder(MainActivity.this, note.getId());

                        Toast.makeText(MainActivity.this, "Marked '" + note.getTitle() + "' as completed!", Toast.LENGTH_SHORT).show();
                        if (ttsManager != null) {
                            ttsManager.speak("Task " + note.getTitle() + " marked as completed");
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No matching task found for: " + taskTitle, Toast.LENGTH_SHORT).show();
                    }
                }));
                break;
            }

            case SNOOZE_REMINDER: {
                long snoozeMillis = DateUtils.parseReminderTimeString(command.getTimeArgument()) - System.currentTimeMillis();
                if (snoozeMillis <= 0) snoozeMillis = 10 * 60 * 1000L; // default 10m

                final long finalSnooze = snoozeMillis;
                noteViewModel.getLatestNote(latestNote -> runOnUiThread(() -> {
                    if (latestNote != null && latestNote.isReminderEnabled()) {
                        long nextTime = System.currentTimeMillis() + finalSnooze;
                        latestNote.setReminderTime(nextTime);
                        latestNote.setReminderDismissed(false);
                        latestNote.setUpdatedAt(System.currentTimeMillis());
                        noteViewModel.update(latestNote);
                        ReminderManager.scheduleReminder(MainActivity.this, latestNote);

                        int mins = (int) (finalSnooze / 60000);
                        Toast.makeText(MainActivity.this, "Reminder snoozed for " + mins + " minutes.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No active reminder found to snooze.", Toast.LENGTH_SHORT).show();
                    }
                }));
                break;
            }

            case DISMISS_REMINDER: {
                noteViewModel.getLatestNote(latestNote -> runOnUiThread(() -> {
                    if (latestNote != null) {
                        latestNote.setReminderEnabled(false);
                        latestNote.setReminderDismissed(true);
                        latestNote.setUpdatedAt(System.currentTimeMillis());
                        noteViewModel.update(latestNote);
                        ReminderManager.cancelReminder(MainActivity.this, latestNote.getId());

                        Toast.makeText(MainActivity.this, "Reminder dismissed.", Toast.LENGTH_SHORT).show();
                    }
                }));
                break;
            }

            case SEARCH_NOTES:
                Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
                if (!command.getQuery().isEmpty()) {
                    searchIntent.putExtra(Constants.EXTRA_INITIAL_QUERY, command.getQuery());
                }
                startActivity(searchIntent);
                break;

            case READ_LATEST_NOTE:
                noteViewModel.getLatestNote(note -> runOnUiThread(() -> {
                    if (note != null) {
                        String toSpeak = "Title: " + note.getTitle() + ". Content: " + (note.getContent() != null ? note.getContent() : "empty note");
                        ttsManager.speak(toSpeak);
                        Toast.makeText(MainActivity.this, "Reading latest note: " + note.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No notes found to read", Toast.LENGTH_SHORT).show();
                    }
                }));
                break;

            case READ_ALL_NOTES:
                noteViewModel.getAllNotesSync(notes -> runOnUiThread(() -> {
                    if (notes != null && !notes.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("You have ").append(notes.size()).append(" items. ");
                        for (int i = 0; i < Math.min(notes.size(), 5); i++) {
                            sb.append("Item ").append(i + 1).append(": ").append(notes.get(i).getTitle()).append(". ");
                        }
                        ttsManager.speak(sb.toString());
                    } else {
                        Toast.makeText(MainActivity.this, "No items found to read", Toast.LENGTH_SHORT).show();
                    }
                }));
                break;

            case DELETE_NOTE:
                if (!command.getTitle().isEmpty()) {
                    noteViewModel.deleteByTitle(command.getTitle(), success -> runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(MainActivity.this, "Deleted item titled: " + command.getTitle(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "No item found with title: " + command.getTitle(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else {
                    Toast.makeText(MainActivity.this, "Please specify which item title to delete", Toast.LENGTH_SHORT).show();
                }
                break;

            case DELETE_ALL_NOTES:
                showDeleteAllConfirmation();
                break;

            case SET_REMINDER: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSIONS_CODE);
                    }
                }
                long reminderTime = DateUtils.parseReminderTimeString(command.getTimeArgument());
                noteViewModel.getLatestNote(latestNote -> runOnUiThread(() -> {
                    if (latestNote != null) {
                        latestNote.setReminderEnabled(true);
                        latestNote.setReminderTime(reminderTime);
                        latestNote.setUpdatedAt(System.currentTimeMillis());
                        noteViewModel.update(latestNote);
                        ReminderManager.scheduleReminder(MainActivity.this, latestNote);
                        Toast.makeText(MainActivity.this, "Reminder scheduled for: " + DateUtils.formatDateTime(reminderTime), Toast.LENGTH_LONG).show();
                    } else {
                        Note newNote = new Note("Voice Reminder", "Reminder created via voice command", "General");
                        newNote.setReminderEnabled(true);
                        newNote.setReminderTime(reminderTime);
                        newNote.setCreatedAt(System.currentTimeMillis());
                        newNote.setUpdatedAt(System.currentTimeMillis());
                        noteViewModel.insert(newNote, noteId -> {
                            newNote.setId(noteId);
                            ReminderManager.scheduleReminder(MainActivity.this, newNote);
                        });
                        Toast.makeText(MainActivity.this, "New reminder note scheduled for: " + DateUtils.formatDateTime(reminderTime), Toast.LENGTH_LONG).show();
                    }
                }));
                break;
            }

            case CHANGE_THEME_DARK:
                ThemeHelper.setThemeMode(this, Constants.THEME_DARK);
                Toast.makeText(this, "Dark mode enabled", Toast.LENGTH_SHORT).show();
                break;

            case CHANGE_THEME_LIGHT:
                ThemeHelper.setThemeMode(this, Constants.THEME_LIGHT);
                Toast.makeText(this, "Light mode enabled", Toast.LENGTH_SHORT).show();
                break;

            case NAVIGATE_SETTINGS:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;

            case NAVIGATE_HOME:
                Toast.makeText(this, "You are on the Home screen", Toast.LENGTH_SHORT).show();
                break;

            case DICTATE_CONTENT:
            default:
                onDictatedContent(command.getRawText());
                break;
        }
    }

    @Override
    public void onDictatedContent(String text) {
        if (text != null && !text.trim().isEmpty()) {
            String content = text.trim();
            String title = content.length() > 25 ? content.substring(0, 25) + "..." : content;
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

            Note newNote = new Note(title, content, "General", Note.TYPE_NOTE);
            newNote.setCreatedAt(System.currentTimeMillis());
            newNote.setUpdatedAt(System.currentTimeMillis());

            final String createdTitle = title;
            noteViewModel.insert(newNote, noteId -> runOnUiThread(() -> {
                String msg = "Note '" + createdTitle + "' created!";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                if (ttsManager != null) {
                    ttsManager.speak("Note created");
                }
            }));
        }
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
        intent.putExtra(Constants.EXTRA_NOTE_ID, note.getId());
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note) {
        showNoteOptionsDialog(note);
    }

    @Override
    public void onTaskStatusChanged(Note note, boolean isCompleted) {
        if (note == null) return;
        note.setTaskStatus(isCompleted ? Note.STATUS_COMPLETED : Note.STATUS_PENDING);
        note.setCompletedAt(isCompleted ? System.currentTimeMillis() : 0);

        if (isCompleted) {
            note.setReminderEnabled(false);
            ReminderManager.cancelReminder(this, note.getId());
        }

        note.setUpdatedAt(System.currentTimeMillis());
        noteViewModel.update(note);

        String msg = isCompleted ? "Task '" + note.getTitle() + "' completed!" : "Task marked pending";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onQuickTtsClick(Note note) {
        if (ttsManager != null) {
            String textToSpeak = (note.getTitle() != null ? note.getTitle() + ". " : "") +
                    (note.getContent() != null ? note.getContent() : "");
            ttsManager.speak(textToSpeak);
            Toast.makeText(this, "Reading item...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoteOptionsDialog(Note note) {
        String pinText = note.isPinned() ? "Unpin Item" : "Pin Item to Top";
        String[] options = {pinText, "Edit Item", "Share Item", "Delete Item"};

        new AlertDialog.Builder(this)
                .setTitle(note.getTitle() != null ? note.getTitle() : "Item Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            note.setPinned(!note.isPinned());
                            note.setUpdatedAt(System.currentTimeMillis());
                            noteViewModel.update(note);
                            break;
                        case 1:
                            onNoteClick(note);
                            break;
                        case 2:
                            ExportUtils.exportSingleNote(this, note);
                            break;
                        case 3:
                            confirmDeleteNote(note);
                            break;
                    }
                })
                .show();
    }

    private void confirmDeleteNote(Note note) {
        lastDeletedNote = note;
        if (note.isReminderEnabled()) {
            ReminderManager.cancelReminder(this, note.getId());
        }
        noteViewModel.delete(note);

        Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.msg_note_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.btn_undo, v -> {
                    if (lastDeletedNote != null) {
                        noteViewModel.insert(lastDeletedNote, noteId -> {
                            if (lastDeletedNote.isReminderEnabled() && lastDeletedNote.getReminderTime() > System.currentTimeMillis()) {
                                lastDeletedNote.setId(noteId);
                                ReminderManager.scheduleReminder(MainActivity.this, lastDeletedNote);
                            }
                        });
                    }
                })
                .show();
    }

    private void showDeleteAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.msg_delete_all_confirm_title)
                .setMessage(R.string.msg_delete_all_confirm_body)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    noteViewModel.deleteAllNotes();
                    Toast.makeText(MainActivity.this, R.string.msg_all_notes_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_voice_help) {
            VoiceHelpDialog.newInstance().show(getSupportFragmentManager(), "VoiceHelpDialog");
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
