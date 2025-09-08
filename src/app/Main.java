package app;

import repo.InMemoryStore;
import service.AppointmentService;
import service.CheckInService;
import service.DoctorService;
import service.PatientService;

/**
 * Main entry point for the De Ruda Clinic management system.
 * Initializes the application components and launches the user interface.
 *
 * This class sets up the core services including:
 * - In-memory data store for all entities
 * - Patient management service
 * - Doctor management service
 * - Appointment scheduling service
 * - Check-in service for patient arrivals
 *
 * The application uses flat-file persistence for patients, doctors, and appointments,
 * and binary serialization for check-in state.
 */
public class Main {
    /**
     * Application entry point that initializes all services and launches the UI.
     *
     * The initialization sequence is:
     * 1. Create the shared in-memory store
     * 2. Initialize core services (patient, doctor, appointment)
     * 3. Load persisted data from flat files
     * 4. Initialize the check-in service (loads its own serialized state)
     * 5. Launch the interactive menu UI
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Create central in-memory store that all services will share
        InMemoryStore store = new InMemoryStore();

        // Initialize core entity services
        PatientService patientSvc = new PatientService(store);
        DoctorService doctorSvc = new DoctorService(store);
        AppointmentService apptSvc = new AppointmentService(store);

        // Load all persistent data from flat files
        patientSvc.loadPatientsFromFile();
        doctorSvc.loadDoctorsFromFile();
        apptSvc.loadAppointmentsFromFile();

        // Initialize check-in service (loads its own serialized state)
        // Must be created after appointment service since it depends on it
        CheckInService checkInSvc = new CheckInService(store, apptSvc);

        // Launch the interactive menu interface
        new Menu(patientSvc, doctorSvc, apptSvc, checkInSvc).run();
    }
}