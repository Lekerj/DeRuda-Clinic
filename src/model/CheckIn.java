package model;

import util.TimeUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a patient check-in event in the clinic system.
 * A check-in is associated with an appointment and tracks the patient's arrival
 * and movement through the clinic, either for scheduled appointments or walk-ins.
 *
 * The CheckIn model supports two primary workflows:
 * 1. Scheduled check-ins: When patients arrive for their pre-scheduled appointments
 * 2. Walk-in check-ins: When patients arrive without appointments and are handled based on priority
 *
 * Each check-in tracks status, location, arrival time, and completion time.
 */
public class CheckIn extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private long appointmentId;
    private long patientId;
    private String status; // CheckedIn, Completed
    private String desk;   // optional
    private String notes;  // optional

    private boolean walkIn;
    private int priority; // >=0, only meaningful for walk-ins

    private String checkedInAt; // always set
    private String completedAt;

    private static final Set<String> VALID_STATUSES = new HashSet<>(Arrays.asList(
            "CheckedIn","Completed"
    ));

    /**
     * Creates a new check-in with the specified details and current timestamp.
     * This constructor is primarily used for creating new check-ins in the system.
     *
     * @param id Check-in ID
     * @param appointmentId Associated appointment ID
     * @param patientId Patient ID
     * @param status Check-in status (must be a valid status)
     * @param desk Desk location (optional, can be null)
     * @param notes Additional notes (optional, can be null)
     * @param walkIn Whether this is a walk-in check-in
     * @param priority Priority level for walk-ins (higher number = higher priority)
     * @throws IllegalArgumentException if status is invalid or IDs are not positive
     */
    public CheckIn(long id, long appointmentId, long patientId,
                   String status, String desk, String notes,
                   boolean walkIn, int priority) {
        super(id);
        setAppointmentId(appointmentId);
        setPatientId(patientId);
        setWalkIn(walkIn);
        setPriority(priority); // no touch variant in constructor
        setDesk(desk);
        setNotes(notes);
        setStatus(status); // validated
        this.checkedInAt = TimeUtil.nowStringSeconds();
        touch();
    }

    /**
     * Creates a check-in with specified details and timestamps.
     * Used primarily for importing data from persistent storage.
     *
     * @param id Check-in ID
     * @param appointmentId Associated appointment ID
     * @param patientId Patient ID
     * @param status Check-in status (must be a valid status)
     * @param desk Desk location (optional, can be null)
     * @param notes Additional notes (optional, can be null)
     * @param checkedInAt Check-in timestamp in format "dd-MM-yyyy HH:mm:ss"
     * @param completedAt Completion timestamp in format "dd-MM-yyyy HH:mm:ss" (may be null)
     * @param walkIn Whether this is a walk-in check-in
     * @param priority Priority level for walk-ins (higher number = higher priority)
     * @param createdAt Creation timestamp in format "dd-MM-yyyy HH:mm:ss"
     * @param updatedAt Last update timestamp in format "dd-MM-yyyy HH:mm:ss"
     * @throws IllegalArgumentException if status is invalid or IDs are not positive
     */
    public CheckIn(long id, long appointmentId, long patientId,
                   String status, String desk, String notes,
                   String checkedInAt, String completedAt,
                   boolean walkIn, int priority,
                   String createdAt, String updatedAt) {
        super(id, createdAt, updatedAt);
        setAppointmentId(appointmentId);
        setPatientId(patientId);
        setWalkIn(walkIn);
        setPriority(priority);
        setDesk(desk);
        setNotes(notes);
        setStatus(status); // no touch
        this.checkedInAt = checkedInAt;
        this.completedAt = completedAt;
        // preserve imported timestamps: no touch
    }

    // -------- Status helpers --------
    /**
     * Checks if the provided status is valid according to system rules.
     * Valid statuses are defined in the VALID_STATUSES set.
     *
     * @param s The status to validate
     * @return true if the status is valid, false otherwise
     */
    public static boolean isValidStatus(String s) {
        if (s == null) return false;
        return VALID_STATUSES.contains(canonicalStatus(s));
    }

    /**
     * Converts a status string to its canonical form.
     * This normalizes status strings by handling case-insensitivity
     * and matching to the official status names.
     *
     * @param s The status string to canonicalize
     * @return The canonical form of the status or the original if not found
     */
    public static String canonicalStatus(String s) {
        if (s == null) return null;
        String t = s.trim();
        for (String v : VALID_STATUSES) {
            if (v.equalsIgnoreCase(t)) return v; // return canonical case
        }
        return t; // unknown -> original (will fail validation later)
    }

    /**
     * Validates that an ID is positive.
     * Used internally by setters to ensure data integrity.
     *
     * @param v The ID to validate
     * @param field Field name for error messages
     * @throws IllegalArgumentException if ID is not positive
     */
    private void validateIdPositive(long v, String field) {
        if (v <= 0) throw new IllegalArgumentException(field + " must be positive");
    }

    // -------- Getters / Setters --------
    /**
     * Gets the associated appointment ID.
     *
     * @return The appointment ID
     */
    public long getAppointmentId() { return appointmentId; }

    /**
     * Sets the associated appointment ID.
     *
     * @param v The appointment ID to set
     * @throws IllegalArgumentException if ID is not positive
     */
    public void setAppointmentId(long v) { validateIdPositive(v, "appointmentId"); this.appointmentId = v; }

    /**
     * Gets the patient ID.
     *
     * @return The patient ID
     */
    public long getPatientId() { return patientId; }

    /**
     * Sets the patient ID.
     *
     * @param v The patient ID to set
     * @throws IllegalArgumentException if ID is not positive
     */
    public void setPatientId(long v) { validateIdPositive(v, "patientId"); this.patientId = v; }

    /**
     * Gets the check-in status.
     *
     * @return The status (CheckedIn or Completed)
     */
    public String getStatus() { return status; }

    /**
     * Sets the check-in status without updating the timestamp.
     *
     * @param s The status to set
     * @throws IllegalArgumentException if status is null, blank, or invalid
     */
    public void setStatus(String s) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException("Status cannot be null or blank");
        String canon = canonicalStatus(s);
        if (!VALID_STATUSES.contains(canon)) {
            throw new IllegalArgumentException("Invalid status: " + s);
        }
        this.status = canon;
    }

    /**
     * Sets the status and updates the timestamp.
     * This is the preferred method to update status as it maintains audit information.
     *
     * @param s The status to set
     * @throws IllegalArgumentException if status is null, blank, or invalid
     */
    public void setStatusAndTouch(String s) { setStatus(s); touch(); }

    /**
     * Gets the desk or location where check-in occurred.
     *
     * @return The desk location or null if not specified
     */
    public String getDesk() { return desk; }

    /**
     * Sets the desk or location.
     *
     * @param d The desk location to set (null allowed)
     */
    public void setDesk(String d) { this.desk = (d == null ? null : d.trim()); }

    /**
     * Gets the check-in notes.
     *
     * @return The notes or null if not specified
     */
    public String getNotes() { return notes; }

    /**
     * Sets the check-in notes.
     *
     * @param n The notes to set (null allowed)
     */
    public void setNotes(String n) { this.notes = (n == null ? null : n.trim()); }

    /**
     * Checks if this is a walk-in check-in.
     *
     * @return true if this is a walk-in, false if it's for a scheduled appointment
     */
    public boolean isWalkIn() { return walkIn; }

    /**
     * Sets the walk-in status.
     *
     * @param walkIn true for walk-in, false for scheduled appointment
     */
    public void setWalkIn(boolean walkIn) { this.walkIn = walkIn; }

    /**
     * Gets the priority level (for walk-ins).
     * Higher values indicate higher priority.
     *
     * @return The priority level (0 or higher)
     */
    public int getPriority() { return priority; }

    /**
     * Sets the priority level without updating the timestamp.
     *
     * @param p The priority level to set (must be 0 or higher)
     * @throws IllegalArgumentException if priority is negative
     */
    public void setPriority(int p) {
        if (p < 0) throw new IllegalArgumentException("Priority must be >= 0");
        this.priority = p;
    }

    /**
     * Sets the priority level and updates the timestamp.
     * This is the preferred method to update priority as it maintains audit information.
     *
     * @param p The priority level to set (must be 0 or higher)
     * @throws IllegalArgumentException if priority is negative
     */
    public void setPriorityAndTouch(int p) { setPriority(p); touch(); }

    /**
     * Gets the timestamp when the patient checked in.
     *
     * @return The check-in timestamp in format "dd-MM-yyyy HH:mm:ss"
     */
    public String getCheckedInAt() { return checkedInAt; }

    /**
     * Sets the check-in timestamp.
     * Used primarily during data import.
     *
     * @param v The timestamp to set in format "dd-MM-yyyy HH:mm:ss"
     */
    public void setCheckedInAt(String v) { this.checkedInAt = v; }

    /**
     * Gets the timestamp when the check-in was completed.
     *
     * @return The completion timestamp in format "dd-MM-yyyy HH:mm:ss", or null if not completed
     */
    public String getCompletedAt() { return completedAt; }

    /**
     * Sets the completion timestamp.
     *
     * @param v The timestamp to set in format "dd-MM-yyyy HH:mm:ss"
     */
    public void setCompletedAt(String v) { this.completedAt = v; }

    /**
     * Returns a string representation of this check-in for debugging purposes.
     *
     * @return A string with key check-in details
     */
    @Override
    public String toString() {
        return "CheckIn{" +
                "id=" + getId() +
                ", appt=" + appointmentId +
                ", patient=" + patientId +
                ", status='" + status + '\'' +
                ", walkIn=" + walkIn +
                ", priority=" + priority +
                ", checkedInAt='" + checkedInAt + '\'' +
                '}';
    }
}
