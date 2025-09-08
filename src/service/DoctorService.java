package service;

import model.Appointment;
import model.Doctor;
import repo.InMemoryStore;
import util.EntitySorter;
import util.PersonSorter;
import util.TimeUtil;
import util.FileIO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for doctor-related operations.
 * Provides business logic and validation for doctor management.
 */
public class DoctorService {

    private static final String DOCTOR_FILE = "src/data/doctor.txt";
    private static final Path APPOINTMENT_FILE_PATH = Paths.get("src/data/appointment.txt");
    private static final Logger LOGGER = Logger.getLogger(DoctorService.class.getName());

    private final Path doctorFilePath;
    private final InMemoryStore storage;

    // Constructor with default file path
    public DoctorService(InMemoryStore storage) {
        this(storage, DOCTOR_FILE);
    }

    // Constructor with configurable file path
    public DoctorService(InMemoryStore storage, String doctorFilePath) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        this.storage = storage;
        this.doctorFilePath = Paths.get(doctorFilePath);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// FileIO related methods for a doctor
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Load doctors from file (call at startup)
    public void loadDoctorsFromFile() {
        List<Doctor> loaded = FileIO.loadDoctors(doctorFilePath, storage);
        for (Doctor d : loaded) {
            storage.saveDoctor(d);
        }
    }

    // Save all doctors to file
    public void saveAllDoctorsToFile() {
        FileIO.saveDoctors(new ArrayList<>(storage.allDoctors()), doctorFilePath);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Creation and retrieval related methods for a doctor
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Creates a new doctor with validation.
     *
     * @param firstName Doctor's first name
     * @param lastName Doctor's last name
     * @param specialty Doctor's specialty
     * @param phoneNumber Doctor's phone number
     * @return The newly created doctor
     * @throws IllegalArgumentException if any input is invalid
     */
    public Doctor createDoctor(String firstName, String lastName, String specialty, String phoneNumber) {
        // Validate inputs
        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        validateSpecialty(specialty);
        validatePhoneNumber(phoneNumber);

        // Check for delimiter characters
        checkNoDelimiter(firstName, "First name");
        checkNoDelimiter(lastName, "Last name");
        checkNoDelimiter(specialty, "Specialty");
        checkNoDelimiter(phoneNumber, "Phone number");

        // Trim inputs
        firstName = firstName.trim();
        lastName = lastName.trim();
        specialty = specialty.trim();
        phoneNumber = phoneNumber.trim();

        // Create doctor
        long id = storage.nextDoctorId();
        Doctor doctor = new Doctor(firstName, lastName, id, phoneNumber, specialty);
        storage.saveDoctor(doctor);
        FileIO.upsertDoctorLine(doctor, doctorFilePath);
        return doctor;
    }

    /**
     * Retrieves a doctor by ID.
     *
     * @param id Doctor's ID
     * @return The doctor
     * @throws IllegalArgumentException if doctor doesn't exist
     */
    public Doctor getDoctorById(long id) {
        Doctor doctor = storage.getDoctor(id);
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor with ID " + id + " does not exist");
        }
        return doctor;
    }

    /**
     * Checks if a doctor exists by ID.
     *
     * @param id Doctor's ID
     * @return true if the doctor exists, false otherwise
     */
    public boolean existsById(long id) {
        return storage.hasDoctor(id);
    }

    /**
     * Returns all doctors.
     *
     * @return Collection of all doctors
     */
    public Collection<Doctor> listOfDoctors() {
        return storage.allDoctors();
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Update related methods for a doctor
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper method to get an existing doctor or throw an exception.
     *
     * @param id The doctor ID
     * @return The doctor if it exists
     * @throws IllegalArgumentException if the doctor doesn't exist
     */
    private Doctor getExistingDoctor(long id) {
        Doctor doctor = storage.getDoctor(id);
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor with ID " + id + " does not exist");
        }
        return doctor;
    }

    /**
     * Updates a doctor's name with validation.
     *
     * @param id Doctor's ID
     * @param firstName New first name
     * @param lastName New last name
     * @return The updated doctor
     * @throws IllegalArgumentException if doctor doesn't exist or names are invalid
     */
    public Doctor updateName(long id, String firstName, String lastName) {
        // Validate inputs
        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        checkNoDelimiter(firstName, "First name");
        checkNoDelimiter(lastName, "Last name");

        // Get existing doctor
        Doctor doctor = getExistingDoctor(id);

        // Update and save - use AndTouch variants for single field updates
        doctor.setFirstNameAndTouch(firstName);
        doctor.setLastNameAndTouch(lastName);
        storage.saveDoctor(doctor);
        FileIO.upsertDoctorLine(doctor, doctorFilePath);
        return doctor;
    }

