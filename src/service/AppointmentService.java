package service;

import model.Appointment;
import repo.InMemoryStore;
import util.EntitySorter;
import util.TimeUtil;
import util.FileIO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Service class for appointment-related operations.
 * Provides business logic for managing appointments including creation, updates,
 * searches, and validation. Handles persistence through InMemoryStore and file storage.
 */
public class AppointmentService {

    private static final String APPOINTMENT_FILE = "src/data/appointment.txt"; // moved under src/data
    private final Path appointmentFilePath;
    private final InMemoryStore storage;

    // Allowed appointment statuses (canonical casing)
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            "Scheduled", "Checked In", "In Progress", "Completed", "Cancelled", "No Show"
    ));

    /**
     * Creates an appointment service with the default file path.
     *
     * @param storage The in-memory store to use
     * @throws IllegalArgumentException if storage is null
     */
    public AppointmentService(InMemoryStore storage) {
        this(storage, APPOINTMENT_FILE);
    }

    /**
     * Creates an appointment service with a custom file path.
     *
     * @param storage The in-memory store to use
     * @param appointmentFilePath The file path to store appointment data
     * @throws IllegalArgumentException if storage is null
     */
    public AppointmentService(InMemoryStore storage, String appointmentFilePath) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        this.storage = storage;
        this.appointmentFilePath = Paths.get(appointmentFilePath);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// FileIO related methods for appointments
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Loads appointments from the configured file path.
     * Should be called at startup to initialize the service.
     */
    public void loadAppointmentsFromFile() {
        List<Appointment> loaded = FileIO.loadAppointments(appointmentFilePath, storage);
        for (Appointment a : loaded) {
            storage.saveAppointment(a);
        }
    }

    /**
     * Saves all appointments to the configured file path.
     * Use this method to persist changes to appointments.
     */
    public void saveAllAppointmentsToFile() {
        FileIO.saveAppointments(new ArrayList<>(storage.allAppointments()), appointmentFilePath);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Creation and retrieval related methods for appointments
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new appointment with validation.
     *
     * @param patientId The patient ID
     * @param doctorId The doctor ID
     * @param appointmentDate The appointment date (dd-MM-yyyy)
     * @param appointmentTime The appointment time (HH:mm)
     * @param duration The appointment duration (HH:mm)
     * @param status The appointment status
     * @param location The appointment location
     * @param followUpDate The follow-up date (dd-MM-yyyy), can be null
     * @param reason The appointment reason
     * @param notes Additional notes, can be null
     * @return The newly created appointment
     * @throws IllegalArgumentException if any input is invalid or referenced entities don't exist
     */
    public Appointment createAppointment(long patientId, long doctorId,
                                         String appointmentDate, String appointmentTime, String duration,
                                         String status, String location, String followUpDate,
                                         String reason, String notes) {
        // Validate inputs
        validatePatientExists(patientId);
        validateDoctorExists(doctorId);
        validateDate(appointmentDate, "Appointment date");
        validateTime(appointmentTime, "Appointment time");
        validateDuration(duration);
        validateStatus(status);
        validateOptionalDate(followUpDate, "Follow-up date");

        // Check for delimiter characters
        checkNoDelimiter(status, "Status");
        checkNoDelimiter(location, "Location");
        checkNoDelimiter(reason, "Reason");
        checkNoDelimiter(notes, "Notes");

        // Canonicalize to stored value
        status = canonicalizeStatus(status);

        // Create appointment
        long id = storage.nextAppointmentId();
        Appointment appointment = new Appointment(id, patientId, doctorId,
                appointmentDate, appointmentTime, duration,
                status, location, followUpDate,
                reason, notes);

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Retrieves an appointment by ID.
     *
     * @param id Appointment's ID
     * @return The appointment
     * @throws IllegalArgumentException if appointment doesn't exist
     */
    public Appointment getAppointmentById(long id) {
        Appointment appointment = storage.getAppointment(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment with ID " + id + " does not exist");
        }
        return appointment;
    }

    /**
     * Checks if an appointment exists by ID.
     *
     * @param id Appointment's ID
     * @return true if the appointment exists, false otherwise
     */
    public boolean existsById(long id) {
        return storage.hasAppointment(id);
    }

    /**
     * Returns all appointments.
     *
     * @return Collection of all appointments
     */
    public Collection<Appointment> listOfAppointments() {
        return storage.allAppointments();
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Update related methods for appointments
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper method to get an existing appointment or throw an exception.
     *
     * @param id The appointment ID
     * @return The appointment if it exists
     * @throws IllegalArgumentException if the appointment doesn't exist
     */
    private Appointment getExistingAppointment(long id) {
        Appointment appointment = storage.getAppointment(id);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment with ID " + id + " does not exist");
        }
        return appointment;
    }

    /**
     * Updates an appointment's status.
     *
     * @param id Appointment's ID
     * @param status New status
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist or status is invalid
     */
    public Appointment updateStatus(long id, String status) {
        validateStatus(status);
        checkNoDelimiter(status, "Status");
        String canonical = canonicalizeStatus(status);

        Appointment appointment = getExistingAppointment(id);
        appointment.setStatusAndTouch(canonical);

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Updates an appointment's date and time.
     *
     * @param id Appointment's ID
     * @param date New date (dd-MM-yyyy)
     * @param time New time (HH:mm)
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist or date/time is invalid
     */
    public Appointment updateDateTime(long id, String date, String time) {
        validateDate(date, "Appointment date");
        validateTime(time, "Appointment time");

        Appointment appointment = getExistingAppointment(id);

        // Use no-touch setters for batch update, then touch once
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.touch();

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Updates an appointment's duration.
     *
     * @param id Appointment's ID
     * @param duration New duration (HH:mm)
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist or duration is invalid
     */
    public Appointment updateDuration(long id, String duration) {
        validateDuration(duration);

        Appointment appointment = getExistingAppointment(id);
        appointment.setDurationAndTouch(duration);

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Updates an appointment's location.
     *
     * @param id Appointment's ID
     * @param location New location
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist
     */
    public Appointment updateLocation(long id, String location) {
        checkNoDelimiter(location, "Location");

        Appointment appointment = getExistingAppointment(id);
        appointment.setLocationAndTouch(location);

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Updates an appointment's notes.
     *
     * @param id Appointment's ID
     * @param notes New notes
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist
     */
    public Appointment updateNotes(long id, String notes) {
        checkNoDelimiter(notes, "Notes");

        Appointment appointment = getExistingAppointment(id);
        appointment.setNotesAndTouch(notes);

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Updates an appointment's follow-up date.
     *
     * @param id Appointment's ID
     * @param followUpDate New follow-up date (dd-MM-yyyy), can be null
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist or date is invalid
     */
    public Appointment updateFollowUpDate(long id, String followUpDate) {
        validateOptionalDate(followUpDate, "Follow-up date");

        Appointment appointment = getExistingAppointment(id);
        appointment.setFollowUpDateAndTouch(followUpDate);

        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /**
     * Updates multiple appointment fields at once.
     * This is more efficient than calling individual update methods.
     *
     * @param id Appointment's ID
     * @param date New date (dd-MM-yyyy), null to keep current
     * @param time New time (HH:mm), null to keep current
     * @param duration New duration (HH:mm), null to keep current
     * @param status New status, null to keep current
     * @param location New location, null to keep current
     * @param followUpDate New follow-up date (dd-MM-yyyy), null to keep current
     * @param reason New reason, null to keep current
     * @param notes New notes, null to keep current
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist or any value is invalid
     */
    public Appointment updateAppointment(long id, String date, String time, String duration,
                                         String status, String location, String followUpDate,
                                         String reason, String notes) {
        Appointment appointment = getExistingAppointment(id);
        boolean changed = false;

        // Validate and set each field if provided
        if (date != null) {
            validateDate(date, "Appointment date");
            appointment.setAppointmentDate(date);
            changed = true;
        }

        if (time != null) {
            validateTime(time, "Appointment time");
            appointment.setAppointmentTime(time);
            changed = true;
        }

        if (duration != null) {
            validateDuration(duration);
            appointment.setDuration(duration);
            changed = true;
        }

        if (status != null) {
            validateStatus(status);
            checkNoDelimiter(status, "Status");
            appointment.setStatus(canonicalizeStatus(status));
            changed = true;
        }

        if (location != null) {
            checkNoDelimiter(location, "Location");
            appointment.setLocation(location);
            changed = true;
        }

        if (followUpDate != null) {
            validateOptionalDate(followUpDate, "Follow-up date");
            appointment.setFollowUpDate(followUpDate);
            changed = true;
        }

        if (reason != null) {
            checkNoDelimiter(reason, "Reason");
            appointment.setReason(reason);
            changed = true;
        }

        if (notes != null) {
            checkNoDelimiter(notes, "Notes");
            appointment.setNotes(notes);
            changed = true;
        }

        // Only touch and save if something changed
        if (changed) {
            appointment.touch();
            storage.saveAppointment(appointment);
            FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        }

        return appointment;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Deletion related methods for appointments
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Deletes an appointment by ID.
     *
     * @param id Appointment's ID
     * @return true if the appointment was deleted, false if it didn't exist
     */
    public boolean deleteAppointment(long id) {
        boolean removed = storage.removeAppointment(id);
        if (removed) {
            FileIO.deleteAppointmentLine(id, appointmentFilePath);
        }
        return removed;
    }

    /**
     * Cancels an appointment (sets status to "Cancelled").
     *
     * @param id Appointment's ID
     * @return The updated appointment
     * @throws IllegalArgumentException if appointment doesn't exist
     */
    public Appointment cancelAppointment(long id) {
        Appointment appointment = getExistingAppointment(id);
        appointment.setStatusAndTouch("Cancelled");
        storage.saveAppointment(appointment);
        FileIO.upsertAppointmentLine(appointment, appointmentFilePath);
        return appointment;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Query methods for appointments
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private ArrayList<Appointment> snapshotAll() {
        return new ArrayList<>(storage.allAppointments());
    }

    /**
     * Finds appointments for a specific patient.
     *
     * @param patientId The patient ID
     * @return List of appointments for the patient
     * @throws IllegalArgumentException if patient ID is invalid
     */
    public List<Appointment> findByPatientId(long patientId) {
        validatePatientExists(patientId);
        return storage.getAppointmentsOfSomePatient(patientId);
    }

    /**
     * Finds appointments for a specific doctor.
     *
     * @param doctorId The doctor ID
     * @return List of appointments for the doctor
     * @throws IllegalArgumentException if doctor ID is invalid
     */
    public List<Appointment> findByDoctorId(long doctorId) {
        validateDoctorExists(doctorId);
        // Use indexed lookup from InMemoryStore (doctor -> appointmentIds) for O(k) retrieval
        return storage.getAppointmentsOfSomeDoctor(doctorId);
    }

    /**
     * Finds appointments by status.
     *
     * @param status The status to search for
     * @return List of matching appointments
     */
    public List<Appointment> findByStatus(String status) {
        validateStatus(status);
        String target = canonicalizeStatus(status);
        List<Appointment> appointments = snapshotAll();
        List<Appointment> result = new ArrayList<>();

        for (Appointment a : appointments) {
            if (target.equalsIgnoreCase(a.getStatus())) {
                result.add(a);
            }
        }

        return result;
    }

    /**
     * Finds appointments for a date range.
     *
     * @param startDate Start date (inclusive, dd-MM-yyyy)
     * @param endDate End date (inclusive, dd-MM-yyyy)
     * @return List of appointments in the date range
     * @throws IllegalArgumentException if dates are invalid
     */
    public List<Appointment> findByDateRange(String startDate, String endDate) {
        validateDate(startDate, "Start date");
        validateDate(endDate, "End date");

        LocalDateTime start = TimeUtil.parseDate(startDate.trim()).atStartOfDay();
        LocalDateTime end = TimeUtil.parseDate(endDate.trim()).atTime(23, 59, 59);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        List<Appointment> appointments = snapshotAll();
        List<Appointment> result = new ArrayList<>();

        for (Appointment a : appointments) {
            try {
                LocalDateTime appointmentDate = TimeUtil.parseDate(a.getAppointmentDate()).atStartOfDay();
                if (!appointmentDate.isBefore(start) && !appointmentDate.isAfter(end)) {
                    result.add(a);
                }
            } catch (Exception e) {
                // Skip appointments with invalid dates
                System.err.println("Warning: Appointment ID " + a.getId() +
                                   " has invalid date format: " + a.getAppointmentDate());
            }
        }

        return result;
    }

    /**
     * Finds appointments created in a specific date/time range.
     *
     * @param startDateTime Start date/time (inclusive, dd-MM-yyyy HH:mm:ss)
     * @param endDateTime End date/time (inclusive, dd-MM-yyyy HH:mm:ss)
     * @return List of appointments created in the date/time range
     * @throws IllegalArgumentException if date/times are invalid
     */
    public List<Appointment> findByCreatedAtRange(String startDateTime, String endDateTime) {
        if (startDateTime == null || startDateTime.trim().isEmpty() ||
            endDateTime == null || endDateTime.trim().isEmpty()) {
            throw new IllegalArgumentException("Start and end date/times cannot be null or blank");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = TimeUtil.parseDateTime(startDateTime.trim());
            end = TimeUtil.parseDateTime(endDateTime.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date format: " + e.getMessage(), e);
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date/time cannot be before start date/time");
        }

        List<Appointment> appointments = snapshotAll();
        List<Appointment> result = new ArrayList<>();

        for (Appointment a : appointments) {
            String createdAt = a.getCreatedAt();
            if (createdAt == null || createdAt.isBlank()) continue;

            try {
                LocalDateTime created = TimeUtil.parseDateTime(createdAt.trim());
                if (!created.isBefore(start) && !created.isAfter(end)) {
                    result.add(a);
                }
            } catch (IllegalArgumentException e) {
                // Skip appointments with unparseable dates
                System.err.println("Warning: Appointment ID " + a.getId() +
                                   " has invalid createdAt format: " + createdAt);
            }
        }

        return result;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Validation methods
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void validatePatientExists(long patientId) {
        if (patientId <= 0) {
            throw new IllegalArgumentException("Patient ID must be positive");
        }
        if (!storage.hasPatient(patientId)) {
            throw new IllegalArgumentException("Patient with ID " + patientId + " does not exist");
        }
    }

    private void validateDoctorExists(long doctorId) {
        if (doctorId <= 0) {
            throw new IllegalArgumentException("Doctor ID must be positive");
        }
        if (!storage.hasDoctor(doctorId)) {
            throw new IllegalArgumentException("Doctor with ID " + doctorId + " does not exist");
        }
    }

    private void validateDate(String date, String fieldName) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        try {
            TimeUtil.parseDate(date.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName.toLowerCase() + " format: " + e.getMessage(), e);
        }
    }

    private void validateOptionalDate(String date, String fieldName) {
        if (date == null || date.trim().isEmpty()) {
            return; // Optional date can be null/empty
        }
        try {
            TimeUtil.parseDate(date.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName.toLowerCase() + " format: " + e.getMessage(), e);
        }
    }

    private void validateTime(String time, String fieldName) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        try {
            TimeUtil.parseTime(time.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName.toLowerCase() + " format: " + e.getMessage(), e);
        }
    }

    private void validateDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration cannot be null or blank");
        }
        try {
            int minutes = TimeUtil.parseDurationToMinutes(duration.trim());
            if (minutes <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid duration format: " + e.getMessage(), e);
        }
    }

    private void validateStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or blank");
        }
        String canon = canonicalizeStatus(status);
        if (!ALLOWED_STATUSES.contains(canon)) {
            throw new IllegalArgumentException("Invalid status. Allowed: " + ALLOWED_STATUSES);
        }
    }

    private void checkNoDelimiter(String value, String fieldName) {
        if (value != null && value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(fieldName + " cannot contain the '|' character");
        }
    }

    private String canonicalizeStatus(String status) {
        String s = status == null ? null : status.trim();
        if (s == null) return null;
        for (String allowed : ALLOWED_STATUSES) {
            if (allowed.equalsIgnoreCase(s)) return allowed; // unify casing/spaces
        }
        return s; // not expected to reach here if validateStatus is used
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Sort related methods for appointments
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public enum AppointmentSortField {
        ID, PATIENT_ID, DOCTOR_ID, START_DATE_TIME, DURATION_MIN, STATUS, CREATED_AT, UPDATED_AT
    }

    public List<Appointment> sort(List<Appointment> list, AppointmentSortField field, EntitySorter.Direction dir) {
        if (list == null || list.size() < 2 || field == null) return list;

        switch (field) {
            case ID:            return EntitySorter.byId(list, dir);
            case CREATED_AT:    return EntitySorter.byCreatedAt(list, dir);
            case UPDATED_AT:    return EntitySorter.byUpdatedAt(list, dir);
            default: break;
        }

        Comparator<Appointment> cmp;
        switch (field) {
            case PATIENT_ID:
                cmp = Comparator.comparingLong(Appointment::getPatientId); break;
            case DOCTOR_ID:
                cmp = Comparator.comparingLong(Appointment::getDoctorId); break;
            case START_DATE_TIME:
                cmp = Comparator.comparing(a -> safeStart(a, dir == EntitySorter.Direction.ASC)); break;
            case DURATION_MIN:
                cmp = Comparator.comparingInt(this::safeDurationMinutes); break;
            case STATUS:
                cmp = Comparator.comparing(a -> safeLower(a.getStatus())); break;
            default:
                return list;
        }
        if (dir == EntitySorter.Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    public List<Appointment> listAllSorted(AppointmentSortField field, EntitySorter.Direction dir) {
        return sort(snapshotAll(), field, dir);
    }

    // Convenience: filter then sort methods
    public List<Appointment> findByPatientIdSorted(long patientId,
                                                  AppointmentSortField field,
                                                  EntitySorter.Direction dir) {
        return sort(findByPatientId(patientId), field, dir);
    }

    public List<Appointment> findByDoctorIdSorted(long doctorId,
                                                 AppointmentSortField field,
                                                 EntitySorter.Direction dir) {
        return sort(findByDoctorId(doctorId), field, dir);
    }

    public List<Appointment> findByStatusSorted(String status,
                                               AppointmentSortField field,
                                               EntitySorter.Direction dir) {
        return sort(findByStatus(status), field, dir);
    }

    // ---------------- Helpers ----------------
    private LocalDateTime safeStart(Appointment a, boolean asc) {
        try {
            return TimeUtil.combine(a.getAppointmentDate(), a.getAppointmentTime());
        } catch (Exception e) {
            return asc ? LocalDateTime.MAX : LocalDateTime.MIN;
        }
    }

    private int safeDurationMinutes(Appointment a) {
        try {
            return TimeUtil.parseDurationToMinutes(a.getDuration());
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
