package com.voicenotes.app.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.voicenotes.app.database.Note;

import java.util.List;

/**
 * Utility for exporting and sharing notes via standard Android Share Sheets.
 */
public final class ExportUtils {

    private ExportUtils() {}

    public static void exportSingleNote(Context context, Note note) {
        if (note == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== VoiceNotes Export ===\n\n");
        sb.append("Title: ").append(note.getTitle() != null ? note.getTitle() : "Untitled").append("\n");
        sb.append("Category: ").append(note.getCategory()).append("\n");
        sb.append("Created: ").append(DateUtils.formatDateTime(note.getCreatedAt())).append("\n");
        sb.append("Modified: ").append(DateUtils.formatDateTime(note.getUpdatedAt())).append("\n");
        if (note.isReminderEnabled() && note.getReminderTime() > 0) {
            sb.append("Reminder: ").append(DateUtils.formatDateTime(note.getReminderTime())).append("\n");
        }
        sb.append("\nContent:\n");
        sb.append(note.getContent() != null ? note.getContent() : "").append("\n");

        shareText(context, sb.toString(), "Share Note: " + note.getTitle());
    }

    public static void exportAllNotes(Context context, List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            Toast.makeText(context, "No notes available to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("       VOICENOTES BACKUP EXPORT\n");
        sb.append("       Exported: ").append(DateUtils.formatDateTime(System.currentTimeMillis())).append("\n");
        sb.append("       Total Notes: ").append(notes.size()).append("\n");
        sb.append("========================================\n\n");

        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sb.append("Note #").append(i + 1).append("\n");
            sb.append("Title: ").append(note.getTitle() != null ? note.getTitle() : "Untitled").append("\n");
            sb.append("Category: ").append(note.getCategory()).append("\n");
            sb.append("Modified: ").append(DateUtils.formatDateTime(note.getUpdatedAt())).append("\n");
            sb.append("Content:\n").append(note.getContent() != null ? note.getContent() : "").append("\n");
            sb.append("----------------------------------------\n\n");
        }

        shareText(context, sb.toString(), "Export All VoiceNotes (" + notes.size() + " notes)");
    }

    private static void shareText(Context context, String text, String chooserTitle) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, chooserTitle);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(shareIntent);
    }
}
