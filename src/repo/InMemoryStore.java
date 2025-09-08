package repo;

import model.Doctor;
import model.Patient;
import model.Appointment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Collection;

/**
 * In-memory implementation of storage for patients, doctors, and appointments.
 *
 * This class provides a non-persistent data storage solution that maintains entities
 * and their relationships in memory. It manages three main entity types:
 * - Patients: Individuals receiving medical care
 * - Doctors: Medical professionals providing care
 * - Appointments: Scheduled meetings between patients and doctors
 *
 * Key features:
 * 1. Entity ID management with auto-incrementing IDs
 * 2. CRUD operations for all entity types
 * 3. Patient & Doctor appointment relationship indexes for efficient lookups
 * 4. Data integrity enforcement through validation
 *
 * The store maintains referential integrity between entities, ensuring that
 * appointments cannot reference non-existent patients or doctors.
 */
public class InMemoryStore {

    // Auto-incrementing ID counters for entity creation
    private long nextPatientId = 1;
    private long nextDoctorId = 1;
    private long nextAppointmentId = 1;

    /**
     * Primary data stores for each entity type.
     * Each HashMap maps from entity ID (Long) to the entity object.
     */
    private final HashMap<Long, Patient> PatientList = new HashMap<>();
    private final HashMap<Long, Doctor> DoctorList = new HashMap<>();
    private final HashMap<Long, Appointment> AppointmentList = new HashMap<>();

    /**
     * Relationship index that maps patient IDs to a list of their appointment IDs.
     * This provides efficient lookup of all appointments for a specific patient
     * without having to scan the entire appointment list.
     */
    private final HashMap<Long, ArrayList<Long>> PatientAppointments = new HashMap<>();

    /**
     * Relationship index that maps doctor IDs to a list of their appointment IDs.
     * Mirrors PatientAppointments for efficient doctor-centric queries.
     */
    private final HashMap<Long, ArrayList<Long>> DoctorAppointments = new HashMap<>();

    /**
     * Creates a new empty in-memory store.
     * Initializes all collections and starts ID sequences at 1.
     */
    public InMemoryStore() {
        // Default constructor
    }

    /**
     * Generates the next available patient ID.
     *
     * @return A unique, auto-incrementing ID for a new patient
     */
    public long nextPatientId() {
        return nextPatientId++;
    }

    /**
     * Generates the next available doctor ID.
     *
     * @return A unique, auto-incrementing ID for a new doctor
     */
    public long nextDoctorId() {
        return nextDoctorId++;
    }

    /**
     * Generates the next available appointment ID.
     *
     * @return A unique, auto-incrementing ID for a new appointment
     */
    public long nextAppointmentId() {
        return nextAppointmentId++;
    }

    /**
     * Ensures the next patient ID is higher than the given ID.
     * Useful when loading patients from external storage to prevent ID collisions.
     *
     * @param id The ID to compare against
     */
    public void ensureNextPatientIdAbove(long id) {
        if (nextPatientId <= id) nextPatientId = id + 1;
    }

    /**
     * Ensures the next doctor ID is higher than the given ID.
     * Useful when loading doctors from external storage to prevent ID collisions.
     *
     * @param id The ID to compare against
     */
    public void ensureNextDoctorIdAbove(long id) {
        if (nextDoctorId <= id) nextDoctorId = id + 1;
    }

    /**
     * Ensures the next appointment ID is higher than the given ID.
     * Useful when loading appointments from external storage to prevent ID collisions.
     *
     * @param id The ID to compare against
     */
    public void ensureNextAppointmentIdAbove(long id) {
        if (nextAppointmentId <= id) nextAppointmentId = id + 1;
    }

    /// /////////////////////////////////////////////////////
    /// Patient Methods                           ///////////
    /// /////////////////////////////////////////////////////

    /**
     * Saves a patient to the store.
     *
     * @param patient Patient to save
     * @return The saved patient
     * @throws IllegalArgumentException if patient is null or has invalid ID
     */
    public Patient savePatient(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient cannot be null");
        }
        validateId(patient.getId(), "Patient");

