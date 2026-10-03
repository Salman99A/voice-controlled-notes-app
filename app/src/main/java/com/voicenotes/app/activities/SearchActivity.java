package com.voicenotes.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.voicenotes.app.R;
import com.voicenotes.app.adapters.CategoryAdapter;
import com.voicenotes.app.adapters.NoteAdapter;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.fragments.VoiceBottomSheetDialog;
import com.voicenotes.app.reminders.ReminderManager;
import com.voicenotes.app.utils.Constants;
import com.voicenotes.app.viewmodel.NoteViewModel;
import com.voicenotes.app.viewmodel.NoteViewModelFactory;
import com.voicenotes.app.voice.TTSManager;
import com.voicenotes.app.voice.VoiceCommandProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Search Activity supporting typed and voice-driven search across note title, content, and category.
 */
public class SearchActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener, VoiceBottomSheetDialog.VoiceDialogListener {

    private NoteViewModel noteViewModel;
    private NoteAdapter searchResultsAdapter;
    private CategoryAdapter categoryAdapter;
    private TTSManager ttsManager;

    private EditText etSearchQuery;
    private ImageView btnClearSearch;
    private ImageView btnVoiceSearch;
    private ImageView btnBack;
    private RecyclerView rvSearchResults;
    private RecyclerView rvSearchCategories;
    private TextView tvResultsCount;
    private LinearLayout layoutSearchEmpty;
    private TextView tvEmptyMessage;

    private String selectedCategory = "All";
    private List<Note> currentRawResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        initViewModel();
        initTTS();
        handleIntentData();
    }

    private void initViews() {
        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        btnBack = findViewById(R.id.btnBack);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvSearchCategories = findViewById(R.id.rvSearchCategories);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        layoutSearchEmpty = findViewById(R.id.layoutSearchEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        btnBack.setOnClickListener(v -> finish());

        // Search Results RecyclerView
        searchResultsAdapter = new NoteAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchResultsAdapter);

        // Category Filter Chips
        categoryAdapter = new CategoryAdapter(Constants.CATEGORY_FILTERS, category -> {
            selectedCategory = category;
            filterResultsByCategory();
        });
        rvSearchCategories.setAdapter(categoryAdapter);

        // Clear Search Query Button
        btnClearSearch.setOnClickListener(v -> {
            etSearchQuery.setText("");
            performSearch("");
        });

        // Voice Search Trigger
        btnVoiceSearch.setOnClickListener(v -> {
            VoiceBottomSheetDialog dialog = VoiceBottomSheetDialog.newInstance();
            dialog.setListener(this);
            dialog.show(getSupportFragmentManager(), "VoiceSearchDialog");
        });

        // Search Query TextWatcher
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                performSearch(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initViewModel() {
        NoteViewModelFactory factory = new NoteViewModelFactory(getApplication());
        noteViewModel = new ViewModelProvider(this, factory).get(NoteViewModel.class);
    }

    private void initTTS() {
        ttsManager = new TTSManager(this, null);
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            String initialQuery = intent.getStringExtra(Constants.EXTRA_INITIAL_QUERY);
            boolean voiceTrigger = intent.getBooleanExtra(Constants.EXTRA_VOICE_TRIGGER, false);

            if (initialQuery != null && !initialQuery.isEmpty()) {
                etSearchQuery.setText(initialQuery);
                etSearchQuery.setSelection(initialQuery.length());
                performSearch(initialQuery);
            } else if (voiceTrigger) {
                btnVoiceSearch.performClick();
            }
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            noteViewModel.getAllNotesSync(notes -> runOnUiThread(() -> {
                currentRawResults = (notes != null) ? notes : new ArrayList<>();
                filterResultsByCategory();
            }));
        } else {
            noteViewModel.searchNotesSync(query, notes -> runOnUiThread(() -> {
                currentRawResults = (notes != null) ? notes : new ArrayList<>();
                filterResultsByCategory();
            }));
        }
    }

    private void filterResultsByCategory() {
        List<Note> filtered = new ArrayList<>();
        if (selectedCategory.equalsIgnoreCase("All")) {
            filtered.addAll(currentRawResults);
        } else {
            for (Note n : currentRawResults) {
                if (selectedCategory.equalsIgnoreCase(n.getCategory())) {
                    filtered.add(n);
                }
            }
        }

        searchResultsAdapter.setNotes(filtered);
        int count = filtered.size();
        tvResultsCount.setVisibility(View.VISIBLE);
        tvResultsCount.setText(count == 1 ? "Found 1 note" : "Found " + count + " notes");

        if (count == 0) {
            layoutSearchEmpty.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
            tvEmptyMessage.setText(R.string.msg_no_notes_found);
        } else {
            layoutSearchEmpty.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCommandExecuted(VoiceCommandProcessor.ParsedCommand command) {
        if (command == null) return;

        if (command.getType() == VoiceCommandProcessor.CommandType.SEARCH_NOTES && !command.getQuery().isEmpty()) {
            etSearchQuery.setText(command.getQuery());
            etSearchQuery.setSelection(command.getQuery().length());
        } else if (!command.getRawText().isEmpty()) {
            etSearchQuery.setText(command.getRawText());
            etSearchQuery.setSelection(command.getRawText().length());
        }
    }

    @Override
    public void onDictatedContent(String text) {
        if (text != null && !text.trim().isEmpty()) {
            etSearchQuery.setText(text.trim());
            etSearchQuery.setSelection(text.trim().length());
        }
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(SearchActivity.this, NoteEditActivity.class);
        intent.putExtra(Constants.EXTRA_NOTE_ID, note.getId());
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note) {}

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
    }

    @Override
    public void onQuickTtsClick(Note note) {
        if (ttsManager != null) {
            String speech = (note.getTitle() != null ? note.getTitle() + ". " : "") +
                    (note.getContent() != null ? note.getContent() : "");
            ttsManager.speak(speech);
            Toast.makeText(this, "Reading note...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
