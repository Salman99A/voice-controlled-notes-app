package com.voicenotes.app.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.voicenotes.app.database.AppDatabase;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.database.NoteDao;

import java.util.List;

/**
 * Repository pattern implementation for managing note and task data operations.
 */
public class NoteRepository {

    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;

    public interface OnNoteInsertedListener {
        void onNoteInserted(long noteId);
    }

    public interface OnSingleNoteLoadedListener {
        void onNoteLoaded(Note note);
    }

    public interface OnNotesLoadedListener {
        void onNotesLoaded(List<Note> notes);
    }

    public interface OnNoteDeletedListener {
        void onNoteDeleted(boolean success);
    }

    public interface OnTaskStatusUpdatedListener {
        void onTaskStatusUpdated(boolean success);
    }

    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        noteDao = db.noteDao();
        allNotes = noteDao.getAllNotes();
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    public LiveData<List<Note>> getRegularNotes() {
        return noteDao.getRegularNotes();
    }

    public LiveData<List<Note>> getActivePendingTodos() {
        return noteDao.getActivePendingTodos();
    }

    public LiveData<List<Note>> getCompletedTasks() {
        return noteDao.getCompletedTasks();
    }

    public LiveData<List<Note>> getNotesByCategory(String category) {
        return noteDao.getNotesByCategory(category);
    }

    public LiveData<List<Note>> searchNotes(String query) {
        return noteDao.searchNotes(query);
    }

    public LiveData<Note> getNoteById(long id) {
        return noteDao.getNoteById(id);
    }

    public void insert(Note note, OnNoteInsertedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = noteDao.insertNote(note);
            note.setId(id);
            if (listener != null) {
                listener.onNoteInserted(id);
            }
        });
    }

    public void update(Note note) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.updateNote(note));
    }

    public void markTaskCompleted(long noteId, OnTaskStatusUpdatedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int rows = noteDao.markTaskCompletedSync(noteId, System.currentTimeMillis());
            if (listener != null) {
                listener.onTaskStatusUpdated(rows > 0);
            }
        });
    }

    public void delete(Note note) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.deleteNote(note));
    }

    public void deleteById(long noteId) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.deleteNoteById(noteId));
    }

    public void deleteByTitle(String title, OnNoteDeletedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int rows = noteDao.deleteNoteByTitle(title);
            if (listener != null) {
                listener.onNoteDeleted(rows > 0);
            }
        });
    }

    public void deleteAllNotes() {
        AppDatabase.databaseWriteExecutor.execute(noteDao::deleteAllNotes);
    }

    public void getLatestNote(OnSingleNoteLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Note note = noteDao.getLatestNoteSync();
            if (listener != null) {
                listener.onNoteLoaded(note);
            }
        });
    }

    public void getNoteByTitleSync(String title, OnSingleNoteLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Note note = noteDao.getNoteByTitleSync(title);
            if (listener != null) {
                listener.onNoteLoaded(note);
            }
        });
    }

    public void getAllNotesSync(OnNotesLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Note> notes = noteDao.getAllNotesSync();
            if (listener != null) {
                listener.onNotesLoaded(notes);
            }
        });
    }

    public void searchNotesSync(String query, OnNotesLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Note> notes = noteDao.searchNotesSync(query);
            if (listener != null) {
                listener.onNotesLoaded(notes);
            }
        });
    }
}
