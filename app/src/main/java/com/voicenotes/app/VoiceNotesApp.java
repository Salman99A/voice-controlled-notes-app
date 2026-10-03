package com.voicenotes.app;

import android.app.Application;

import com.voicenotes.app.utils.NotificationHelper;
import com.voicenotes.app.utils.ThemeHelper;

/**
 * Main Application class for VoiceNotes.
 */
public class VoiceNotesApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme on application startup
        ThemeHelper.applyTheme(this);

        // Register Android Notification Channels
        NotificationHelper.createNotificationChannel(this);
    }
}
