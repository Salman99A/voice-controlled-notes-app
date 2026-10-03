package com.voicenotes.app.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Global Constants for VoiceNotes.
 */
public final class Constants {

    private Constants() {}

    public static final String PREFS_NAME = "VoiceNotesPrefs";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_VOICE_COMMANDS_ENABLED = "voice_commands_enabled";
    public static final String KEY_TTS_SPEED = "tts_speed";
    public static final String KEY_TTS_PITCH = "tts_pitch";
    public static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    public static final String KEY_REMINDER_SOUND = "reminder_sound";
    public static final String KEY_VIBRATION_ENABLED = "vibration_enabled";

    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_SYSTEM = 3;

    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_NOTE = "extra_note";
    public static final String EXTRA_INITIAL_QUERY = "extra_initial_query";
    public static final String EXTRA_VOICE_TRIGGER = "extra_voice_trigger";
    public static final String EXTRA_VOICE_TITLE = "extra_voice_title";
    public static final String EXTRA_VOICE_CONTENT = "extra_voice_content";

    public static final String CHANNEL_REMINDERS_ID = "channel_voicenotes_reminders_v2";
    public static final String CHANNEL_REMINDERS_NAME = "VoiceNotes Reminders";

    public static final List<String> CATEGORIES = Arrays.asList(
            "General",
            "Work",
            "Study",
            "Ideas",
            "Personal",
            "To-Do"
    );

    public static final List<String> CATEGORY_FILTERS = Arrays.asList(
            "All",
            "General",
            "Work",
            "Study",
            "Ideas",
            "Personal",
            "To-Do"
    );
}