    /**
     * Updates a doctor's specialty with validation.
     *
     * @param id Doctor's ID
     * @param specialty New specialty
     * @return The updated doctor
     * @throws IllegalArgumentException if doctor doesn't exist or specialty is invalid
     */
    public Doctor updateSpecialty(long id, String specialty) {
        // Validate input
        validateSpecialty(specialty);
        checkNoDelimiter(specialty, "Specialty");

        // Get existing doctor
        Doctor doctor = getExistingDoctor(id);

        // Update and save - use AndTouch variant for single field update
        doctor.setSpecialtyAndTouch(specialty);
        storage.saveDoctor(doctor);
        FileIO.upsertDoctorLine(doctor, doctorFilePath);
        return doctor;
    }

    /**
     * Updates a doctor's phone number with validation.
     *
     * @param id Doctor's ID
     * @param phoneNumber New phone number
     * @return The updated doctor
     * @throws IllegalArgumentException if doctor doesn't exist or phone number is invalid
     */
    public Doctor updatePhone(long id, String phoneNumber) {
        // Validate input
        validatePhoneNumber(phoneNumber);
        checkNoDelimiter(phoneNumber, "Phone number");

        // Get existing doctor
        Doctor doctor = getExistingDoctor(id);

        // Update and save - use AndTouch variant for single field update
        doctor.setPhoneNumberAndTouch(phoneNumber);
        storage.saveDoctor(doctor);
        FileIO.upsertDoctorLine(doctor, doctorFilePath);
        return doctor;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Deletion related methods for a doctor
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Deletes a doctor if they have no appointments.
     * Note: This method assumes an isReferenced() method exists in the storage
     * to check if any appointments reference this doctor.
     *
     * @param id Doctor's ID
     * @return true if the doctor was deleted, false otherwise
     * @throws IllegalStateException if the doctor has appointments
     */
    public boolean deleteDoctor(long id) {
        Doctor doctor = storage.getDoctor(id);
        if (doctor == null) {
            return false;
        }
        if (storage.doctorHasAppointments(id)) { // use indexed lookup
            throw new IllegalStateException("Cannot delete doctor with existing appointments. Cancel them first.");
        }
        boolean removed = storage.removeDoctor(id);
        if (removed) FileIO.deleteDoctorLine(id, doctorFilePath);
        return removed;
    }

    // Indexed check (replaces linear scan)
    private boolean hasAppointments(long doctorId) {
        return storage.doctorHasAppointments(doctorId);
    }

    // Symmetry with PatientService: count and list appointments for a doctor
    public int appointmentCount(long doctorId) {
        return storage.getAppointmentsOfSomeDoctor(doctorId).size();
    }

    public List<Appointment> listAppointments(long doctorId) {
        List<Appointment> allAppointments = storage.getAppointmentsOfSomeDoctor(doctorId);
        // Filter out completed appointments
        List<Appointment> activeAppointments = new ArrayList<>();
        for (Appointment a : allAppointments) {
            String status = a.getStatus();
            // Only include appointments that are not Completed, No Show, or Checked In (active appointments)
            if (status == null || (!status.equalsIgnoreCase("Completed")
                    && !status.equalsIgnoreCase("No Show")
                    && !status.equalsIgnoreCase("Checked In"))) {
                activeAppointments.add(a);
            }
        }
        return activeAppointments;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Categorizing Attribute Categories
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Make a modifiable copy of all doctors (base for all finders)
    private ArrayList<Doctor> snapshotAll() {
        return new ArrayList<>(storage.allDoctors());
    }

    // 1) All doctors (UNSORTED)
    public List<Doctor> listAllUnsorted() {
        return snapshotAll();
    }

    // 2) Last name starts-with (case-insensitive), UNSORTED
    public List<Doctor> findByLastNamePrefix(String prefix) {
        if (prefix == null || (prefix = prefix.trim()).isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }
        String pfx = prefix.toLowerCase(Locale.ROOT);
        List<Doctor> doctors = snapshotAll();
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) {
            String ln = d.getLastName();
            if (ln != null && ln.trim().toLowerCase(Locale.ROOT).startsWith(pfx)) {
                result.add(d);
            }
        }
        return result;
    }

    // 3) First name starts-with (case-insensitive), UNSORTED
    public List<Doctor> findByFirstNamePrefix(String prefix) {
        if (prefix == null || (prefix = prefix.trim()).isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }
        String pfx = prefix.toLowerCase(Locale.ROOT);
        List<Doctor> doctors = snapshotAll();
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) {
            String fn = d.getFirstName();
            if (fn != null && fn.trim().toLowerCase(Locale.ROOT).startsWith(pfx)) {
                result.add(d);
            }
        }
        return result;
    }

