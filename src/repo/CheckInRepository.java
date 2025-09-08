package repo;

import model.CheckIn;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Binary serialization repository for CheckIn state (queue + entities + id counter).
 * Flat-file (pipe-delimited) persistence is intentionally NOT used for check-ins.
 *
 * This repository manages the persistence of the complete check-in system state,
 * including active check-ins, queue structures, and indexing data. It uses Java's
 * built-in serialization to store and retrieve the complete state atomically.
 *
 * Unlike other entities that use flat-file storage, check-ins are serialized as a complete
 * state snapshot to maintain queue integrity and relationships between components.
 */
public class CheckInRepository {

    public static final String DEFAULT_FILE = "src/data/checkins.ser"; // moved under src/data

    /**
     * Serializable aggregate snapshot of all check-in related data.
     *
     * Represents the complete state of the check-in system, including:
     * - All active check-in entities
     * - Priority queues for both walk-in and scheduled patients
     * - Record of walk-in appointments for status tracking
     * - Auto-increment counter for check-in IDs
     * - Index structures for efficient lookups by appointment and patient
     */
    public static class State implements Serializable {
        private static final long serialVersionUID = 3L; // bumped: added scheduledQueue

        /**
         * Maps check-in IDs to their respective CheckIn objects
         * Contains all active check-ins in the system
         */
        public Map<Long, CheckIn> checkIns = new HashMap<>();

        /**
         * Queue of walk-in check-in IDs in priority order
         * Contains only walk-in entries in CheckedIn status
         */
        public Deque<Long> walkInQueue = new LinkedList<>();

        /**
         * Queue of scheduled check-in IDs in appointment time order
         * Contains only scheduled entries in CheckedIn status
         */
        public Deque<Long> scheduledQueue = new LinkedList<>();

        /**
         * Set of appointment IDs that were created through the walk-in process
         * Used to distinguish between scheduled and walk-in appointments
         */
        public Set<Long> walkInAppointmentIds = new HashSet<>();

        /**
         * Next check-in ID to be assigned
         * Auto-increments when new check-ins are created
         */
        public long nextCheckInId = 1L;

        /**
         * Lookup index mapping appointment IDs to their check-in IDs
         * Enables O(1) lookup of check-in by appointment
         */
        public Map<Long, Long> apptIndex = new HashMap<>();

        /**
         * Lookup index mapping patient IDs to lists of their check-in IDs
         * Enables efficient retrieval of all check-ins for a patient
         */
        public Map<Long, List<Long>> patientIndex = new HashMap<>();
    }

    private final Path file;

    /**
     * Creates a repository with the default file path.
     */
    public CheckInRepository() { this(DEFAULT_FILE); }

    /**
     * Creates a repository with a custom file path.
     *
     * @param path Path to the serialized state file
     */
    public CheckInRepository(String path) { this.file = Paths.get(path); }

    /**
     * Loads the check-in system state from the serialization file.
     * If the file doesn't exist or is corrupted, returns a fresh state.
     * This provides fault tolerance against data corruption.
     *
     * @return The loaded state or a new state if loading fails
     */
    public State load() {
        if (!Files.exists(file)) return new State();
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
            Object obj = ois.readObject();
            return (State) obj;
        } catch (Exception e) {
            // Corrupt or incompatible -> start fresh
            return new State();
        }
    }

    /**
     * Saves the check-in system state to the serialization file.
     * Uses atomic file operations to ensure data integrity during saves.
     * The save process:
     * 1. Creates parent directories if needed
     * 2. Writes to a temporary file
     * 3. Atomically replaces the original file with the temporary one
     *
     * @param state The state to save
     * @throws IllegalArgumentException if state is null
     * @throws UncheckedIOException if saving fails due to I/O errors
     */
    public void save(State state) {
        if (state == null) throw new IllegalArgumentException("State cannot be null");
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                oos.writeObject(state);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ioe) {
            throw new UncheckedIOException("Failed to save CheckIn state", ioe);
        }
    }
}
