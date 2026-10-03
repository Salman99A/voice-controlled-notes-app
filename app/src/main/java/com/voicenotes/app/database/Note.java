package com.voicenotes.app.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Room Database Entity representing a Note or To-Do task in VoiceNotes.
 */
@Entity(
    tableName = "notes",
    indices = {
        @Index(value = {"title"}),
        @Index(value = {"category"}),
        @Index(value = {"updatedAt"}),
        @Index(value = {"noteType"}),
        @Index(value = {"taskStatus"})
    }
)
public class Note implements Serializable {

    public static final String TYPE_NOTE = "NOTE";
    public static final String TYPE_TODO = "TODO";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String content;
    private String category;
    private long createdAt;
    private long updatedAt;
    private long reminderTime; // epoch millis (0 if disabled)
    private boolean isReminderEnabled;
    private boolean isPinned;
    private String colorHex;

    // To-Do & Repeating Reminder extension fields
    private String noteType;
    private String taskStatus;
    private long completedAt;
    private long reminderRepeatInterval; // epoch millis (0 = once)
    private boolean reminderDismissed;

    public Note() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.category = "General";
        this.isReminderEnabled = false;
        this.reminderTime = 0;
        this.isPinned = false;
        this.colorHex = "#6C3BFF";
        this.noteType = TYPE_NOTE;
        this.taskStatus = STATUS_PENDING;
        this.completedAt = 0;
        this.reminderRepeatInterval = 0;
        this.reminderDismissed = false;
    }

    @Ignore
    public Note(String title, String content, String category) {
        this();
        this.title = title;
        this.content = content;
        this.category = (category != null && !category.isEmpty()) ? category : "General";
    }

    @Ignore
    public Note(String title, String content, String category, String noteType) {
        this(title, content, category);
        this.noteType = (noteType != null && !noteType.isEmpty()) ? noteType : TYPE_NOTE;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isReminderEnabled() {
        return isReminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        isReminderEnabled = reminderEnabled;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getNoteType() {
        return (noteType != null && !noteType.isEmpty()) ? noteType : TYPE_NOTE;
    }

    public void setNoteType(String noteType) {
        this.noteType = noteType;
    }

    public String getTaskStatus() {
        return (taskStatus != null && !taskStatus.isEmpty()) ? taskStatus : STATUS_PENDING;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public long getReminderRepeatInterval() {
        return reminderRepeatInterval;
    }

    public void setReminderRepeatInterval(long reminderRepeatInterval) {
        this.reminderRepeatInterval = reminderRepeatInterval;
    }

    public boolean isReminderDismissed() {
        return reminderDismissed;
    }

    public void setReminderDismissed(boolean reminderDismissed) {
        this.reminderDismissed = reminderDismissed;
    }

    public boolean isTodo() {
        return TYPE_TODO.equalsIgnoreCase(getNoteType());
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equalsIgnoreCase(getTaskStatus());
    }
}
