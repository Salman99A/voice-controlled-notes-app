package com.voicenotes.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for date formatting and natural language voice reminder time parsing.
 */
public final class DateUtils {

    private DateUtils() {}

    public static String formatRelative(long millis) {
        if (millis <= 0) return "";

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeStr = timeFormat.format(new Date(millis));

        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Today • " + timeStr;
        }

        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday • " + timeStr;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
        return dateFormat.format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    /**
     * Parses natural language reminder arguments like "tomorrow at 10 AM", "in 1 minute", "5 mins", "in 2 hours".
     */
    public static long parseReminderTimeString(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            return System.currentTimeMillis() + 3600000;
        }

        String lower = argument.toLowerCase(Locale.ROOT).trim();
        Calendar cal = Calendar.getInstance();

        // Check for seconds, e.g. "10 seconds", "in 30 sec"
        Pattern secPattern = Pattern.compile("(?:in\\s+)?(\\d+)\\s*(?:second|sec)s?", Pattern.CASE_INSENSITIVE);
        Matcher secMatcher = secPattern.matcher(lower);
        if (secMatcher.find()) {
            try {
                int count = Integer.parseInt(secMatcher.group(1));
                return System.currentTimeMillis() + ((long) count * 1000);
            } catch (Exception ignored) {}
        }

        // Check for minutes, e.g. "1 minute", "in 2 minutes", "5 mins", "in 5 min"
        Pattern minPattern = Pattern.compile("(?:in\\s+)?(\\d+)\\s*(?:minute|min)s?", Pattern.CASE_INSENSITIVE);
        Matcher minMatcher = minPattern.matcher(lower);
        if (minMatcher.find()) {
            try {
                int count = Integer.parseInt(minMatcher.group(1));
                return System.currentTimeMillis() + ((long) count * 60 * 1000);
            } catch (Exception ignored) {}
        }

        // Check for hours, e.g. "1 hour", "in 2 hours", "2 hrs"
        Pattern hrPattern = Pattern.compile("(?:in\\s+)?(\\d+)\\s*(?:hour|hr)s?", Pattern.CASE_INSENSITIVE);
        Matcher hrMatcher = hrPattern.matcher(lower);
        if (hrMatcher.find()) {
            try {
                int count = Integer.parseInt(hrMatcher.group(1));
                return System.currentTimeMillis() + ((long) count * 3600 * 1000);
            } catch (Exception ignored) {}
        }

        // Check if "tomorrow" is mentioned
        if (lower.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Check for specific hour/min e.g. "10:30 am", "5 pm", "10 am"
        Pattern timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");
        Matcher matcher = timePattern.matcher(lower);
        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
                String ampm = matcher.group(3);

                if ("pm".equalsIgnoreCase(ampm) && hour < 12) {
                    hour += 12;
                } else if ("am".equalsIgnoreCase(ampm) && hour == 12) {
                    hour = 0;
                }

                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                // If scheduled time is in the past for today, push to tomorrow
                if (cal.getTimeInMillis() <= System.currentTimeMillis() && !lower.contains("tomorrow")) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                return cal.getTimeInMillis();
            } catch (Exception ignored) {}
        }

        // Default fallback: 1 hour from now
        return System.currentTimeMillis() + 3600000;
    }
}
