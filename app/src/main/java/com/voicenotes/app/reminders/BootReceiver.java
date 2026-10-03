package com.voicenotes.app.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.voicenotes.app.database.AppDatabase;

/**
 * Re-registers all active reminders after the device finishes booting.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "VoiceNotes";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()))) {
            Log.d(TAG, "BootReceiver: Boot completed, rescheduling all active reminders");
            final PendingResult pendingResult = goAsync();
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    ReminderManager.rescheduleAllActiveReminders(context);
                } catch (Exception e) {
                    Log.e(TAG, "Error in BootReceiver rescheduling active reminders", e);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
