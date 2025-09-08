package repo;

import model.CheckIn;
import util.TimeUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Flat-file repository for completed check-in records.
 * Stores the history of check-ins that have been completed and removed from active queue.
 *
 * This class uses a pipe-delimited text file format to store historical check-in data.
 * Records are append-only, meaning new records are added to the end of the file without
 * modifying existing data, which provides durability and faster writes.
 *
 * The file format is:
 * id|appointmentId|patientId|doctorId|status|walkIn(1/0)|priority|desk|notes|checkedInAt|completedAt|updatedAt|createdAt
 *
 * This repository supports backward compatibility with older file formats by handling
 * both the current 13-column format and a legacy 17-column format.
 */
public class CheckInHistoryRepository {

    public static final String FILE = "src/data/checkins_history.txt";
    private static final char SEP = '|';

    /**
     * Record class representing a historical check-in entry.
     * Contains all data about a completed check-in that has been archived.
     *
     * Fields directly correspond to the columns in the flat file format,
     * allowing straightforward serialization and deserialization.
     */
    public static class Record {
        public long checkInId;
        public long appointmentId;
        public long patientId;
        public long doctorId;
        public String status; // terminal (Completed)
        public boolean walkIn;
        public int priority;
        public String desk;
        public String notes;
        public String checkedInAt;
        public String completedAt;
        public String createdAt;
        public String updatedAt;

        /**
         * Creates a history record from an active check-in.
         * Extracts all relevant data from the check-in and doctor ID to create a permanent history record.
         *
         * @param c        The check-in to create a record from
         * @param doctorId The ID of the doctor associated with this check-in
         * @return A new history record containing all check-in data
         */
        public static Record from(CheckIn c, long doctorId) {
            Record r = new Record();
            r.checkInId = c.getId();
            r.appointmentId = c.getAppointmentId();
            r.patientId = c.getPatientId();
            r.doctorId = doctorId;
            r.status = c.getStatus();
            r.walkIn = c.isWalkIn();
            r.priority = c.getPriority();
            r.desk = sanitize(c.getDesk());
            r.notes = sanitize(c.getNotes());
            r.checkedInAt = nz(c.getCheckedInAt());
            r.completedAt = nz(c.getCompletedAt());
            r.createdAt = nz(c.getCreatedAt());
            r.updatedAt = nz(c.getUpdatedAt());
            return r;
        }

        /**
         * Serializes the record to a pipe-delimited string.
         * Creates a compact 13-column format suitable for storage.
         *
         * @return A pipe-delimited string representation of this record
         */
        public String serialize() {
            return checkInId + String.valueOf(SEP) +
                    appointmentId + SEP +
                    patientId + SEP +
                    doctorId + SEP +
                    nz(status) + SEP +
                    (walkIn ? "1" : "0") + SEP +
                    priority + SEP +
                    nz(desk) + SEP +
                    nz(notes) + SEP +
                    nz(checkedInAt) + SEP +
                    nz(completedAt) + SEP +
                    nz(updatedAt) + SEP +
                    nz(createdAt);
        }

