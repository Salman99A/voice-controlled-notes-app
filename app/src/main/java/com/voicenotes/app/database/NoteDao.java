package com.voicenotes.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for Note and To-Do task operations in Room.
 */
@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNoteById(long noteId);

    @Query("DELETE FROM notes WHERE LOWER(title) = LOWER(:title)")
    int deleteNoteByTitle(String title);

    @Query("DELETE FROM notes")
    void deleteAllNotes();

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    List<Note> getAllNotesSync();

    @Query("SELECT * FROM notes WHERE noteType = 'NOTE' ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getRegularNotes();

    @Query("SELECT * FROM notes WHERE noteType = 'TODO' AND taskStatus = 'PENDING' ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getActivePendingTodos();

    @Query("SELECT * FROM notes WHERE taskStatus = 'COMPLETED' ORDER BY completedAt DESC, updatedAt DESC")
    LiveData<List<Note>> getCompletedTasks();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    LiveData<Note> getNoteById(long id);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getNoteByIdSync(long id);

    @Query("SELECT * FROM notes WHERE LOWER(title) LIKE '%' || LOWER(:title) || '%' LIMIT 1")
    Note getNoteByTitleSync(String title);

    @Query("SELECT * FROM notes WHERE category = :category ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getNotesByCategory(String category);

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> searchNotes(String query);

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    List<Note> searchNotesSync(String query);

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT 1")
    Note getLatestNoteSync();

    @Query("SELECT * FROM notes WHERE isReminderEnabled = 1 AND taskStatus != 'COMPLETED' AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    List<Note> getActiveRemindersSync(long currentTime);

    // Sync task state updates for notification action buttons & voice commands
    @Query("UPDATE notes SET taskStatus = 'COMPLETED', completedAt = :completedAt, isReminderEnabled = 0, updatedAt = :completedAt WHERE id = :noteId")
    int markTaskCompletedSync(long noteId, long completedAt);

    @Query("UPDATE notes SET reminderTime = :nextReminderTime, reminderDismissed = 0, updatedAt = :currentTime WHERE id = :noteId")
    int snoozeTaskReminderSync(long noteId, long nextReminderTime, long currentTime);

    @Query("UPDATE notes SET isReminderEnabled = 0, reminderDismissed = 1, updatedAt = :currentTime WHERE id = :noteId")
    int dismissTaskReminderSync(long noteId, long currentTime);
}
