package com.voicenotes.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.voicenotes.app.R;
import com.voicenotes.app.activities.NoteEditActivity;
import com.voicenotes.app.reminders.ReminderActionReceiver;

/**
 * Handles creation of sound-configured Notification Channels and rendering of Note/To-Do Reminder notifications with action buttons.
 */
public final class NotificationHelper {

    private static final String TAG = "VoiceNotes";

    private NotificationHelper() {}

    public static String getChannelForSound(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String soundChoice = prefs.getString(Constants.KEY_REMINDER_SOUND, "Default");
        return "channel_voicenotes_sound_" + soundChoice.toLowerCase().replaceAll("\\s+", "_");
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            String soundChoice = prefs.getString(Constants.KEY_REMINDER_SOUND, "Default");
            boolean vibrationEnabled = prefs.getBoolean(Constants.KEY_VIBRATION_ENABLED, true);

            String channelId = getChannelForSound(context);
            NotificationChannel channel = manager.getNotificationChannel(channelId);

            if (channel == null) {
                int importance = "Silent".equalsIgnoreCase(soundChoice) ?
                        NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel newChannel = new NotificationChannel(
                        channelId,
                        "VoiceNotes Reminders (" + soundChoice + ")",
                        importance
                );
                newChannel.setDescription("Scheduled voice note and task reminder alerts");
                newChannel.enableLights(true);
                newChannel.setLightColor(Color.parseColor("#6C3BFF"));
                newChannel.enableVibration(vibrationEnabled);
                newChannel.setShowBadge(true);
                newChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                Uri soundUri = getSoundUri(soundChoice);
                if (soundUri != null) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build();
                    newChannel.setSound(soundUri, audioAttributes);
                } else if ("Silent".equalsIgnoreCase(soundChoice)) {
                    newChannel.setSound(null, null);
                }

                manager.createNotificationChannel(newChannel);
                Log.d(TAG, "Created NotificationChannel: " + channelId + " with sound: " + soundChoice);
            }
        }
    }

    private static Uri getSoundUri(String soundChoice) {
        if ("Silent".equalsIgnoreCase(soundChoice)) {
            return null;
        } else if ("Classic".equalsIgnoreCase(soundChoice)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        } else if ("Soft".equalsIgnoreCase(soundChoice) || "Attention".equalsIgnoreCase(soundChoice)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else if ("Urgent".equalsIgnoreCase(soundChoice)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }

    public static void showReminderNotification(Context context, long noteId, String title, String content) {
        Log.d(TAG, "showReminderNotification called for noteId=" + noteId + ", title=" + title);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        createNotificationChannel(context);
        String channelId = getChannelForSound(context);

        int notificationId = (noteId <= 0) ? (int) (System.currentTimeMillis() & 0xfffffff) : (int) noteId;

        // Content Intent (Opens NoteEditActivity when user taps notification body)
        Intent contentIntent = new Intent(context, NoteEditActivity.class);
        contentIntent.putExtra(Constants.EXTRA_NOTE_ID, noteId);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingContentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action 1: DONE
        Intent doneIntent = new Intent(context, ReminderActionReceiver.class);
        doneIntent.setAction(ReminderActionReceiver.ACTION_DONE);
        doneIntent.putExtra(Constants.EXTRA_NOTE_ID, noteId);
        doneIntent.putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        PendingIntent pendingDoneIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1000,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action 2: SNOOZE (10 minutes)
        Intent snoozeIntent = new Intent(context, ReminderActionReceiver.class);
        snoozeIntent.setAction(ReminderActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(Constants.EXTRA_NOTE_ID, noteId);
        snoozeIntent.putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        snoozeIntent.putExtra(ReminderActionReceiver.EXTRA_SNOOZE_MILLIS, 10 * 60 * 1000L);

        PendingIntent pendingSnoozeIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action 3: DISMISS
        Intent dismissIntent = new Intent(context, ReminderActionReceiver.class);
        dismissIntent.setAction(ReminderActionReceiver.ACTION_DISMISS);
        dismissIntent.putExtra(Constants.EXTRA_NOTE_ID, noteId);
        dismissIntent.putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        PendingIntent pendingDismissIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 3000,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String displayTitle = (title != null && !title.trim().isEmpty()) ? title : "Note Reminder";
        String displayContent = (content != null && !content.trim().isEmpty()) ? content : "Scheduled reminder for your task.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("⏰ " + displayTitle)
                .setContentText(displayContent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(displayContent + "\n\nThis To-Do is still pending."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#6C3BFF"))
                .setContentIntent(pendingContentIntent)
                .addAction(R.drawable.ic_save, "✓ DONE", pendingDoneIntent)
                .addAction(R.drawable.ic_alarm, "💤 SNOOZE 10M", pendingSnoozeIntent)
                .addAction(R.drawable.ic_clear, "✕ DISMISS", pendingDismissIntent);

        manager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification posted with ID=" + notificationId + " on channel=" + channelId);
    }
}
