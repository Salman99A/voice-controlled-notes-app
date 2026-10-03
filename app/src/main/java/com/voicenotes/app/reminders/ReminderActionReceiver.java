package com.voicenotes.app.reminders;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.voicenotes.app.database.AppDatabase;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.utils.Constants;

/**
 * Handles interactive Notification Action Buttons: DONE, SNOOZE, and DISMISS.
 */
public class ReminderActionReceiver extends BroadcastReceiver {

    private static final String TAG = "VoiceNotes";

    public static final String ACTION_DONE = "com.voicenotes.app.ACTION_DONE";
    public static final String ACTION_SNOOZE = "com.voicenotes.app.ACTION_SNOOZE";
    public static final String ACTION_DISMISS = "com.voicenotes.app.ACTION_DISMISS";

    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_SNOOZE_MILLIS = "extra_snooze_millis";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        final PendingResult pendingResult = goAsync();
        String action = intent.getAction();
        long noteId = intent.getLongExtra(Constants.EXTRA_NOTE_ID, -1);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) noteId);
        long snoozeMillis = intent.getLongExtra(EXTRA_SNOOZE_MILLIS, 10 * 60 * 1000L); // Default 10 minutes

        Log.d(TAG, "ReminderActionReceiver: Received action " + action + " for noteId=" + noteId);

        // Cancel the displayed notification immediately
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && notificationId != -1) {
            manager.cancel(notificationId);
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(context);
                Note note = (noteId != -1) ? db.noteDao().getNoteByIdSync(noteId) : null;

                long now = System.currentTimeMillis();

                if (ACTION_DONE.equals(action)) {
                    Log.d(TAG, "Action DONE: Marking task as completed and cancelling reminders");
                    if (noteId != -1) {
                        db.noteDao().markTaskCompletedSync(noteId, now);
                        ReminderManager.cancelReminder(context, noteId);
                    }
                    showToast(context, "Task marked as completed!");

                } else if (ACTION_SNOOZE.equals(action)) {
                    long actualSnooze = (note != null && note.getReminderRepeatInterval() > 0) ?
                            note.getReminderRepeatInterval() : snoozeMillis;
                    long nextTrigger = now + actualSnooze;

                    Log.d(TAG, "Action SNOOZE: Snoozing reminder for " + (actualSnooze / 60000) + " minutes");
                    if (noteId != -1) {
                        db.noteDao().snoozeTaskReminderSync(noteId, nextTrigger, now);
                        if (note != null) {
                            note.setReminderTime(nextTrigger);
                            note.setReminderDismissed(false);
                            ReminderManager.scheduleReminder(context, note);
                        }
                    }
                    int mins = (int) (actualSnooze / 60000);
                    showToast(context, "Reminder snoozed for " + (mins > 0 ? mins : 10) + " minutes.");

                } else if (ACTION_DISMISS.equals(action)) {
                    Log.d(TAG, "Action DISMISS: Dismissing reminder (task remains pending)");
                    if (noteId != -1) {
                        db.noteDao().dismissTaskReminderSync(noteId, now);
                        ReminderManager.cancelReminder(context, noteId);
                    }
                    showToast(context, "Reminder dismissed.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling ReminderActionReceiver action", e);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }
}
