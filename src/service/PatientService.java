package service;

import model.Appointment;
import model.Patient;
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

/**
 * Service class for patient-related operations.
 * Provides business logic and validation for patient management.
 */
public class PatientService {

    private static final String PATIENT_FILE = "src/data/patients.txt";
    // Added: path to appointments file for cascade deletions (now under src/data)
    private static final Path APPOINTMENT_FILE_PATH = Paths.get("src/data/appointment.txt");
    private final Path patientFilePath;
    private final InMemoryStore storage;

    // Constructor with default file path
    public PatientService(InMemoryStore storage) {
        this(storage, PATIENT_FILE);
    }

    // Constructor with configurable file path
    public PatientService(InMemoryStore storage, String patientFilePath) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        this.storage = storage;
        this.patientFilePath = Paths.get(patientFilePath);
    }
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// FileIO related methods for a patient
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Load patients from file (call at startup)
    public void loadPatientsFromFile() {
        List<Patient> loaded = FileIO.loadPatients(patientFilePath, storage);
        for (Patient p : loaded) {
            storage.savePatient(p);
        }
    }

    // Save all patients to file
    public void saveAllPatientsToFile() {
        FileIO.savePatients(new ArrayList<>(storage.allPatients()), patientFilePath);
    }

/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Creation and retrieval related methods for a patient
/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Creates a new patient with validation.
     *
     * @param firstName Patient's first name
     * @param lastName Patient's last name
     * @param age Patient's age
     * @param phoneNumber Patient's phone number
     * @return The newly created patient
     * @throws IllegalArgumentException if any input is invalid
     */
    public Patient createPatient(String firstName, String lastName, int age, String phoneNumber) {
        // Validate inputs
        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        validateAge(age);
        validatePhoneNumber(phoneNumber);

        // Check for delimiter characters
        checkNoDelimiter(firstName, "First name");
        checkNoDelimiter(lastName, "Last name");
        checkNoDelimiter(phoneNumber, "Phone number");

        // Trim inputs
        firstName = firstName.trim();
        lastName = lastName.trim();
        phoneNumber = phoneNumber.trim();

        // Create patient
        long id = storage.nextPatientId();
        Patient patient = new Patient(firstName, lastName, age, id, phoneNumber);
        storage.savePatient(patient);
        FileIO.upsertPatientLine(patient, patientFilePath);
        return patient;
    }

    /**
     * Retrieves a patient by ID.
     *
     * @param id Patient's ID
     * @return The patient
     * @throws IllegalArgumentException if patient doesn't exist
     */
    public Patient getPatientById(long id) {
        Patient patient = storage.getPatient(id);
        if (patient == null) {
            throw new IllegalArgumentException("Patient with ID " + id + " does not exist");
        }
        return patient;
    }

    /**
     * Checks if a patient exists by ID.
     *
     * @param id Patient's ID
     * @return true if the patient exists, false otherwise
     */
    public boolean existsById(long id) {
        return storage.hasPatient(id);
    }

    /**
     * Returns all patients.
     *
     * @return Collection of all patients
     */
    public Collection<Patient> listOfPatients() {
        return storage.allPatients();
    }

