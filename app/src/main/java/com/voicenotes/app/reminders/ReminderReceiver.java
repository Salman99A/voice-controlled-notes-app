package com.voicenotes.app.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.voicenotes.app.database.AppDatabase;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.utils.Constants;
import com.voicenotes.app.utils.DateUtils;
import com.voicenotes.app.utils.NotificationHelper;

/**
 * BroadcastReceiver triggered by AlarmManager to post Note Reminder notifications.
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "VoiceNotes";

    @Override
    public void onReceive(Context context, Intent intent) {
        long receiverTime = System.currentTimeMillis();
        Log.d(TAG, "ReminderReceiver: onReceive executed at " + receiverTime + " (" + DateUtils.formatDateTime(receiverTime) + ")");
        if (intent == null) return;

        final PendingResult pendingResult = goAsync();

        long noteId = intent.getLongExtra(Constants.EXTRA_NOTE_ID, -1);
        String title = intent.getStringExtra(Constants.EXTRA_VOICE_TITLE);
        String content = intent.getStringExtra(Constants.EXTRA_VOICE_CONTENT);

        Log.d(TAG, "ReminderReceiver: noteId=" + noteId + ", title=" + title);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                Note note = (noteId != -1) ? db.noteDao().getNoteByIdSync(noteId) : null;

                if (note != null) {
                    long delta = receiverTime - note.getReminderTime();
                    Log.d(TAG, "ReminderReceiver: Displaying notification for note '" + note.getTitle() + "'. Scheduled: " + note.getReminderTime() + ", Actual: " + receiverTime + " (Delta: " + delta + "ms)");
                    NotificationHelper.showReminderNotification(context, note.getId(), note.getTitle(), note.getContent());
                } else {
                    Log.d(TAG, "ReminderReceiver: Note not found in DB or newly created, displaying fallback notification");
                    String displayTitle = (title != null && !title.trim().isEmpty()) ? title : "Note Reminder";
                    String displayContent = (content != null && !content.trim().isEmpty()) ? content : "Scheduled reminder for your note.";
                    long displayId = (noteId > 0) ? noteId : System.currentTimeMillis();
                    NotificationHelper.showReminderNotification(context, displayId, displayTitle, displayContent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in ReminderReceiver onReceive background execution", e);
                String displayTitle = (title != null && !title.trim().isEmpty()) ? title : "Note Reminder";
                String displayContent = (content != null && !content.trim().isEmpty()) ? content : "Scheduled reminder for your note.";
                NotificationHelper.showReminderNotification(context, System.currentTimeMillis(), displayTitle, displayContent);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