        PatientList.put(patient.getId(), patient);
        return patient;
    }

    /**
     * Retrieves a patient by ID.
     *
     * @param id ID of the patient to retrieve
     * @return The patient, or null if not found
     * @throws IllegalArgumentException if ID is invalid
     */
    public Patient getPatient(long id) {
        validateId(id, "Patient");
        return PatientList.get(id);
    }

    /**
     * Checks if a patient with the given ID exists.
     *
     * @param id ID to check
     * @return true if the patient exists, false otherwise
     * @throws IllegalArgumentException if ID is invalid
     */
    public boolean hasPatient(long id) {
        validateId(id, "Patient");
        return PatientList.containsKey(id);
    }

    /**
     * Returns all patients in the store.
     *
     * @return An unmodifiable collection of all patients
     */
    public Collection<Patient> allPatients() {
        return Collections.unmodifiableCollection(PatientList.values());
    }

    /**
     * Removes a patient by ID.
     *
     * @param id ID of the patient to remove
     * @return true if the patient was removed, false if it didn't exist
     * @throws IllegalArgumentException if ID is invalid
     */
    public boolean removePatient(long id) {
        validateId(id, "Patient");
        return PatientList.remove(id) != null;
    }


    /// /////////////////////////////////////////////////////
    /// Doctor Methods                           ///////////
    /// /////////////////////////////////////////////////////

    /**
     * Saves a doctor to the store.
     *
     * @param doctor Doctor to save
     * @return The saved doctor
     * @throws IllegalArgumentException if doctor is null or has invalid ID
     */
    public Doctor saveDoctor(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("Doctor cannot be null");
        }
        validateId(doctor.getId(), "Doctor");

        DoctorList.put(doctor.getId(), doctor);
        return doctor;
    }


    /**
     * Removes a doctor by ID.
     *
     * @param id ID of the doctor to remove
     * @return true if the doctor was removed, false if it didn't exist
     * @throws IllegalArgumentException if ID is invalid
     */
    public boolean removeDoctor(long id) {
        validateId(id, "Doctor");
        return DoctorList.remove(id) != null;
    }

    /**
     * Retrieves a doctor by ID.
     *
     * @param id ID of the doctor to retrieve
     * @return The doctor, or null if not found
     * @throws IllegalArgumentException if ID is invalid
     */
    public Doctor getDoctor(long id) {
        validateId(id, "Doctor");
        return DoctorList.get(id);
    }

    /**
     * Checks if a doctor with the given ID exists.
     *
     * @param id ID to check
     * @return true if the doctor exists, false otherwise
     * @throws IllegalArgumentException if ID is invalid
     */
    public boolean hasDoctor(long id) {
        validateId(id, "Doctor");
        return DoctorList.containsKey(id);
    }

    /**
     * Returns all doctors in the store.
     *
     * @return An unmodifiable collection of all doctors
     */
    public Collection<Doctor> allDoctors() {
        return Collections.unmodifiableCollection(DoctorList.values());
    }

    /// /////////////////////////////////////////////////////
    /// Appointment Methods                      ///////////
    /// /////////////////////////////////////////////////////

    /**
     * Saves an appointment to the store and updates patient/doctor appointment relationships.
     *
     * This method has complex behavior to maintain referential integrity:
     * 1. Validates the appointment ID and referenced patient ID
     * 2. Ensures the referenced patient exists (checks for doctor existence as well)
     * 3. Handles appointment reassignment between patients:
     *    - If the appointment previously belonged to another patient (appointment
     *      patient ID changed), then remove the appointment from the previous patient's
     *      appointment list
     * 4. Updates the current patient's appointment list, creating it if needed
     * 5. Prevents duplicate appointment IDs in a patient's appointment list
     *
     * @param appointment Appointment to save
     * @return The saved appointment
     * @throws IllegalArgumentException if appointment is null, has invalid ID, or references non-existent patient
     */
    public Appointment saveAppointment(Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment cannot be null");
        }
        validateId(appointment.getId(), "Appointment");

        long newPid = appointment.getPatientId();
        validateId(newPid, "Patient");
        long newDid = appointment.getDoctorId();
        validateId(newDid, "Doctor");

        if (!hasDoctor(newDid)) {
            throw new IllegalArgumentException("Referenced doctor with ID " + newDid + " does not exist");
        }
        if (!hasPatient(newPid)) {
            throw new IllegalArgumentException("Referenced patient with ID " + newPid + " does not exist");
        }

        // Store the appointment and get any previous version (if it existed)
        Appointment prev = AppointmentList.put(appointment.getId(), appointment);

        // Handle patient reassignment
        if (prev != null && prev.getPatientId() != newPid) {
            ArrayList<Long> oldList = PatientAppointments.get(prev.getPatientId());
            if (oldList != null) {
                oldList.remove(Long.valueOf(prev.getId()));
                if (oldList.isEmpty()) {
                    PatientAppointments.remove(prev.getPatientId());
                }
            }
        }
        // Handle doctor reassignment
        if (prev != null && prev.getDoctorId() != newDid) {
            ArrayList<Long> oldDList = DoctorAppointments.get(prev.getDoctorId());
            if (oldDList != null) {
                oldDList.remove(Long.valueOf(prev.getId()));
                if (oldDList.isEmpty()) {
                    DoctorAppointments.remove(prev.getDoctorId());
                }
            }
        }

        // Link to patient
        ArrayList<Long> list;
        if (!PatientAppointments.containsKey(newPid)) {
            list = new ArrayList<>();
            PatientAppointments.put(newPid, list);
        } else {
            list = PatientAppointments.get(newPid);
        }
        if (!list.contains(appointment.getId())) {
            list.add(appointment.getId());
        }

        // Link to doctor
        ArrayList<Long> dlist;
        if (!DoctorAppointments.containsKey(newDid)) {
            dlist = new ArrayList<>();
            DoctorAppointments.put(newDid, dlist);
        } else {
            dlist = DoctorAppointments.get(newDid);
        }
        if (!dlist.contains(appointment.getId())) {
            dlist.add(appointment.getId());
        }

        return appointment;
    }

    /**
     * Retrieves an appointment by ID.
     *
     * @param id ID of the appointment to retrieve
     * @return The appointment, or null if not found
     * @throws IllegalArgumentException if ID is invalid
     */
    public Appointment getAppointment(long id) {
        validateId(id, "Appointment");
        return AppointmentList.get(id);
    }

    /**
     * Checks if an appointment with the given ID exists.
     *
     * @param id ID to check
     * @return true if the appointment exists, false otherwise
     * @throws IllegalArgumentException if ID is invalid
     */
    public boolean hasAppointment(long id) {
        validateId(id, "Appointment");
        return AppointmentList.containsKey(id);
    }

    /**
     * Returns all appointments in the store.
     *
     * @return An unmodifiable collection of all appointments
     */
    public Collection<Appointment> allAppointments() {
        return Collections.unmodifiableCollection(AppointmentList.values());
    }

    /**
     * Retrieves all appointments for a patient with the specified ID.
     *
     * This method uses the PatientAppointments index for efficient lookup:
     * 1. Validates the patient ID and existence
     * 2. Retrieves the list of appointment IDs for the patient
     * 3. Looks up each appointment by ID from the main appointment store
     * 4. Handles potential data inconsistencies (missing appointments, null values)
     *    with appropriate warnings
     *
     * @param patientId ID of the patient whose appointments to retrieve
     * @return List of appointments for the patient (may be empty but never null)
     * @throws IllegalArgumentException if the patient ID is invalid or the patient doesn't exist
     */
    public ArrayList<Appointment> getAppointmentsOfSomePatient(long patientId) {
        validateId(patientId, "Patient");

        if (!hasPatient(patientId)) {
            throw new IllegalArgumentException("Patient with ID " + patientId + " does not exist");
        }

        ArrayList<Appointment> appointments = new ArrayList<>();
        ArrayList<Long> appointmentIds = PatientAppointments.get(patientId);

        // Return empty list if patient has no appointments
        if (appointmentIds == null) {
            return appointments;
        }

        // Retrieve each appointment by ID
        for (Long id : appointmentIds) {
            if (id == null) {
                continue; // Skip null IDs for robustness
            }

            Appointment a = AppointmentList.get(id);
            if (a != null) {
                appointments.add(a);
            } else {
                // Data integrity warning: referenced appointment not found
                System.err.println("Warning: Appointment ID " + id +
                        " referenced for patient " + patientId +
                        " but not found in appointment records");
            }
        }

        return appointments;
    }

    /**
     * Retrieves all appointments for a doctor with the specified ID using the doctor index.
     *
     * @param doctorId ID of the doctor
     * @return List of appointments for the doctor
     */
    public ArrayList<Appointment> getAppointmentsOfSomeDoctor(long doctorId) {
        validateId(doctorId, "Doctor");
        if (!hasDoctor(doctorId)) {
            throw new IllegalArgumentException("Doctor with ID " + doctorId + " does not exist");
        }
        ArrayList<Appointment> appointments = new ArrayList<>();
        ArrayList<Long> ids = DoctorAppointments.get(doctorId);
        if (ids == null) return appointments;
        for (Long aid : ids) {
            if (aid == null) continue;
            Appointment ap = AppointmentList.get(aid);
            if (ap != null) {
                appointments.add(ap);
            } else {
                System.err.println("Warning: Appointment ID " + aid +
                        " referenced for doctor " + doctorId + " but not found in appointment records");
            }
        }
        return appointments;
    }

    /**
     * Removes an appointment by ID and updates related data structures.
     *
     * This method maintains referential integrity by:
     * 1. Removing the appointment from the main appointment store
     * 2. Removing the appointment reference from the patient's appointment list
     * 3. Cleaning up empty patient appointment lists
     *
     * @param id ID of the appointment to remove
     * @return true if the appointment was removed, false if it didn't exist
     * @throws IllegalArgumentException if ID is invalid
     */
    public boolean removeAppointment(long id) {
        validateId(id, "Appointment");

        // Remove from main store and check if it existed
        Appointment a = AppointmentList.remove(id);
        if (a == null) {
            return false;
        }
        // Patient side
        ArrayList<Long> AppointmentsOfSomePatient = PatientAppointments.get(a.getPatientId());
        if (AppointmentsOfSomePatient != null) {
            AppointmentsOfSomePatient.remove(Long.valueOf(id));
            if (AppointmentsOfSomePatient.isEmpty()) {
                PatientAppointments.remove(a.getPatientId());
            }
        }
        // Doctor side
        ArrayList<Long> AppointmentsOfSomeDoctor = DoctorAppointments.get(a.getDoctorId());
        if (AppointmentsOfSomeDoctor != null) {
            AppointmentsOfSomeDoctor.remove(Long.valueOf(id));
            if (AppointmentsOfSomeDoctor.isEmpty()) {
                DoctorAppointments.remove(a.getDoctorId());
            }
        }
        return true;
    }

    /**
     * Indicates whether a doctor currently has any appointments (fast via doctor index).
     *
     * @param doctorId doctor ID
     * @return true if at least one appointment references the doctor
     */
    public boolean doctorHasAppointments(long doctorId) {
        validateId(doctorId, "Doctor");
        var list = DoctorAppointments.get(doctorId);
        return list != null && !list.isEmpty();
    }

    /**
     * Indicates whether a patient currently has any appointments (fast via patient index).
     *
     * @param patientId patient ID
     * @return true if at least one appointment references the patient
     */
    public boolean patientHasAppointments(long patientId) {
        validateId(patientId, "Patient");
        var list = PatientAppointments.get(patientId);
        return list != null && !list.isEmpty();
    }

    /**
     * Validates that an ID is positive.
     *
     * This helper method centralizes ID validation logic to ensure all IDs
     * across the system follow the same rules. IDs must be positive numbers.
     *
     * @param id ID to validate
     * @param entityType Type of entity (for error message)
     * @throws IllegalArgumentException if ID is invalid (zero or negative)
     */
    private void validateId(long id, String entityType) {
        if (id <= 0) {
            throw new IllegalArgumentException(entityType + " ID must be positive, got: " + id);
        }
    }

}