/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Update related methods for a patient
/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Patient updateAge(long id, int age) {
        // Validate input
        validateAge(age);

        // Get existing patient
        Patient patient = storage.getPatient(id);
        if (patient == null) {
            throw new IllegalArgumentException("Patient with ID " + id + " does not exist");
        }

        // Update and save - use AndTouch variant for single field update
        patient.setAgeAndTouch(age);
        storage.savePatient(patient);
        FileIO.upsertPatientLine(patient, patientFilePath);
        return patient;
    }

    /**
     * Helper method to get an existing patient or throw an exception.
     *
     * @param id The patient ID
     * @return The patient if it exists
     * @throws IllegalArgumentException if the patient doesn't exist
     */
    private Patient getExistingPatient(long id) {
        Patient patient = storage.getPatient(id);
        if (patient == null) {
            throw new IllegalArgumentException("Patient with ID " + id + " does not exist");
        }
        return patient;
    }

    /**
     * Updates a patient's name with validation.
     *
     * @param id Patient's ID
     * @param firstName New first name
     * @param lastName New last name
     * @return The updated patient
     * @throws IllegalArgumentException if patient doesn't exist or names are invalid
     */
    public Patient updateName(long id, String firstName, String lastName) {
        // Validate inputs
        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        checkNoDelimiter(firstName, "First name");
        checkNoDelimiter(lastName, "Last name");

        // Get existing patient
        Patient patient = getExistingPatient(id);

        // Update and save - use AndTouch variants for single field updates
        patient.setFirstNameAndTouch(firstName);
        patient.setLastNameAndTouch(lastName);
        storage.savePatient(patient);
        FileIO.upsertPatientLine(patient, patientFilePath);
        return patient;
    }

    /**
     * Updates a patient's phone number with validation.
     *
     * @param id Patient's ID
     * @param phoneNumber New phone number
     * @return The updated patient
     * @throws IllegalArgumentException if patient doesn't exist or phone number is invalid
     */
    public Patient updatePhone(long id, String phoneNumber) {
        // Validate input
        validatePhoneNumber(phoneNumber);
        checkNoDelimiter(phoneNumber, "Phone number");

        // Get existing patient
        Patient patient = getExistingPatient(id);

        // Update and save - use AndTouch variant for single field update
        patient.setPhoneNumberAndTouch(phoneNumber);
        storage.savePatient(patient);
        FileIO.upsertPatientLine(patient, patientFilePath);
        return patient;
    }

/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Deletion related methods for a patient
/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Deletes a patient if they have no appointments.
     *
     * @param id Patient's ID
     * @return true if the patient was deleted, false otherwise
     * @throws IllegalStateException if the patient has appointments
     */
    public boolean deletePatient(long id) {
        Patient patient = storage.getPatient(id);
        if (patient == null) {
            return false;
        }

        if (patientHasAppointments(id)) {
            throw new IllegalStateException("Cannot delete patient with existing appointments. Cancel them first.");
        }
        boolean removed = storage.removePatient(id);
        if (removed) FileIO.deletePatientLine(id, patientFilePath);
        return removed;
    }

    /**
     * Checks if a patient has any appointments (efficient check using index).
     *
     * @param patientId Patient's ID
     * @return true if the patient has at least one appointment
     */
    private boolean patientHasAppointments(long patientId) {
        if (patientId <= 0) {
            throw new IllegalArgumentException("Patient ID must be positive");
        }
        return storage.patientHasAppointments(patientId);
    }

