package com.voicenotes.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.voicenotes.app.database.Note;
import com.voicenotes.app.repository.NoteRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel managing UI state and data operations for Notes and To-Dos.
 */
public class NoteViewModel extends AndroidViewModel {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_NOTES = "NOTES";
    public static final String FILTER_TODOS = "TODOS";
    public static final String FILTER_COMPLETED = "COMPLETED";

    private final NoteRepository repository;
    private final LiveData<List<Note>> allNotes;
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");
    private final MutableLiveData<String> selectedViewFilter = new MutableLiveData<>(FILTER_ALL);

    private final LiveData<List<Note>> filteredNotes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        allNotes = repository.getAllNotes();

        // Multi-level transformation combining View Filter (Notes / Active To-Dos / Completed) and Category
        filteredNotes = Transformations.switchMap(selectedViewFilter, filter -> {
            LiveData<List<Note>> source;
            if (FILTER_NOTES.equalsIgnoreCase(filter)) {
                source = repository.getRegularNotes();
            } else if (FILTER_TODOS.equalsIgnoreCase(filter)) {
                source = repository.getActivePendingTodos();
            } else if (FILTER_COMPLETED.equalsIgnoreCase(filter)) {
                source = repository.getCompletedTasks();
            } else {
                source = repository.getAllNotes();
            }

            return Transformations.map(source, notes -> {
                String cat = selectedCategory.getValue();
                if (notes == null || cat == null || cat.equalsIgnoreCase("All")) {
                    return notes;
                }
                List<Note> result = new ArrayList<>();
                for (Note n : notes) {
                    if (cat.equalsIgnoreCase(n.getCategory())) {
                        result.add(n);
                    }
                }
                return result;
            });
        });
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    public LiveData<List<Note>> getFilteredNotes() {
        return filteredNotes;
    }

    public void setSelectedCategory(String category) {
        selectedCategory.setValue(category);
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedViewFilter(String filter) {
        selectedViewFilter.setValue(filter);
    }

    public LiveData<String> getSelectedViewFilter() {
        return selectedViewFilter;
    }

    public LiveData<List<Note>> searchNotes(String query) {
        return repository.searchNotes(query);
    }

    public LiveData<Note> getNoteById(long id) {
        return repository.getNoteById(id);
    }

    public void insert(Note note, NoteRepository.OnNoteInsertedListener listener) {
        repository.insert(note, listener);
    }

    public void update(Note note) {
        repository.update(note);
    }

    public void markTaskCompleted(long noteId, NoteRepository.OnTaskStatusUpdatedListener listener) {
        repository.markTaskCompleted(noteId, listener);
    }

    public void delete(Note note) {
        repository.delete(note);
    }

    public void deleteById(long noteId) {
        repository.deleteById(noteId);
    }

    public void deleteByTitle(String title, NoteRepository.OnNoteDeletedListener listener) {
        repository.deleteByTitle(title, listener);
    }

    public void deleteAllNotes() {
        repository.deleteAllNotes();
    }

    public void getLatestNote(NoteRepository.OnSingleNoteLoadedListener listener) {
        repository.getLatestNote(listener);
    }

    public void getNoteByTitleSync(String title, NoteRepository.OnSingleNoteLoadedListener listener) {
        repository.getNoteByTitleSync(title, listener);
    }

    public void getAllNotesSync(NoteRepository.OnNotesLoadedListener listener) {
        repository.getAllNotesSync(listener);
    }

    public void searchNotesSync(String query, NoteRepository.OnNotesLoadedListener listener) {
        repository.searchNotesSync(query, listener);
    }

    public NoteRepository getRepository() {
        return repository;
    }
}
