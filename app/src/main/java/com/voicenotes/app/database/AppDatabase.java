package com.voicenotes.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room Database singleton class for VoiceNotes application.
 */
@Database(entities = {Note.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NoteDao noteDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notes ADD COLUMN noteType TEXT DEFAULT 'NOTE'");
            database.execSQL("ALTER TABLE notes ADD COLUMN taskStatus TEXT DEFAULT 'PENDING'");
            database.execSQL("ALTER TABLE notes ADD COLUMN completedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE notes ADD COLUMN reminderRepeatInterval INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE notes ADD COLUMN reminderDismissed INTEGER NOT NULL DEFAULT 0");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_noteType ON notes(noteType)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_taskStatus ON notes(taskStatus)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "voicenotes_database"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(sRoomDatabaseCallback)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Callback sRoomDatabaseCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                NoteDao dao = INSTANCE.noteDao();

                // Prepopulate welcome note
                Note welcomeNote = new Note();
                welcomeNote.setTitle("Welcome to VoiceNotes! 🎙️");
                welcomeNote.setContent("Tap the big microphone button to speak and control your notes with voice commands.\n\nTry saying:\n• 'Create a note'\n• 'Create a to-do to submit assignment'\n• 'Mark Java assignment as done'\n• 'Turn on dark mode'");
                welcomeNote.setCategory("General");
                welcomeNote.setNoteType(Note.TYPE_NOTE);
                welcomeNote.setPinned(true);
                welcomeNote.setCreatedAt(System.currentTimeMillis());
                welcomeNote.setUpdatedAt(System.currentTimeMillis());
                dao.insertNote(welcomeNote);

                // Sample project note
                Note projectNote = new Note();
                projectNote.setTitle("Mobile App Project Architecture");
                projectNote.setContent("Architecture layers:\n1. UI: Material Design & Voice Bottom Sheet\n2. ViewModel & LiveData\n3. Repository & Room Persistence\n4. Android SpeechRecognizer & TTS");
                projectNote.setCategory("Study");
                projectNote.setNoteType(Note.TYPE_NOTE);
                projectNote.setPinned(false);
                projectNote.setCreatedAt(System.currentTimeMillis() - 3600000);
                projectNote.setUpdatedAt(System.currentTimeMillis() - 3600000);
                dao.insertNote(projectNote);

                // Sample To-Do Task
                Note sampleTodo = new Note();
                sampleTodo.setTitle("Submit Java Assignment");
                sampleTodo.setContent("Complete inheritance and Room DB implementation for SIH prototype.");
                sampleTodo.setCategory("To-Do");
                sampleTodo.setNoteType(Note.TYPE_TODO);
                sampleTodo.setTaskStatus(Note.STATUS_PENDING);
                sampleTodo.setCreatedAt(System.currentTimeMillis() - 1800000);
                sampleTodo.setUpdatedAt(System.currentTimeMillis() - 1800000);
                dao.insertNote(sampleTodo);
            });
        }
    };
}
