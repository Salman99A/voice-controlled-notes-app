package com.voicenotes.app.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.voicenotes.app.activities.NoteEditActivity;
import com.voicenotes.app.database.AppDatabase;
import com.voicenotes.app.database.Note;
import com.voicenotes.app.utils.Constants;
import com.voicenotes.app.utils.DateUtils;

import java.util.List;

/**
 * Manages exact-to-the-second scheduling, cancelling, and rescheduling of note reminders using AlarmManager.
 */
public final class ReminderManager {

    private static final String TAG = "VoiceNotes";

    private ReminderManager() {}

    public static void scheduleReminder(Context context, Note note) {
        if (note == null) {
            Log.w(TAG, "scheduleReminder skipped: note is null");
            return;
        }

        if (!note.isReminderEnabled()) {
            Log.w(TAG, "scheduleReminder skipped: reminder is not enabled for note '" + note.getTitle() + "'");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long triggerAtMillis = note.getReminderTime();

        if (triggerAtMillis <= currentTime) {
            Log.w(TAG, "scheduleReminder skipped: reminder time (" + triggerAtMillis + ") is in the past compared to current time (" + currentTime + ")");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service is null");
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(Constants.EXTRA_NOTE_ID, note.getId());
        intent.putExtra(Constants.EXTRA_VOICE_TITLE, note.getTitle());
        intent.putExtra(Constants.EXTRA_VOICE_CONTENT, note.getContent());

        int requestCode = (int) (note.getId() > 0 ? note.getId() : (note.getCreatedAt() & 0xfffffff));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long delaySeconds = (triggerAtMillis - currentTime) / 1000;
        Log.d(TAG, "Scheduling exact alarm for note '" + note.getTitle() + "' (ID: " + note.getId() + ") at " + triggerAtMillis + " (" + DateUtils.formatDateTime(triggerAtMillis) + ") - in " + delaySeconds + "s");

        // Intent for AlarmClockInfo clock icon click in status bar
        Intent showIntent = new Intent(context, NoteEditActivity.class);
        showIntent.putExtra(Constants.EXTRA_NOTE_ID, note.getId());
        PendingIntent showOperation = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerAtMillis, showOperation);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                    Log.d(TAG, "Exact alarm scheduled with setAlarmClock");
                } else {
                    Log.w(TAG, "canScheduleExactAlarms is false; scheduling via setAlarmClock fallback");
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                Log.d(TAG, "Exact alarm scheduled with setAlarmClock");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException using setAlarmClock, falling back to setExactAndAllowWhileIdle", se);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } catch (Exception e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception scheduling alarm", e);
        }
    }

    public static void cancelReminder(Context context, long noteId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = (int) (noteId > 0 ? noteId : System.currentTimeMillis() & 0xfffffff);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Cancelled reminder alarm for note ID: " + noteId);
    }

    public static void rescheduleAllActiveReminders(Context context) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<Note> activeReminders = db.noteDao().getActiveRemindersSync(System.currentTimeMillis());
            Log.d(TAG, "rescheduleAllActiveReminders: Rescheduling " + activeReminders.size() + " active reminders");
            for (Note note : activeReminders) {
                scheduleReminder(context, note);
            }
        });
    }
}