    // 4) Phone exact (duplicates allowed), UNSORTED
    public List<Doctor> findByPhone(String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        String phone = phoneNumber.trim();
        List<Doctor> doctors = snapshotAll();
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) {
            String ph = d.getPhoneNumber();
            if (ph != null && ph.equals(phone)) {
                result.add(d);
            }
        }
        return result;
    }

    // 5) Specialty contains (case-insensitive), UNSORTED
    public List<Doctor> findBySpecialty(String fragment) {
        if (fragment == null || (fragment = fragment.trim()).isEmpty()) {
            return snapshotAll();
        }
        String needle = fragment.toLowerCase(Locale.ROOT);
        List<Doctor> doctors = snapshotAll();
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) {
            String sp = d.getSpecialty();
            if (sp != null && sp.toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(d);
            }
        }
        return result;
    }

    // 6) Created-at BETWEEN [start..end] inclusive, UNSORTED
    // expects "dd-MM-yyyy HH:mm:ss" (TimeUtil also accepts without seconds)
    public List<Doctor> filterByCreatedAtRange(String start, String end) {
        if (start == null || start.trim().isEmpty() || end == null || end.trim().isEmpty()) {
            throw new IllegalArgumentException("Start and end dates cannot be null or blank");
        }

        LocalDateTime from;
        LocalDateTime to;
        try {
            from = TimeUtil.parseDateTime(start.trim());
            to = TimeUtil.parseDateTime(end.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date format: " + e.getMessage(), e);
        }

        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<Doctor> doctors = snapshotAll();
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) {
            String c = d.getCreatedAt();
            if (c == null || c.isBlank()) continue;

            try {
                var created = TimeUtil.parseDateTime(c.trim());
                if (!created.isBefore(from) && !created.isAfter(to)) { // inclusive
                    result.add(d);
                }
            } catch (IllegalArgumentException e) {
                // Skip doctors with unparseable dates instead of failing the entire search
                LOGGER.log(Level.WARNING, "Doctor ID " + d.getId() +
                                  " has invalid createdAt date format: " + c, e);
                continue;
            }
        }
        return result;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Validation methods
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Validates that a name is not null or blank.
     *
     * @param name The name to validate
     * @param fieldName Field name for error messages
     * @throws IllegalArgumentException if name is null or blank
     */
    private void validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }

    /**
     * Validates that a specialty is not null or blank.
     *
     * @param specialty The specialty to validate
     * @throws IllegalArgumentException if specialty is null or blank
     */
    private void validateSpecialty(String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
            throw new IllegalArgumentException("Specialty cannot be null or blank");
        }
    }

    /**
     * Validates that a phone number contains only digits and is of appropriate length.
     *
     * @param phoneNumber The phone number to validate
     * @throws IllegalArgumentException if phone number is invalid
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }

        String trimmed = phoneNumber.trim();
        if (!trimmed.matches("\\d{7,15}")) {
            throw new IllegalArgumentException("Phone number must contain only digits and be 7-15 characters long");
        }
    }

    /**
     * Checks that a field doesn't contain the delimiter character.
     *
     * @param value The value to check
     * @param fieldName Field name for error messages
     * @throws IllegalArgumentException if value contains the delimiter
     */
    private void checkNoDelimiter(String value, String fieldName) {
        if (value != null && value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(fieldName + " cannot contain the '|' character");
        }
    }

    public enum DoctorSortField {
        ID, FIRST_NAME, LAST_NAME, FULL_NAME, SPECIALTY, PHONE, CREATED_AT, UPDATED_AT
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Sort related methods for a doctor
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sorts (in-place) the provided doctor list by the selected field (one criterion only).
     */
    public List<Doctor> sort(List<Doctor> doctors, DoctorSortField field, EntitySorter.Direction dir) {
        if (doctors == null || doctors.size() < 2 || field == null) return doctors;
        switch (field) {
            case ID:         return EntitySorter.byId(doctors, dir);
            case FIRST_NAME: return PersonSorter.byFirstName(doctors, dir);
            case LAST_NAME:  return PersonSorter.byLastName(doctors, dir);
            case FULL_NAME:  return PersonSorter.byFullName(doctors, dir);
            case SPECIALTY:  return PersonSorter.bySpecialty(doctors, dir);
            case PHONE:      return PersonSorter.byPhone(doctors, dir);
            case CREATED_AT: return EntitySorter.byCreatedAt(doctors, dir);
            case UPDATED_AT: return EntitySorter.byUpdatedAt(doctors, dir);
            default:         return doctors;
        }
    }

    // Convenience: filter then sort example
    public List<Doctor> findByFirstNamePrefixSorted(String prefix, DoctorSortField field, EntitySorter.Direction dir) {
        return sort(findByFirstNamePrefix(prefix), field, dir);
    }

    // Convenience: filter by specialty then sort
    public List<Doctor> findBySpecialtySorted(String fragment, DoctorSortField field, EntitySorter.Direction dir) {
        return sort(findBySpecialty(fragment), field, dir);
    }

    // Added: method to cancel and remove all appointments for a doctor
    public int cancelAllAppointmentsOfSomeDoctor(long doctorId) {
        List<Appointment> list = new ArrayList<>(storage.getAppointmentsOfSomeDoctor(doctorId));
        int removed = 0;
        for (Appointment a : list) {
            if (storage.removeAppointment(a.getId())) {
                // Persist removal to file (prevent phantom appointments after restart)
                FileIO.deleteAppointmentLine(a.getId(), APPOINTMENT_FILE_PATH);
                removed++;
            } else {
                LOGGER.log(Level.WARNING, "Failed to remove appointment with ID: " + a.getId());
            }
        }
        return removed;
    }
}