/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// Appointment related methods for a patient
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Counts the number of appointments for a patient.
     *
     * @param patientId Patient's ID
     * @return Number of appointments
     * @throws IllegalArgumentException if patient ID is invalid or patient doesn't exist
     */
    public int appointmentCount(long patientId) {
        return storage.getAppointmentsOfSomePatient(patientId).size();
    }

    /**
     * Lists all appointments for a patient.
     *
     * @param patientId Patient's ID
     * @return List of appointments
     * @throws IllegalArgumentException if patient ID is invalid or patient doesn't exist
     */
    public List<Appointment> listAppointments(long patientId) {
        List<Appointment> allAppointments = storage.getAppointmentsOfSomePatient(patientId);
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

    public int cancelAllAppointmentsOfSomePatient(long patientId) {
        List<Appointment> list = new ArrayList<>(storage.getAppointmentsOfSomePatient(patientId));
        int removed = 0;
        for (Appointment a : list) {
            if (storage.removeAppointment(a.getId())) {
                // Persist removal to file (prevent phantom appointments after restart)
                FileIO.deleteAppointmentLine(a.getId(), APPOINTMENT_FILE_PATH);
                removed++;
            } else {
                System.err.println("Failed to remove appointment with ID: " + a.getId());
            }
        }
        return removed;
    }
/// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// Categorizing Attribute Categories
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Make a modifiable copy of all patients (base for all finders)
private ArrayList<Patient> snapshotAll() {
    return new ArrayList<>(storage.allPatients());
}

    // 1) All patients (UNSORTED)
    public List<Patient> listAllUnsorted() {
        return snapshotAll();
    }

    // 2) Last name starts-with (case-insensitive), UNSORTED
    public List<Patient> findByLastNamePrefix(String prefix) {
        if (prefix == null || (prefix = prefix.trim()).isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }
        String pfx = prefix.toLowerCase(Locale.ROOT);
        List<Patient> patients = snapshotAll();
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            String ln = p.getLastName();
            if (ln != null && ln.trim().toLowerCase(Locale.ROOT).startsWith(pfx)) {
                result.add(p);
            }
        }
        return result;
    }

    // 3) First name starts-with (case-insensitive), UNSORTED
    public List<Patient> findByFirstNamePrefix(String prefix) {
        if (prefix == null || (prefix = prefix.trim()).isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }
        String pfx = prefix.toLowerCase(Locale.ROOT);
        List<Patient> patients = snapshotAll();
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            String fn = p.getFirstName();
            if (fn != null && fn.trim().toLowerCase(Locale.ROOT).startsWith(pfx)) {
                result.add(p);
            }
        }
        return result;
    }

    // 4) Phone exact (duplicates allowed), UNSORTED
    public List<Patient> findByPhone(String phoneNumber) {
        validatePhoneNumber(phoneNumber); // uses your existing validator
        String phone = phoneNumber.trim();
        List<Patient> patients = snapshotAll();
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            String ph = p.getPhoneNumber();
            if (ph != null && ph.equals(phone)) {
                result.add(p);
            }
        }
        return result;
    }

    // 5) Age in range [minAge..maxAge], UNSORTED
    public List<Patient> filterByAgeRange(int minAge, int maxAge) {
        if (minAge < 1 || maxAge > 120 || minAge > maxAge) {
            throw new IllegalArgumentException("Invalid age range: " + minAge + " to " + maxAge);
        }
        List<Patient> patients = snapshotAll();
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            int a = p.getAge();
            if (a >= minAge && a <= maxAge) {
                result.add(p);
            }
        }
        return result;
    }

    // 6) Created-at BETWEEN [start..end] inclusive, UNSORTED
    // expects "dd-MM-yyyy HH:mm:ss" (TimeUtil also accepts without seconds)
    public List<Patient> filterByCreatedAtRange(String start, String end) {
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

        List<Patient> patients = snapshotAll();
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            String c = p.getCreatedAt();
            if (c == null || c.isBlank()) continue;

            try {
                var created = TimeUtil.parseDateTime(c.trim());
                if (!created.isBefore(from) && !created.isAfter(to)) { // inclusive
                    result.add(p);
                }
            } catch (IllegalArgumentException e) {
                // Skip patients with unparseable dates instead of failing the entire search
                System.err.println("Warning: Patient ID " + p.getId() +
                                  " has invalid createdAt date format: " + c);
                continue;
            }
        }
        return result;
    }

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
     * Validates that an age is within reasonable bounds.
     *
     * @param age The age to validate
     * @throws IllegalArgumentException if age is outside valid range
     */
    private void validateAge(int age) {
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120, got: " + age);
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

    public enum PatientSortField {
        ID, FIRST_NAME, LAST_NAME, FULL_NAME, AGE, PHONE, CREATED_AT, UPDATED_AT
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Sort related methods for a patient
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sorts (in-place) the provided patient list by the selected field (one criterion only).
     */
    public List<Patient> sort(List<Patient> patients, PatientSortField field, EntitySorter.Direction dir) {
        if (patients == null || patients.size() < 2 || field == null) return patients;
        switch (field) {
            case ID:          return EntitySorter.byId(patients, dir);
            case FIRST_NAME:  return PersonSorter.byFirstName(patients, dir);
            case LAST_NAME:   return PersonSorter.byLastName(patients, dir);
            case FULL_NAME:   return PersonSorter.byFullName(patients, dir);
            case AGE:         return PersonSorter.byAge(patients, dir);
            case PHONE:       return PersonSorter.byPhone(patients, dir);
            case CREATED_AT:  return EntitySorter.byCreatedAt(patients, dir);
            case UPDATED_AT:  return EntitySorter.byUpdatedAt(patients, dir);
            default:          return patients;
        }
    }

    // Convenience: filter then sort example
    public List<Patient> findByFirstNamePrefixSorted(String prefix, PatientSortField field, EntitySorter.Direction dir) {
        return sort(findByFirstNamePrefix(prefix), field, dir);
    }

    // Convenience: find by last name prefix then sort
    public List<Patient> findByLastNamePrefixSorted(String prefix, PatientSortField field, EntitySorter.Direction dir) {
        return sort(findByLastNamePrefix(prefix), field, dir);
    }
}