        /**
         * Parses a record from a pipe-delimited string.
         * Handles both the current 13-column format and legacy 17-column format for backward compatibility.
         *
         * @param line The line to parse
         * @return A parsed record
         * @throws IllegalArgumentException if the line format is invalid
         */
        public static Record parse(String line) {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 13 && parts.length != 17) {
                throw new IllegalArgumentException("Bad history record token count: " + parts.length + " -> " + Arrays.toString(parts));
            }
            Record r = new Record();
            try {
                if (parts.length == 13) {
                    int i = 0;
                    r.checkInId = Long.parseLong(parts[i++]);
                    r.appointmentId = Long.parseLong(parts[i++]);
                    r.patientId = Long.parseLong(parts[i++]);
                    r.doctorId = Long.parseLong(parts[i++]);
                    r.status = parts[i++];
                    r.walkIn = "1".equals(parts[i++]);
                    r.priority = Integer.parseInt(parts[i++]);
                    r.desk = emptyToNull(parts[i++]);
                    r.notes = emptyToNull(parts[i++]);
                    r.checkedInAt = emptyToNull(parts[i++]);
                    r.completedAt = emptyToNull(parts[i++]);
                    r.updatedAt = emptyToNull(parts[i++]);
                    r.createdAt = emptyToNull(parts[i]);
                } else { // legacy 17
                    int i = 0;
                    r.checkInId = Long.parseLong(parts[i++]);
                    r.appointmentId = Long.parseLong(parts[i++]);
                    r.patientId = Long.parseLong(parts[i++]);
                    r.doctorId = Long.parseLong(parts[i++]);
                    r.status = parts[i++];
                    r.walkIn = "1".equals(parts[i++]);
                    r.priority = Integer.parseInt(parts[i++]);
                    r.desk = emptyToNull(parts[i++]);
                    r.notes = emptyToNull(parts[i++]);
                    r.checkedInAt = emptyToNull(parts[i++]);
                    String _calledAt = parts[i++]; // ignored
                    String _startedAt = parts[i++]; // ignored
                    r.completedAt = emptyToNull(parts[i++]);
                    String _cancelledAt = parts[i++]; // ignored
                    String _noShowAt = parts[i++]; // ignored
                    r.updatedAt = emptyToNull(parts[i++]);
                    r.createdAt = emptyToNull(parts[i++]);
                }
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Malformed history row: " + ex.getMessage(), ex);
            }
            return r;
        }

        /**
         * Converts null string to empty string.
         * Used during serialization to ensure consistent output format.
         *
         * @param s String to convert
         * @return Empty string if input is null, otherwise the input string
         */
        private static String nz(String s) {
            return s == null ? "" : s;
        }

        /**
         * Converts empty string to null.
         * Used during parsing to ensure consistent data model.
         *
         * @param s String to convert
         * @return Null if input is null or empty, otherwise the input string
         */
        private static String emptyToNull(String s) {
            return (s == null || s.isEmpty()) ? null : s;
        }

        /**
         * Sanitizes a string by replacing pipe characters with slashes.
         * This prevents delimiter conflicts in the flat file format.
         *
         * @param s String to sanitize
         * @return Sanitized string or null if input is null
         */
        private static String sanitize(String s) {
            return s == null ? null : s.replace('|', '/');
        }
    }

    private final Path file;

    /**
     * Creates a repository using the default file path.
     */
    public CheckInHistoryRepository() {
        this(FILE);
    }

    /**
     * Creates a repository with a custom file path.
     *
     * @param path The path to the history file
     */
    public CheckInHistoryRepository(String path) {
        this.file = Paths.get(path);
    }

    /**
     * Appends a record to the history file.
     * Creates parent directories if they don't exist and appends the record to the end of the file.
     *
     * This is an append-only operation, which helps preserve historical integrity.
     *
     * @param r The record to append to history
     * @throws UncheckedIOException if writing to the file fails
     */
    public void append(Record r) {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
                bw.write(r.serialize());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads all records from the history file.
     * Records are sorted with most recent first based on check-in time.
     * Skips malformed lines with a warning message.
     *
     * @return List of all history records, sorted by check-in time (recent first)
     * @throws UncheckedIOException if reading from the file fails
     */
    public List<Record> loadAll() {
        List<Record> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    out.add(Record.parse(line));
                } catch (Exception ex) {
                    System.err.println("[History] Skip: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Modified: sort only by checkedInAt timestamp (most recent first)
        out.sort((a, b) -> {
            try {
                var adt = TimeUtil.parseDateTime(a.checkedInAt);
                var bdt = TimeUtil.parseDateTime(b.checkedInAt);
                return bdt.compareTo(adt); // most recent first
            } catch (Exception e) {
                return 0;
            }
        });
        return out;
    }

    /**
     * Clears all history by deleting the history file.
     * This operation permanently removes all historical records.
     *
     * @throws UncheckedIOException if deleting the file fails
     */
    public void clearAll() {
        try {
            if (Files.exists(file)) Files.delete(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
