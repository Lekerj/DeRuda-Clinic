package model;

import util.TimeUtil;

/**
 * Represents a medical appointment in the clinic system.
 * An appointment connects a patient with a doctor at a specific date/time,
 * and tracks various attributes related to the appointment.
 */
public class Appointment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private long patientId;
    private long doctorId;
    private String location;
    private String status; // e.g., "Scheduled", "Completed", "Cancelled", "No Show" , "Checked In", "In Progress"

    private String appointmentDate; // dd-MM-yyyy
    private String followUpDate;    // dd-MM-yyyy (may be null/blank)

    private String appointmentTime; // HH:mm
    private String duration;        // HH:mm

    private String reason;
    private String notes;

    /**
     * Creates a new appointment with the specified details and current timestamp.
     *
     * @param id The appointment ID
     * @param patientId The patient ID
     * @param doctorId The doctor ID
     * @param appointmentDate The appointment date (dd-MM-yyyy)
     * @param appointmentTime The appointment time (HH:mm)
     * @param duration The appointment duration (HH:mm)
     * @param status The appointment status
     * @param location The appointment location
     * @param followUpDate The follow-up date (dd-MM-yyyy)
     * @param reason The appointment reason
     * @param notes Additional notes
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Appointment(long id, long patientId, long doctorId,
                      String appointmentDate, String appointmentTime, String duration,
                      String status, String location, String followUpDate,
                      String reason, String notes) {
        super(id);
        setPatientId(patientId);
        setDoctorId(doctorId);
        setAppointmentDate(appointmentDate);
        setAppointmentTime(appointmentTime);
        setDuration(duration);
        setStatus(status);
        setLocation(location);
        setFollowUpDate(followUpDate);
        setReason(reason);
        setNotes(notes);
        touch(); // Single touch after all fields are set
    }

    /**
     * Creates an appointment with the specified details and timestamps.
     * Used primarily for importing data.
     *
     * @param id The appointment ID
     * @param patientId The patient ID
     * @param doctorId The doctor ID
     * @param appointmentDate The appointment date (dd-MM-yyyy)
     * @param appointmentTime The appointment time (HH:mm)
     * @param duration The appointment duration (HH:mm)
     * @param status The appointment status
     * @param location The appointment location
     * @param followUpDate The follow-up date (dd-MM-yyyy)
     * @param reason The appointment reason
     * @param notes Additional notes
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Appointment(long id, long patientId, long doctorId,
                      String appointmentDate, String appointmentTime, String duration,
                      String status, String location, String followUpDate,
                      String reason, String notes, String createdAt, String updatedAt) {
        super(id, createdAt, updatedAt);
        setPatientId(patientId);
        setDoctorId(doctorId);
        setAppointmentDate(appointmentDate);
        setAppointmentTime(appointmentTime);
        setDuration(duration);
        setStatus(status);
        setLocation(location);
        setFollowUpDate(followUpDate);
        setReason(reason);
        setNotes(notes);
        // Do NOT call touch() or re-set created/updated here to preserve imported audit stamps
    }

    /**
     * Gets the patient ID.
     *
     * @return The patient ID
     */
    public long getPatientId() {
        return patientId;
    }

    /**
     * Sets the patient ID without updating the timestamp.
     *
     * @param patientId The patient ID to set
     * @throws IllegalArgumentException if the ID is invalid
     */
    public void setPatientId(long patientId) {
        if (patientId <= 0) {
            throw new IllegalArgumentException("Patient ID must be positive, got: " + patientId);
        }
        this.patientId = patientId;
    }

    /**
     * Sets the patient ID and updates the timestamp.
     *
     * @param patientId The patient ID to set
     * @throws IllegalArgumentException if the ID is invalid
     */
    public void setPatientIdAndTouch(long patientId) {
        setPatientId(patientId);
        touch();
    }

    /**
     * Gets the doctor ID.
     *
     * @return The doctor ID
     */
    public long getDoctorId() {
        return doctorId;
    }

    /**
     * Sets the doctor ID without updating the timestamp.
     *
     * @param doctorId The doctor ID to set
     * @throws IllegalArgumentException if the ID is invalid
     */
    public void setDoctorId(long doctorId) {
        if (doctorId <= 0) {
            throw new IllegalArgumentException("Doctor ID must be positive, got: " + doctorId);
        }
        this.doctorId = doctorId;
    }

    /**
     * Sets the doctor ID and updates the timestamp.
     *
     * @param doctorId The doctor ID to set
     * @throws IllegalArgumentException if the ID is invalid
     */
    public void setDoctorIdAndTouch(long doctorId) {
        setDoctorId(doctorId);
        touch();
    }

    /**
     * Gets the appointment date.
     *
     * @return The appointment date (dd-MM-yyyy)
     */
    public String getAppointmentDate() {
        return appointmentDate;
    }

    /**
     * Sets the appointment date without updating the timestamp.
     *
     * @param appointmentDate The appointment date to set
     * @throws IllegalArgumentException if the date is invalid
     */
    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = TimeUtil.normalizeDate(appointmentDate);
    }

    /**
     * Sets the appointment date and updates the timestamp.
     *
     * @param appointmentDate The appointment date to set
     * @throws IllegalArgumentException if the date is invalid
     */
    public void setAppointmentDateAndTouch(String appointmentDate) {
        setAppointmentDate(appointmentDate);
        touch();
    }

    /**
     * Gets the appointment time.
     *
     * @return The appointment time (HH:mm)
     */
    public String getAppointmentTime() {
        return appointmentTime;
    }

    /**
     * Sets the appointment time without updating the timestamp.
     * Accepts HH:mm or HH:mm:ss format but stores only HH:mm.
     *
     * @param appointmentTime The appointment time to set
     * @throws IllegalArgumentException if the time is invalid
     */
    public void setAppointmentTime(String appointmentTime) {
        String norm = TimeUtil.normalizeTime(appointmentTime);
        if (norm.length() == 8) { // HH:mm:ss format
            norm = norm.substring(0, 5); // drop :ss
        }
        this.appointmentTime = norm;
    }

    /**
     * Sets the appointment time and updates the timestamp.
     * Accepts HH:mm or HH:mm:ss format but stores only HH:mm.
     *
     * @param appointmentTime The appointment time to set
     * @throws IllegalArgumentException if the time is invalid
     */
    public void setAppointmentTimeAndTouch(String appointmentTime) {
        setAppointmentTime(appointmentTime);
        touch();
    }

    /**
     * Gets the appointment duration.
     *
     * @return The duration (HH:mm)
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Sets the appointment duration without updating the timestamp.
     *
     * @param duration The duration to set in HH:mm format
     * @throws IllegalArgumentException if the duration is invalid
     */
    public void setDuration(String duration) {
        int mins = TimeUtil.parseDurationToMinutes(duration); // validates HH:mm
        this.duration = TimeUtil.formatDurationFromMinutes(mins); // canonical HH:mm
    }

    /**
     * Sets the appointment duration and updates the timestamp.
     *
     * @param duration The duration to set in HH:mm format
     * @throws IllegalArgumentException if the duration is invalid
     */
    public void setDurationAndTouch(String duration) {
        setDuration(duration);
        touch();
    }

    /**
     * Gets the appointment status.
     *
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the appointment status without updating the timestamp.
     *
     * @param status The status to set
     * @throws IllegalArgumentException if the status is invalid
     */
    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }
        this.status = status.trim();
    }

    /**
     * Sets the appointment status and updates the timestamp.
     *
     * @param status The status to set
     * @throws IllegalArgumentException if the status is invalid
     */
    public void setStatusAndTouch(String status) {
        setStatus(status);
        touch();
    }

    /**
     * Gets the appointment location.
     *
     * @return The location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the appointment location without updating the timestamp.
     *
     * @param location The location to set
     */
    public void setLocation(String location) {
        this.location = (location == null ? null : location.trim());
    }

    /**
     * Sets the appointment location and updates the timestamp.
     *
     * @param location The location to set
     */
    public void setLocationAndTouch(String location) {
        setLocation(location);
        touch();
    }

    /**
     * Gets the follow-up date.
     *
     * @return The follow-up date (dd-MM-yyyy)
     */
    public String getFollowUpDate() {
        return followUpDate;
    }

    /**
     * Sets the follow-up date without updating the timestamp.
     * Can be null or blank.
     *
     * @param followUpDate The follow-up date to set
     * @throws IllegalArgumentException if the date is invalid
     */
    public void setFollowUpDate(String followUpDate) {
        if (followUpDate == null || followUpDate.trim().isEmpty()) {
            this.followUpDate = null;
        } else {
            this.followUpDate = TimeUtil.normalizeDate(followUpDate);
        }
    }

    /**
     * Sets the follow-up date and updates the timestamp.
     * Can be null or blank.
     *
     * @param followUpDate The follow-up date to set
     * @throws IllegalArgumentException if the date is invalid
     */
    public void setFollowUpDateAndTouch(String followUpDate) {
        setFollowUpDate(followUpDate);
        touch();
    }

    /**
     * Gets the appointment reason.
     *
     * @return The reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the appointment reason without updating the timestamp.
     *
     * @param reason The reason to set
     */
    public void setReason(String reason) {
        this.reason = (reason == null ? null : reason.trim());
    }

    /**
     * Sets the appointment reason and updates the timestamp.
     *
     * @param reason The reason to set
     */
    public void setReasonAndTouch(String reason) {
        setReason(reason);
        touch();
    }

    /**
     * Gets the appointment notes.
     *
     * @return The notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the appointment notes without updating the timestamp.
     *
     * @param notes The notes to set
     */
    public void setNotes(String notes) {
        this.notes = (notes == null ? null : notes.trim());
    }

    /**
     * Sets the appointment notes and updates the timestamp.
     *
     * @param notes The notes to set
     */
    public void setNotesAndTouch(String notes) {
        setNotes(notes);
        touch();
    }

    @Override
    public String toString() {
        // Matches FileIO.APPOINTMENT_FORMAT (serializeAppointment)
        String followUp = getFollowUpDate();
        String loc = getLocation();
        String reasonVal = getReason();
        String notesVal = getNotes();
        return getId() + "|" +
               getPatientId() + "|" +
               getDoctorId() + "|" +
               getAppointmentDate() + "|" +
               getAppointmentTime() + "|" +
               getDuration() + "|" +
               getStatus() + "|" +
               (loc == null ? "" : loc) + "|" +
               (followUp == null ? "" : followUp) + "|" +
               (reasonVal == null ? "" : reasonVal) + "|" +
               (notesVal == null ? "" : notesVal) + "|" +
               getUpdatedAt() + "|" +
               getCreatedAt();
    }
}
