package com.voicenotes.app.voice;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Voice Command Parser and Intent Extractor for VoiceNotes.
 */
public class VoiceCommandProcessor {

    public enum CommandType {
        CREATE_NOTE,
        CREATE_TODO,
        COMPLETE_TASK,
        SNOOZE_REMINDER,
        DISMISS_REMINDER,
        SEARCH_NOTES,
        READ_LATEST_NOTE,
        READ_ALL_NOTES,
        READ_CURRENT_NOTE,
        DELETE_NOTE,
        DELETE_ALL_NOTES,
        SET_REMINDER,
        CHANGE_THEME_DARK,
        CHANGE_THEME_LIGHT,
        NAVIGATE_SETTINGS,
        NAVIGATE_HOME,
        DICTATE_CONTENT
    }

    public static class ParsedCommand {
        private final CommandType type;
        private final String rawText;
        private String title = "";
        private String content = "";
        private String query = "";
        private String timeArgument = "";

        public ParsedCommand(CommandType type, String rawText) {
            this.type = type;
            this.rawText = rawText;
        }

        public CommandType getType() {
            return type;
        }

        public String getRawText() {
            return rawText;
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

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getTimeArgument() {
            return timeArgument;
        }

        public void setTimeArgument(String timeArgument) {
            this.timeArgument = timeArgument;
        }

        public String getSummary() {
            switch (type) {
                case CREATE_NOTE:
                    return "Create Note" + (!title.isEmpty() ? " titled '" + title + "'" : "");
                case CREATE_TODO:
                    return "Create To-Do" + (!title.isEmpty() ? " '" + title + "'" : "");
                case COMPLETE_TASK:
                    return "Mark Task Done" + (!title.isEmpty() ? " '" + title + "'" : "");
                case SNOOZE_REMINDER:
                    return "Snooze Reminder" + (!timeArgument.isEmpty() ? " for " + timeArgument : "");
                case DISMISS_REMINDER:
                    return "Dismiss Reminder";
                case SEARCH_NOTES:
                    return "Search" + (!query.isEmpty() ? " for '" + query + "'" : "");
                case READ_LATEST_NOTE:
                    return "Read Latest Note";
                case READ_ALL_NOTES:
                    return "Read All Notes";
                case READ_CURRENT_NOTE:
                    return "Read Current Note";
                case DELETE_NOTE:
                    return "Delete Note" + (!title.isEmpty() ? " titled '" + title + "'" : "");
                case DELETE_ALL_NOTES:
                    return "Delete All Notes";
                case SET_REMINDER:
                    return "Set Reminder" + (!timeArgument.isEmpty() ? " for '" + timeArgument + "'" : "");
                case CHANGE_THEME_DARK:
                    return "Turn On Dark Mode";
                case CHANGE_THEME_LIGHT:
                    return "Turn On Light Mode";
                case NAVIGATE_SETTINGS:
                    return "Open Settings";
                case NAVIGATE_HOME:
                    return "Go Home";
                case DICTATE_CONTENT:
                default:
                    return "Dictated Note Content";
            }
        }
    }

    /**
     * Parses the spoken input into a structured command.
     */
    public static ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParsedCommand(CommandType.DICTATE_CONTENT, "");
        }

        String raw = input.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. Theme Commands
        if (lower.matches(".*(turn on|enable|switch to|activate) dark (mode|theme).*") || lower.equals("dark mode")) {
            return new ParsedCommand(CommandType.CHANGE_THEME_DARK, raw);
        }
        if (lower.matches(".*(turn off dark|disable dark|turn on light|enable light|switch to light) (mode|theme).*") || lower.equals("light mode")) {
            return new ParsedCommand(CommandType.CHANGE_THEME_LIGHT, raw);
        }

        // 2. Navigation Commands
        if (lower.matches(".*(open|go to|show) settings.*")) {
            return new ParsedCommand(CommandType.NAVIGATE_SETTINGS, raw);
        }
        if (lower.matches(".*(go home|open notes|show my notes|go back|home screen).*")) {
            return new ParsedCommand(CommandType.NAVIGATE_HOME, raw);
        }

