package util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * Utility class for handling all date and time operations in the clinic application.
 * Provides centralized parsing, formatting, and interval calculations using Java's time API.
 * All operations use strict validation to ensure data integrity.
 */
public class TimeUtil {

    // Standard date and time patterns
    private static final String DATE_PATTERN = "dd-MM-uuuu";
    private static final String TIME_PATTERN = "HH:mm";
    private static final String TIME_SECONDS_PATTERN = "HH:mm:ss";
    private static final String DATETIME_PATTERN = "dd-MM-uuuu HH:mm";
    private static final String DATETIME_SECONDS_PATTERN = "dd-MM-uuuu HH:mm:ss";

    // Pre-defined formatters with strict validation
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_SECONDS_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DATETIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_SECONDS_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    // Pattern to check for seconds component (two colons)
    private static final Pattern TIME_WITH_SECONDS_PATTERN = Pattern.compile(".*:.*:.*");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // --- New: sanitize potentially problematic unicode before parsing ---
    private static String sanitizeForParsing(String s) {
        if (s == null) return null;
        // Fast path common replacements
        String t = s
                .replace('\u00A0', ' ')  // NBSP
                .replace('\u202F', ' ')  // NARROW NBSP
                .replace('\u2007', ' ')  // FIGURE SPACE
                .replace('\u2010', '-')  // HYPHEN
                .replace('\u2011', '-')  // NB HYPHEN
                .replace('\u2012', '-')  // FIGURE DASH
                .replace('\u2013', '-')  // EN DASH
                .replace('\u2014', '-')  // EM DASH
                .replace('\u2212', '-')  // MINUS SIGN
                .replace('\uFF1A', ':')  // FULL-WIDTH COLON
                .replace("\uFEFF", "") // BOM
                .replace("\u200B", "") // ZERO WIDTH SPACE
                .replace("\u200C", "") // ZERO WIDTH NON-JOINER
                .replace("\u200D", "") // ZERO WIDTH JOINER
                .replace("\u2060", "") // WORD JOINER
                .replace("\u200E", "") // LRM
                .replace("\u200F", ""); // RLM
        // Also handle RATIO COLON and MODIFIER COLON variants
        t = t.replace('\u2236', ':').replace('\u02D0', ':').replace('\uA789', ':');

        // Now strictly rebuild from allowed chars only: digits, '-', ':', ' '
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isDigit(cp)) {
                int val = Character.getNumericValue(cp);
                if (val >= 0 && val <= 9) sb.append((char) ('0' + val));
                continue;
            }
            switch (cp) {
                case ' ': sb.append(' '); break;
                case '-': sb.append('-'); break;
                case ':': sb.append(':'); break;
                default: // skip everything else
            }
        }
        // collapse spaces and trim
        String out = sb.toString().replaceAll("\\s+", " ").trim();
        return out;
    }

    /**
     * Parses a date string in format "dd-MM-yyyy".
     *
     * @param s The date string to parse
     * @return Parsed LocalDate
     * @throws IllegalArgumentException if the string is null, empty or cannot be parsed
     */
    public static LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string cannot be null or empty. Expected format: " + DATE_PATTERN);
        }

        try {
            String norm = sanitizeForParsing(s);
            return LocalDate.parse(norm, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: '" + s +
                    "'. Expected format: " + DATE_PATTERN, e);
        }
    }

    /**
     * Formats a LocalDate into a string using pattern "dd-MM-yyyy".
     *
     * @param d The LocalDate to format
     * @return Formatted date string
     * @throws IllegalArgumentException if the date is null
     */
    public static String formatDate(LocalDate d) {
        if (d == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return d.format(DATE_FORMATTER);
    }

    /**
     * Parses a time string in format "HH:mm" or "HH:mm:ss".
     * If seconds are omitted, they are set to 0.
     *
     * @param s The time string to parse
     * @return Parsed LocalTime
     * @throws IllegalArgumentException if the string is null, empty or cannot be parsed
     */
    public static LocalTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be null or empty. " +
                    "Expected format: " + TIME_PATTERN + " or " + TIME_SECONDS_PATTERN);
        }

        String trimmed = sanitizeForParsing(s);
        boolean hasSeconds = TIME_WITH_SECONDS_PATTERN.matcher(trimmed).matches();

        try {
            if (hasSeconds) {
                return LocalTime.parse(trimmed, TIME_SECONDS_FORMATTER);
            } else {
                return LocalTime.parse(trimmed, TIME_FORMATTER);
            }
        } catch (DateTimeParseException e1) {
            // If specific format detection failed, try both formats
            try {
                return LocalTime.parse(trimmed, TIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalTime.parse(trimmed, TIME_SECONDS_FORMATTER);
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Invalid time format: '" + s +
                            "'. Expected format: " + TIME_PATTERN + " or " + TIME_SECONDS_PATTERN, e3);
                }
            }
        }
    }

    /**
     * Formats a LocalTime into a string using pattern "HH:mm".
     *
     * @param t The LocalTime to format
     * @return Formatted time string without seconds
     * @throws IllegalArgumentException if the time is null
     */
    public static String formatTime(LocalTime t) {
        if (t == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        return t.format(TIME_FORMATTER);
    }

    /**
     * Formats a LocalTime into a string using pattern "HH:mm:ss".
     *
     * @param t The LocalTime to format
     * @return Formatted time string with seconds
     * @throws IllegalArgumentException if the time is null
     */
    public static String formatTimeSeconds(LocalTime t) {
        if (t == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        return t.format(TIME_SECONDS_FORMATTER);
    }

    /**
     * Parses a datetime string in format "dd-MM-yyyy HH:mm" or "dd-MM-yyyy HH:mm:ss".
     * If seconds are omitted, they are set to 0.
     *
     * @param s The datetime string to parse
     * @return Parsed LocalDateTime
     * @throws IllegalArgumentException if the string is null, empty or cannot be parsed
     */
    public static LocalDateTime parseDateTime(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Datetime string cannot be null or empty. " +
                    "Expected format: " + DATETIME_PATTERN + " or " + DATETIME_SECONDS_PATTERN);
        }

        String trimmed = sanitizeForParsing(s);
        boolean hasSeconds = TIME_WITH_SECONDS_PATTERN.matcher(trimmed).matches();

        try {
            if (hasSeconds) {
                return LocalDateTime.parse(trimmed, DATETIME_SECONDS_FORMATTER);
            } else {
                return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
            }
        } catch (DateTimeParseException e1) {
            // If specific format detection failed, try both formats
            try {
                return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(trimmed, DATETIME_SECONDS_FORMATTER);
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Invalid datetime format: '" + s +
                            "'. Expected format: " + DATETIME_PATTERN + " or " + DATETIME_SECONDS_PATTERN, e3);
                }
            }
        }
    }

    /**
     * Formats a LocalDateTime into a string using pattern "dd-MM-yyyy HH:mm".
     *
     * @param dt The LocalDateTime to format
     * @return Formatted datetime string without seconds
     * @throws IllegalArgumentException if the datetime is null
     */
    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) {
            throw new IllegalArgumentException("DateTime cannot be null");
        }
        return dt.format(DATETIME_FORMATTER);
    }

    /**
     * Formats a LocalDateTime into a string using pattern "dd-MM-yyyy HH:mm:ss".
     *
     * @param dt The LocalDateTime to format
     * @return Formatted datetime string with seconds
     * @throws IllegalArgumentException if the datetime is null
     */
    public static String formatDateTimeSeconds(LocalDateTime dt) {
        if (dt == null) {
            throw new IllegalArgumentException("DateTime cannot be null");
        }
        return dt.format(DATETIME_SECONDS_FORMATTER);
    }

    /**
     * Gets the current date and time.
     *
     * @return Current LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Gets the current date and time formatted as a string with seconds.
     *
     * @return Current datetime as string in format "dd-MM-yyyy HH:mm:ss"
     */
    public static String nowStringSeconds() {
        return formatDateTimeSeconds(now());
    }

    /**
     * Computes the end time of an appointment by adding duration to the start time.
     *
     * @param start Start time of appointment
     * @param durationMinutes Duration in minutes
     * @return End time of appointment
     * @throws IllegalArgumentException if start is null or durationMinutes is not positive
     */
    public static LocalDateTime computeEnd(LocalDateTime start, int durationMinutes) {
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive, got: " + durationMinutes);
        }
        return start.plusMinutes(durationMinutes);
    }

    /**
     * Calculates the duration in minutes between start and end times.
     *
     * @param start Start time
     * @param end End time
     * @return Duration in minutes
     * @throws IllegalArgumentException if start or end is null, or if end is not after start
     */
    public static int durationMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times cannot be null");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        return (int) Duration.between(start, end).toMinutes();
    }

    /**
     * Checks if two time intervals overlap using half-open interval semantics [start, end).
     * Overlap occurs when aStart < bEnd AND bStart < aEnd.
     *
     * @param aStart Start of first interval
     * @param aEnd End of first interval
     * @param bStart Start of second interval
     * @param bEnd End of second interval
     * @return true if intervals overlap, false otherwise
     * @throws IllegalArgumentException if any parameter is null or if either end is not after its start
     */
    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd,
                                   LocalDateTime bStart, LocalDateTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
            throw new IllegalArgumentException("Time parameters cannot be null");
        }

        if (!aEnd.isAfter(aStart)) {
            throw new IllegalArgumentException("End time must be after start time for first interval");
        }

        if (!bEnd.isAfter(bStart)) {
            throw new IllegalArgumentException("End time must be after start time for second interval");
        }

        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /**
     * Combines a date string and a time string into a LocalDateTime.
     *
     * @param date_ddMMyyyy Date in format "dd-MM-yyyy"
     * @param time_HHmm_or_HHmmss Time in format "HH:mm" or "HH:mm:ss"
     * @return Combined LocalDateTime
     * @throws IllegalArgumentException if either string is invalid
     */
    public static LocalDateTime combine(String date_ddMMyyyy, String time_HHmm_or_HHmmss) {
        LocalDate date = parseDate(date_ddMMyyyy);
        LocalTime time = parseTime(time_HHmm_or_HHmmss);
        return LocalDateTime.of(date, time);
    }

    /**
     * Normalizes a date string by parsing and reformatting it.
     *
     * @param date Date string to normalize
     * @return Normalized date string in format "dd-MM-yyyy"
     * @throws IllegalArgumentException if the input is invalid
     */
    public static String normalizeDate(String date) {
        return formatDate(parseDate(date));
    }

    /**
     * Normalizes a time string by parsing and reformatting it.
     * Preserves seconds if they were in the original string.
     *
     * @param time Time string to normalize
     * @return Normalized time string in format "HH:mm" or "HH:mm:ss"
     * @throws IllegalArgumentException if the input is invalid
     */
    public static String normalizeTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be null or empty");
        }

        LocalTime parsed = parseTime(time);
        boolean hasSeconds = TIME_WITH_SECONDS_PATTERN.matcher(time.trim()).matches();

        return hasSeconds ? formatTimeSeconds(parsed) : formatTime(parsed);
    }

    /**
     * Normalizes a datetime string by parsing and reformatting it.
     * Preserves seconds if they were in the original string.
     *
     * @param dateTime DateTime string to normalize
     * @return Normalized datetime string in format "dd-MM-yyyy HH:mm" or "dd-MM-yyyy HH:mm:ss"
     * @throws IllegalArgumentException if the input is invalid
     */
    public static String normalizeDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            throw new IllegalArgumentException("DateTime string cannot be null or empty");
        }

        LocalDateTime parsed = parseDateTime(dateTime);
        boolean hasSeconds = TIME_WITH_SECONDS_PATTERN.matcher(dateTime.trim()).matches();

        return hasSeconds ? formatDateTimeSeconds(parsed) : formatDateTime(parsed);
    }

    /**
     * Parses a duration string in format "HH:mm" to total minutes.
     *
     * @param hhColonMm Duration string in format "HH:mm"
     * @return Total duration in minutes
     * @throws IllegalArgumentException if the input is invalid or contains seconds
     */
    public static int parseDurationToMinutes(String hhColonMm) {
        if (hhColonMm == null || hhColonMm.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be null or empty");
        }

        String trimmed = hhColonMm.trim();

        // Check for seconds component (reject if present)
        if (TIME_WITH_SECONDS_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Duration format must be 'HH:mm' without seconds, got: " + hhColonMm);
        }

        try {
            // Parse as time and convert to minutes
            LocalTime time = LocalTime.parse(trimmed, TIME_FORMATTER);
            return time.getHour() * 60 + time.getMinute();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid duration format: '" + hhColonMm +
                    "'. Expected format: " + TIME_PATTERN, e);
        }
    }

    /**
     * Formats a duration in minutes to a string in format "HH:mm".
     *
     * @param minutes Total duration in minutes
     * @return Formatted duration string
     * @throws IllegalArgumentException if minutes is negative
     */
    public static String formatDurationFromMinutes(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Duration minutes cannot be negative, got: " + minutes);
        }

        int hours = minutes / 60;
        int mins = minutes % 60;

        return String.format("%02d:%02d", hours, mins);
    }
}