        // 3. Task Completion Commands ("Mark Java assignment as done", "I completed my Java assignment", "Complete Java assignment")
        Pattern completePattern = Pattern.compile("^(?:mark|set)(?: the)? (?:task|todo|to-do|note)? (.+?) (?:as )?(?:done|completed|finished)$", Pattern.CASE_INSENSITIVE);
        Matcher completeMatcher = completePattern.matcher(raw);
        if (completeMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.COMPLETE_TASK, raw);
            cmd.setTitle(cleanTitle(completeMatcher.group(1)));
            return cmd;
        }

        Pattern completedByPattern = Pattern.compile("^(?:i )?(?:completed|finished|done with)(?: my| the)? (?:task|todo|to-do|note)? (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher completedByMatcher = completedByPattern.matcher(raw);
        if (completedByMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.COMPLETE_TASK, raw);
            cmd.setTitle(cleanTitle(completedByMatcher.group(1)));
            return cmd;
        }

        Pattern completeDirectPattern = Pattern.compile("^(?:complete|finish) (?:my |the )?(?:task|todo|to-do|note)? (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher completeDirectMatcher = completeDirectPattern.matcher(raw);
        if (completeDirectMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.COMPLETE_TASK, raw);
            cmd.setTitle(cleanTitle(completeDirectMatcher.group(1)));
            return cmd;
        }

        // 4. Snooze & Dismiss Voice Commands
        Pattern snoozePattern = Pattern.compile("^(?:snooze)(?: this| the)?(?: reminder)?(?: for)? (.+)?$", Pattern.CASE_INSENSITIVE);
        Matcher snoozeMatcher = snoozePattern.matcher(raw);
        if (snoozeMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.SNOOZE_REMINDER, raw);
            String duration = snoozeMatcher.group(1);
            cmd.setTimeArgument(duration != null && !duration.trim().isEmpty() ? duration.trim() : "10 minutes");
            return cmd;
        }

        if (lower.matches(".*(dismiss|cancel)(?: this| the)? reminder.*") || lower.equals("dismiss") || lower.equals("dismiss reminder")) {
            return new ParsedCommand(CommandType.DISMISS_REMINDER, raw);
        }

        // 5. TTS / Read Commands
        if (lower.matches(".*(read|speak|play) (my )?latest note.*") || lower.matches(".*(read|speak) (the )?last note.*")) {
            return new ParsedCommand(CommandType.READ_LATEST_NOTE, raw);
        }
        if (lower.matches(".*(read|speak|play) (all |my )?notes.*")) {
            return new ParsedCommand(CommandType.READ_ALL_NOTES, raw);
        }
        if (lower.matches(".*(read|speak|play) this note.*")) {
            return new ParsedCommand(CommandType.READ_CURRENT_NOTE, raw);
        }

        // 6. Delete Commands
        if (lower.matches(".*(delete|remove|clear) all (my )?notes.*")) {
            return new ParsedCommand(CommandType.DELETE_ALL_NOTES, raw);
        }

        Pattern deletePattern = Pattern.compile("^(?:delete|remove)(?: the)? note (?:titled|named|called|about) (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher deleteMatcher = deletePattern.matcher(raw);
        if (deleteMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.DELETE_NOTE, raw);
            cmd.setTitle(cleanTitle(deleteMatcher.group(1)));
            return cmd;
        }

        // 7. Create To-Do Commands ("Create a to-do to submit Java assignment", "Add a task to buy groceries")
        Pattern createTodoPattern = Pattern.compile("^(?:create|add|make|new)(?: a)? (?:to-do|todo|task) (?:to|for|about|titled|called)? (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher createTodoMatcher = createTodoPattern.matcher(raw);
        if (createTodoMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_TODO, raw);
            cmd.setTitle(cleanTitle(createTodoMatcher.group(1)));
            return cmd;
        }

        // 8. Reminder Commands
        Pattern reminderPattern = Pattern.compile("^(?:set a? reminder|remind me)(?: for| at| on)? (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher reminderMatcher = reminderPattern.matcher(raw);
        if (reminderMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.SET_REMINDER, raw);
            cmd.setTimeArgument(reminderMatcher.group(1).trim());
            return cmd;
        }
        if (lower.matches("^(?:set reminder|set a reminder|remind me)$")) {
            return new ParsedCommand(CommandType.SET_REMINDER, raw);
        }

        // 9. Search Commands
        Pattern searchPattern = Pattern.compile("^(?:search for|search notes for|search|find notes about|find my notes about|find) (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher searchMatcher = searchPattern.matcher(raw);
        if (searchMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.SEARCH_NOTES, raw);
            cmd.setQuery(searchMatcher.group(1).trim());
            return cmd;
        }
        if (lower.matches("^(?:search notes|find my notes|search)$")) {
            return new ParsedCommand(CommandType.SEARCH_NOTES, raw);
        }

        // 10. Create Regular Note Commands
        // Form: "Create a note titled [title] with content [content]"
        Pattern titleContentPattern = Pattern.compile("^(?:create|make|add|new)(?: a)? note (?:titled|called|named|title as|title is|with title as|with title|title) (.+?) (?:with content|saying|and text|and content|and add|content) (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher titleContentMatcher = titleContentPattern.matcher(raw);
        if (titleContentMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_NOTE, raw);
            cmd.setTitle(cleanTitle(titleContentMatcher.group(1)));
            cmd.setContent(titleContentMatcher.group(2).trim());
            return cmd;
        }

        // Form: "Create a note titled [title]" / "New note titled [title]"
        Pattern titleOnlyPattern = Pattern.compile("^(?:create|make|add|new)(?: a)? note (?:titled|called|named|title as|title is|with title as|with title|title) (.+)$", Pattern.CASE_INSENSITIVE);
        Matcher titleOnlyMatcher = titleOnlyPattern.matcher(raw);
        if (titleOnlyMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_NOTE, raw);
            cmd.setTitle(cleanTitle(titleOnlyMatcher.group(1)));
            return cmd;
        }

        // Form: "Add [content] to my notes"
        Pattern addToNotesPattern = Pattern.compile("^add (.+?) to (?:my )?notes$", Pattern.CASE_INSENSITIVE);
        Matcher addToNotesMatcher = addToNotesPattern.matcher(raw);
        if (addToNotesMatcher.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_NOTE, raw);
            cmd.setContent(addToNotesMatcher.group(1).trim());
            return cmd;
        }

        // Form: "Create a note" / "New note"
        if (lower.matches("^(?:create a note|create note|new note|add a note|add note|make a note|make note)$")) {
            return new ParsedCommand(CommandType.CREATE_NOTE, raw);
        }

        // 11. Fallback: Direct Dictated Note Content
        ParsedCommand dictate = new ParsedCommand(CommandType.DICTATE_CONTENT, raw);
        dictate.setContent(raw);
        return dictate;
    }

    private static String cleanTitle(String input) {
        if (input == null) return "";
        String title = input.replaceAll("^[\"']|[\"']$", "").replaceAll("^(?:as|is|to|named|called|for)\\s+", "").trim();
        if (title.isEmpty()) return "";
        return Character.toUpperCase(title.charAt(0)) + title.substring(1);
    }
}
