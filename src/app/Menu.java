package app;

import model.Appointment;
import model.CheckIn;
import model.Doctor;
import model.Patient;
import service.AppointmentService;
import service.CheckInService;
import service.DoctorService;
import service.PatientService;
import util.EntitySorter;
import util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Command-line interface for the De Ruda Clinic management system.
 * Provides interactive menus for managing patients, doctors, appointments, and check-ins.
 *
 * This class handles all user interaction and delegates business logic to the appropriate
 * service classes. It's organized into menu sections for different entity types with
 * operations for creating, updating, searching, and deleting records.
 *
 * The menu structure includes:
 * - Patient management (registration, updates, search, appointments)
 * - Doctor management (registration, updates, search, appointments)
 * - Appointment management (scheduling, updates, search)
 * - Check-in management (walk-ins, scheduled check-ins, history)
 */
public class Menu {
    // Disable hybrid confirmations entirely (kept as compile-time flag)
    private static final boolean HYBRID_CONFIRM_UPDATES = false;
    // Centralized list of valid appointment status options for prompts
    private static final String APPOINTMENT_STATUS_OPTIONS = "Scheduled|Completed|Cancelled|No Show|Checked In|In Progress";

    private final PatientService patientSvc;
    private final DoctorService doctorSvc;
    private final AppointmentService apptSvc;
    private final CheckInService checkInSvc;
    private final Scanner in = new Scanner(System.in);

    /**
     * Creates a new menu with the specified services.
     *
     * @param patientSvc The patient service for patient operations
     * @param doctorSvc The doctor service for doctor operations
     * @param apptSvc The appointment service for appointment operations
     * @param checkInSvc The check-in service for check-in operations
     */
    public Menu(PatientService patientSvc, DoctorService doctorSvc, AppointmentService apptSvc, CheckInService checkInSvc) {
        this.patientSvc = patientSvc;
        this.doctorSvc = doctorSvc;
        this.apptSvc = apptSvc;
        this.checkInSvc = checkInSvc;
    }

    /**
     * Starts the interactive menu loop.
     * Displays the main menu and processes user input until exit.
     */
    public void run() {
        boolean running = true;
        while (running) {
            printMain();
            String choice = prompt("Select option: ").trim();
            try {
                switch (choice) {
                    case "1": patientMenu(); break;
                    case "2": doctorMenu(); break;
                    case "3": appointmentMenu(); break;
                    case "4": checkInMenu(); break;
                    case "5": saveAll(); break;
                    case "0": running = false; saveAll(); System.out.println("Goodbye."); break;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void printMain() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1) Patients");
        System.out.println("2) Doctors");
        System.out.println("3) Appointments");
        System.out.println("4) Check-Ins");
        System.out.println("5) Save All");
        System.out.println("0) Exit");
    }

    // ===================== PATIENT MENU (updated spec) =====================
    private void patientMenu() {
        while (true) {
            System.out.println("\n--- Patient Menu ---");
            System.out.println("1) Register New Patient");
            System.out.println("2) Update Patient Information");
            System.out.println("3) Delete Patient");
            System.out.println("4) Search Patients");
            System.out.println("5) List a Patient's Appointments");
            System.out.println("0) Back");
            String c = prompt("Select: ");
            try {
                switch (c) {
                    case "1": createPatient(); break;
                    case "2": updatePatientMenu(); break; // renamed behavior
                    case "3": deletePatientInteractive(); break;
                    case "4": searchPatientsFlow(); break;
                    case "5": listAppointmentsOfPatientFlow(); break;
                    case "0": return;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }
    }

    // Simplified per spec: every update action now re-selects a patient
    private void updatePatientMenu() {
        boolean loop = true;
        while (loop) {
            System.out.println("\nUpdate Patient Attributes (each action requires selecting a patient)");
            System.out.println("1) Update First Name");
            System.out.println("2) Update Last Name");
            System.out.println("3) Update Age");
            System.out.println("4) Update Phone Number");
            System.out.println("0) Back");
            String ch = prompt("Select: ");
            try {
                switch (ch) {
                    case "1": {
                        Patient p = selectPatient(); if (p == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Patient#"+p.getId()+" First="+p.getFirstName()+" Last="+p.getLastName());
                            if (act.equals("R")) { p = selectPatient(); if (p==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String newFn = prompt("New first name (required): ");
                        patientSvc.updateName(p.getId(), newFn, p.getLastName());
                        System.out.println("Updated first name.");
                        break; }
                    case "2": {
                        Patient p = selectPatient(); if (p == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Patient#"+p.getId()+" First="+p.getFirstName()+" Last="+p.getLastName());
                            if (act.equals("R")) { p = selectPatient(); if (p==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String newLn = prompt("New last name (required): ");
                        patientSvc.updateName(p.getId(), p.getFirstName(), newLn);
                        System.out.println("Updated last name.");
                        break; }
                    case "3": {
                        Patient p = selectPatient(); if (p == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Patient#"+p.getId()+" Age="+p.getAge());
                            if (act.equals("R")) { p = selectPatient(); if (p==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        try {
                            int newAge = Integer.parseInt(prompt("New age (1-120): ").trim());
                            patientSvc.updateAge(p.getId(), newAge);
                            System.out.println("Updated age.");
                        } catch (NumberFormatException nfe) { System.out.println("Invalid age."); }
                        break; }
                    case "4": {
                        Patient p = selectPatient(); if (p == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Patient#"+p.getId()+" Phone="+p.getPhoneNumber());
                            if (act.equals("R")) { p = selectPatient(); if (p==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String phone = prompt("New phone (7-15 digits): ");
                        patientSvc.updatePhone(p.getId(), phone);
                        System.out.println("Updated phone.");
                        break; }
                    case "0": loop = false; break;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception ex) { System.err.println("Error: " + ex.getMessage()); }
        }
    }

    // Search + Sort + Flip ordering
    private void searchPatientsFlow() {
        List<Patient> list = applyPatientFilter();
        if (list.isEmpty()) { System.out.println("No patients match filter"); return; }
        list = applyPatientSort(list);
        printPatients(list);
        flipLoop(list, this::printPatients);
    }

    private void printPatients(List<Patient> list) {
        list.forEach(p -> System.out.println(p.getId() + ": " + p.getFirstName() + " " + p.getLastName() + " age=" + p.getAge()));
        System.out.println("Total: " + list.size());
    }

    private void listAppointmentsOfPatientFlow() {
        String raw = prompt("Enter Patient ID (blank to search): ");
        Patient p;
        if (raw.isBlank()) {
            p = selectPatient();
            if (p == null) return;
        } else {
            try { p = patientSvc.getPatientById(Long.parseLong(raw.trim())); } catch (Exception e) { System.out.println("Invalid ID"); return; }
        }
        try {
            List<Appointment> appts = patientSvc.listAppointments(p.getId());
            if (appts.isEmpty()) { System.out.println("No appointments."); return; }
            appts.forEach(a -> System.out.println(fmtAppt(a)));
            System.out.println("Total: " + appts.size());
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    // ===================== DOCTOR MENU (updated spec) =====================
    private void doctorMenu() {
        while (true) {
            System.out.println("\n--- Doctor Menu ---");
            System.out.println("1) Register New Doctor");
            System.out.println("2) Update Doctor Information");
            System.out.println("3) Delete Doctor");
            System.out.println("4) Search Doctors");
            System.out.println("5) List a Doctor's Appointments");
            System.out.println("0) Back");
            String c = prompt("Select: ");
            try {
                switch (c) {
                    case "1": createDoctor(); break;
                    case "2": updateDoctorMenu(); break; // reuse existing but rename semantics
                    case "3": deleteDoctorInteractive(); break;
                    case "4": searchDoctorsFlow(); break;
                    case "5": listAppointmentsOfDoctorFlow(); break;
                    case "0": return;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }
    }

    private void searchDoctorsFlow() {
        List<Doctor> list = applyDoctorFilter();
        if (list.isEmpty()) { System.out.println("No doctors match filter"); return; }
        list = applyDoctorSort(list);
        printDoctors(list);
        flipLoop(list, this::printDoctors);
    }

    private void printDoctors(List<Doctor> list) {
        list.forEach(d -> System.out.println(d.getId() + ": Dr. " + d.getFirstName() + " " + d.getLastName() + " (" + d.getSpecialty() + ")"));
        System.out.println("Total: " + list.size());
    }

    private void listAppointmentsOfDoctorFlow() {
        String raw = prompt("Enter Doctor ID (blank to search): ");
        Doctor d;
        if (raw.isBlank()) {
            d = selectDoctor();
            if (d == null) return;
        } else {
            try { d = doctorSvc.getDoctorById(Long.parseLong(raw.trim())); } catch (Exception e) { System.out.println("Invalid ID"); return; }
        }
        try {
            List<Appointment> appts = doctorSvc.listAppointments(d.getId());
            if (appts.isEmpty()) { System.out.println("No appointments."); return; }
            appts.forEach(a -> System.out.println(fmtAppt(a)));
            System.out.println("Total: " + appts.size());
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    // ===================== APPOINTMENT MENU (updated spec) =====================
    private void appointmentMenu() {
        while (true) {
            System.out.println("\n--- Appointment Menu ---");
            System.out.println("1) Create Appointment");
            System.out.println("2) Update Appointment Information");
            System.out.println("3) Delete Appointment");
            System.out.println("4) Search Appointments");
            System.out.println("0) Back");
            String c = prompt("Select: ");
            try {
                switch (c) {
                    case "1": createAppointment(); break;
                    case "2": updateAppointmentMenu(); break; // existing detailed update
                    case "3": deleteAppointmentInteractive(); break;
                    case "4": searchAppointmentsFlow(); break;
                    case "0": return;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }
    }

    private void searchAppointmentsFlow() {
        List<Appointment> list = applyAppointmentFilter();
        if (list.isEmpty()) { System.out.println("No appointments after filter"); return; }
        list = applyAppointmentSort(list);
        printAppointments(list);
        flipLoop(list, this::printAppointments);
    }

    private void printAppointments(List<Appointment> list) {
        list.forEach(a -> System.out.println(fmtAppt(a)));
        System.out.println("Total: " + list.size());
    }

    // Flip ordering utility
    private <T> void flipLoop(List<T> original, java.util.function.Consumer<List<T>> printer) {
        while (true) {
            String inp = prompt("Press F to flip order, Enter to finish: ");
            if (inp == null || inp.isBlank()) break;
            if (inp.equalsIgnoreCase("F")) {
                List<T> rev = new ArrayList<>(original);
                java.util.Collections.reverse(rev);
                printer.accept(rev);
            } else break;
        }
    }

    private void deletePatientInteractive() {
        String raw = prompt("Patient ID or 'S' to search: ");
        long id;
        if (raw.equalsIgnoreCase("S")) {
            Patient p = selectPatient();
            if (p == null) { System.out.println("No selection."); return; }
            id = p.getId();
        } else {
            id = Long.parseLong(raw);
        }
        boolean ok = patientSvc.deletePatient(id);
        System.out.println(ok ? "Deleted." : "Not found.");
    }

    private void updateDoctorMenu() {
        boolean loop = true;
        while (loop) {
            System.out.println("\nUpdate Doctor Attributes (each action requires selecting a doctor)");
            System.out.println("1) Update Name");
            System.out.println("2) Update Phone");
            System.out.println("3) Update Specialty");
            System.out.println("0) Back");
            String ch = prompt("Select: ");
            try {
                switch (ch) {
                    case "1": {
                        Doctor d = selectDoctor(); if (d == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Doctor#"+d.getId()+" Dr."+d.getFirstName()+" "+d.getLastName());
                            if (act.equals("R")) { d = selectDoctor(); if (d==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String fn = prompt("New first name (required): ");
                        String ln = prompt("New last name (required): ");
                        doctorSvc.updateName(d.getId(), fn, ln);
                        System.out.println("Updated name.");
                        break; }
                    case "2": {
                        Doctor d = selectDoctor(); if (d == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Doctor#"+d.getId()+" Phone="+d.getPhoneNumber());
                            if (act.equals("R")) { d = selectDoctor(); if (d==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String phone = prompt("New phone (7-15 digits): ");
                        doctorSvc.updatePhone(d.getId(), phone);
                        System.out.println("Updated phone.");
                        break; }
                    case "3": {
                        Doctor d = selectDoctor(); if (d == null) { System.out.println("Cancelled."); break; }
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Doctor#"+d.getId()+" Specialty="+d.getSpecialty());
                            if (act.equals("R")) { d = selectDoctor(); if (d==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String spec = prompt("New specialty (required): ");
                        doctorSvc.updateSpecialty(d.getId(), spec);
                        System.out.println("Updated specialty.");
                        break; }
                    case "0": loop = false; break;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception ex) { System.err.println("Error: " + ex.getMessage()); }
        }
    }

    private void deleteDoctorInteractive() {
        String raw = prompt("Doctor ID or 'S' to search: ");
        long id;
        if (raw.equalsIgnoreCase("S")) {
            Doctor d = selectDoctor();
            if (d == null) { System.out.println("No selection."); return; }
            id = d.getId();
        } else {
            id = Long.parseLong(raw);
        }
        try {
            boolean ok = doctorSvc.deleteDoctor(id);
            System.out.println(ok ? "Deleted." : "Not found.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // ===================== APPOINTMENT MENU (granular) =====================
    // Duplicate removed: unified into single appointmentMenu() with search functionality above.
    // (Previously a second appointmentMenu without search existed here and has been deleted.)

    private void updateAppointmentMenu() {
        boolean loop = true;
        while (loop) {
            System.out.println("\nUpdate Appointment Attributes (each action selects an appointment)");
            System.out.println("1) Update Status");
            System.out.println("2) Update Date & Time");
            System.out.println("3) Update Duration");
            System.out.println("4) Update Location");
            System.out.println("5) Update Follow-Up Date");
            System.out.println("6) Update Reason");
            System.out.println("7) Update Notes");
            System.out.println("8) Batch Update (multi fields)");
            System.out.println("9) Cancel Appointment (status=Cancelled)");
            System.out.println("0) Back");
            String ch = prompt("Select: ");
            try {
                switch (ch) {
                    case "1": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Status="+a.getStatus());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String status = prompt("New status (" + APPOINTMENT_STATUS_OPTIONS + "): ");
                        apptSvc.updateStatus(a.getId(), status);
                        System.out.println("Updated status.");
                        break; }
                    case "2": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Date="+a.getAppointmentDate()+" "+a.getAppointmentTime());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String date = prompt("New date (dd-MM-yyyy): ");
                        String time = prompt("New time (HH:mm, 24h): ");
                        apptSvc.updateDateTime(a.getId(), date, time);
                        System.out.println("Updated date/time.");
                        break; }
                    case "3": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Duration="+a.getDuration());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String dur = prompt("New duration (HH:mm): ");
                        apptSvc.updateDuration(a.getId(), dur);
                        System.out.println("Updated duration.");
                        break; }
                    case "4": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Location="+a.getLocation());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String loc = prompt("New location (optional; blank allowed): ");
                        apptSvc.updateLocation(a.getId(), loc);
                        System.out.println("Updated location.");
                        break; }
                    case "5": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" FollowUp="+a.getFollowUpDate());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String fup = prompt("New follow-up date (dd-MM-yyyy or blank): ");
                        apptSvc.updateFollowUpDate(a.getId(), fup.isBlank()? null : fup);
                        System.out.println("Updated follow-up date.");
                        break; }
                    case "6": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Reason="+a.getReason());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String reason = prompt("New reason (optional; blank keep): ");
                        if (!reason.isBlank()) apptSvc.updateAppointment(a.getId(), null,null,null,null,null,null,reason,null);
                        System.out.println("Reason updated (if provided).");
                        break; }
                    case "7": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Notes="+a.getNotes());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        String notes = prompt("New notes (optional; blank keep): ");
                        if (!notes.isBlank()) apptSvc.updateNotes(a.getId(), notes);
                        System.out.println("Notes updated (if provided).");
                        break; }
                    case "8": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Batch");
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        batchUpdateAppointmentInteractive(a.getId());
                        System.out.println("Batch update applied.");
                        break; }
                    case "9": {
                        Appointment a = selectAppointment(); if (a == null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("Appt#"+a.getId()+" Cancel current status="+a.getStatus());
                            if (act.equals("R")) { a = selectAppointment(); if (a==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        apptSvc.cancelAppointment(a.getId());
                        System.out.println("Appointment cancelled.");
                        break; }
                    case "0": loop = false; break;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception ex) { System.err.println("Error: " + ex.getMessage()); }
        }
    }

    private void batchUpdateAppointmentInteractive(long id) {
        String date = opt(prompt("New date (dd-MM-yyyy) blank=keep: "));
        String time = opt(prompt("New time (HH:mm) blank=keep: "));
        String dur = opt(prompt("New duration (HH:mm) blank=keep: "));
        String status = opt(prompt("New status (" + APPOINTMENT_STATUS_OPTIONS + ") blank=keep: "));
        String loc = opt(prompt("New location (optional) blank=keep: "));
        String fup = opt(prompt("New follow-up date (dd-MM-yyyy) blank=keep: "));
        String reason = opt(prompt("New reason (optional) blank=keep: "));
        String notes = opt(prompt("New notes (optional) blank=keep: "));
        apptSvc.updateAppointment(id, date,time,dur,status,loc,fup,reason,notes);
    }

    private void deleteAppointmentInteractive() {
        String raw = prompt("Appointment ID or 'S' to search: ");
        long id;
        if (raw.equalsIgnoreCase("S")) {
            Appointment a = selectAppointment();
            if (a == null) { System.out.println("No selection."); return; }
            id = a.getId();
        } else {
            id = Long.parseLong(raw);
        }
        boolean ok = apptSvc.deleteAppointment(id);
        System.out.println(ok ? "Deleted." : "Not found.");
    }

    // ===================== SELECTION HELPERS =====================
    private Patient selectPatient() {
        List<Patient> list = applyPatientFilter();
        if (list.isEmpty()) { System.out.println("No patients found."); return null; }
        list = applyPatientSort(list);
        return pickEntity(list, p -> p.getId() + ": " + p.getFirstName() + " " + p.getLastName() + " age=" + p.getAge());
    }

    private Doctor selectDoctor() {
        List<Doctor> list = applyDoctorFilter();
        if (list.isEmpty()) { System.out.println("No doctors found."); return null; }
        list = applyDoctorSort(list);
        return pickEntity(list, d -> d.getId() + ": Dr. " + d.getFirstName() + " " + d.getLastName() + " (" + d.getSpecialty() + ")");
    }

    private Appointment selectAppointment() {
        List<Appointment> list = applyAppointmentFilter();
        if (list.isEmpty()) { System.out.println("No appointments found."); return null; }
        list = applyAppointmentSort(list);
        return pickEntity(list, this::fmtAppt);
    }

    private <T> T pickEntity(List<T> list, java.util.function.Function<T,String> labeler) {
        System.out.println("\nResults:");
        int i = 1;
        for (T e : list) {
            System.out.println(i++ + ") " + labeler.apply(e));
        }
        String inp = prompt("Select # (or 0 cancel): ");
        try {
            int idx = Integer.parseInt(inp);
            if (idx <= 0 || idx > list.size()) return null;
            return list.get(idx - 1);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ===================== CHECK-IN MENU (NEW SPEC) =====================
    private void checkInMenu() {
        while (true) {
            System.out.println("\n--- Check-In Menu ---");
            System.out.println("1) WalkIn CheckIn");
            System.out.println("2) Appointment CheckIn");
            System.out.println("3) Check-In History (View/Search)");
            System.out.println("0) Back");
            String c = prompt("Select: ");
            switch (c) {
                case "1": walkInCheckInMenu(); break;
                case "2": scheduledCheckInMenu(); break; // changed to submenu
                case "3": viewCheckInHistoryFlow(); break;
                case "0": return;
                default: System.out.println("Unknown option");
            }
        }
    }

    // ---------- Walk-In Submenu ----------
    private void walkInCheckInMenu() {
        while (true) {
            System.out.println("\n--- Walk-In Check-In ---");
            System.out.println("1) Inscription (New Walk-In)");
            System.out.println("2) Update Check-In Information");
            System.out.println("3) Delete a Check-In");
            System.out.println("4) Call Next Walk-In");
            System.out.println("5) List Queue (Priority Order)");
            System.out.println("6) List All Walk-In Check-Ins");
            System.out.println("7) List Options");
            System.out.println("0) Back");
            String c = prompt("Select: ");
            try {
                switch (c) {
                    case "1": createWalkInCheckIn(); break;
                    case "2": updateWalkInCheckInFlow(); break;
                    case "3": deleteWalkInCheckIn(); break;
                    case "4": callNextWalkInAction(); break;
                    case "5": listWalkInQueue(); break; // existing method reused
                    case "6": listAllWalkIns(); break;
                    case "7": walkInListOptions(); break;
                    case "0": return;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception ex) { System.err.println("Error: " + ex.getMessage()); }
        }
    }

    private void createWalkInCheckIn() {
        System.out.println("-- New Walk-In --");
        try {
            // 1) Patient: select existing or create new
            Patient patient;
            String pidRaw = prompt("Patient ID (blank to search, 'N' to create new): ").trim();
            if (pidRaw.isEmpty()) {
                patient = selectPatient();
                if (patient == null) { System.out.println("Cancelled."); return; }
            } else if (pidRaw.equalsIgnoreCase("N")) {
                String fn = prompt("First name (required): ");
                String ln = prompt("Last name (required): ");
                int age = Integer.parseInt(prompt("Age (1-120): "));
                String phone = prompt("Phone (7-15 digits): ");
                patient = patientSvc.createPatient(fn, ln, age, phone);
                System.out.println("Created patient ID=" + patient.getId());
            } else {
                patient = patientSvc.getPatientById(Long.parseLong(pidRaw));
            }

            // 2) Doctor: select via ID or search
            Doctor doctor;
            String didRaw = prompt("Doctor ID (blank to search): ").trim();
            if (didRaw.isEmpty()) {
                doctor = selectDoctor();
                if (doctor == null) { System.out.println("Cancelled."); return; }
            } else {
                doctor = doctorSvc.getDoctorById(Long.parseLong(didRaw));
            }

            // 3) Appointment details - use current date/time for walk-ins
            // Get current date and time
            String currentDate = TimeUtil.formatDate(java.time.LocalDate.now());
            String currentTime = TimeUtil.formatTime(java.time.LocalTime.now());
            System.out.println("Using current date and time: " + currentDate + " " + currentTime);

            // Only prompt for duration
            String dur = prompt("Duration (HH:mm): ");
            int priority = Integer.parseInt(prompt("Priority (integer >= 0): ").trim());
            String desk = promptOptional("Desk (optional; blank ok): ");
            String reason = promptOptional("Reason (optional; blank ok): ");
            String notes = promptOptional("Notes (optional; blank ok): ");

            // Create appointment implicitly and check-in as walk-in
            CheckIn c = checkInSvc.checkInWalkIn(
                    patient.getId(), doctor.getId(),
                    currentDate, currentTime, dur,
                    priority,
                    emptyToNull(desk), emptyToNull(reason), emptyToNull(notes)
            );
            System.out.println("Created Appointment ID=" + c.getAppointmentId() + ", Walk-In Check-In CI=" + c.getId());
        } catch (Exception e) {
            System.err.println("Failed: " + e.getMessage());
        }
    }

    private void updateWalkInCheckInFlow() {
        boolean loop = true;
        while (loop) {
            System.out.println("\nUpdate Walk-In Check-In (each action selects a walk-in check-in)");
            System.out.println("1) Call Patient (complete & archive)");
            System.out.println("2) Update Desk");
            System.out.println("3) Update Notes");
            System.out.println("4) Update Priority (only if status=CheckedIn)");
            System.out.println("5) Show Details");
            System.out.println("0) Back");
            String ch = prompt("Select: ");
            try {
                switch (ch) {
                    case "1": {
                        CheckIn c = selectWalkInCheckIn(); if (c==null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("CheckIn#"+c.getId()+" Status="+c.getStatus());
                            if (act.equals("R")) { c = selectWalkInCheckIn(); if (c==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        changeWalkInStatus(c.getId());
                        System.out.println("Status updated.");
                        break; }
                    case "2": {
                        CheckIn c = selectWalkInCheckIn(); if (c==null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("CheckIn#"+c.getId()+" Desk="+c.getDesk());
                            if (act.equals("R")) { c = selectWalkInCheckIn(); if (c==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        checkInSvc.updateDesk(c.getId(), prompt("Desk (optional; blank): "));
                        System.out.println("Desk updated.");
                        break; }
                    case "3": {
                        CheckIn c = selectWalkInCheckIn(); if (c==null) break;
                        if (HYBRID_CONFIRM_UPDATES) {
                            String act = hybridPrompt("CheckIn#"+c.getId()+" Notes="+c.getNotes());
                            if (act.equals("R")) { c = selectWalkInCheckIn(); if (c==null) break; }
                            if (act.equals("C")) break; if (act.equals("Q")) { loop=false; break; }
                        }
                        checkInSvc.updateNotes(c.getId(), prompt("Notes (optional; blank): "));
                        System.out.println("Notes updated.");
                        break; }
                    case "4": {
                        CheckIn c = selectWalkInCheckIn(); if (c==null) break; printCheckInDetailed(c); break; }
                    case "0": loop = false; break;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }
    }

    private void changeWalkInStatus(long id) {
        System.out.println("Status Options: 1) Call patient (complete)  2) Complete now  0) Cancel");
        String opt = prompt("Select: ");
        switch (opt) {
            case "1": checkInSvc.markCalled(id); break;
            case "2": checkInSvc.markCompleted(id); break;
            default: System.out.println("No change");
        }
    }

    private void deleteWalkInCheckIn() {
        CheckIn c = selectWalkInCheckIn();
        if (c == null) { System.out.println("No selection."); return; }
        if (!c.isWalkIn()) { System.out.println("Not a walk-in."); return; }
        boolean ok = checkInSvc.deleteCheckIn(c.getId());
        System.out.println(ok ? "Deleted." : "Not found.");
    }

    private void callNextWalkInAction() {
        CheckIn c = checkInSvc.callNextWalkIn();
        if (c == null) System.out.println("Queue empty"); else System.out.println("Called CI=" + c.getId() + " patient=" + c.getPatientId());
    }

    private void listAllWalkIns() {
        List<CheckIn> list = checkInSvc.listAllWalkIns();
        if (list.isEmpty()) { System.out.println("None."); return; }
        list.sort((a,b) -> Long.compare(b.getId(), a.getId())); // recent-first by id, returns 0 when equal
        for (CheckIn c : list) printCheckInDetailed(c);
        System.out.println("Total: " + list.size());
    }

    private void walkInListOptions() {
        System.out.println("1) By Patient ID  2) By Appointment ID  0) Back");
        String opt = prompt("Select: ");
        switch (opt) {
            case "1":
                try {
                    long pid = Long.parseLong(prompt("Patient ID: "));
                    List<CheckIn> list = checkInSvc.listWalkInsByPatient(pid);
                    if (list.isEmpty()) { System.out.println("None."); return; }
                    list.forEach(this::printCheckInDetailed);
                } catch (Exception e) { System.out.println("Invalid."); }
                break;
            case "2":
                try {
                    long aid = Long.parseLong(prompt("Appointment ID: "));
                    CheckIn c = checkInSvc.findByAppointmentId(aid);
                    if (c == null || !c.isWalkIn()) System.out.println("No walk-in check-in for that appointment."); else printCheckInDetailed(c);
                } catch (Exception e) { System.out.println("Invalid."); }
                break;
            default: return;
        }
    }

    private CheckIn selectWalkInCheckIn() {
        List<CheckIn> list = checkInSvc.listAllWalkIns();
        if (list.isEmpty()) { System.out.println("No walk-ins available"); return null; }
        // order by priority desc then checkedInAt asc (similar to queue) for selection clarity
        list.sort((a,b) -> {
            int p = Integer.compare(b.getPriority(), a.getPriority());
            if (p != 0) return p;
            String at = a.getCheckedInAt(), bt = b.getCheckedInAt();
            return at.compareTo(bt);
        });
        int i=1; for (CheckIn c : list) { System.out.println(i++ + ") CI=" + c.getId() + " appt=" + c.getAppointmentId() + " status=" + c.getStatus() + " prio=" + c.getPriority()); }
        String sel = prompt("Select # (0 cancel): ");
        try { int idx = Integer.parseInt(sel); if (idx<=0 || idx>list.size()) return null; return list.get(idx-1);} catch(Exception e){ return null; }
    }


    // ---------- Appointment (Scheduled) Check-In Flow ----------
    private void appointmentCheckInFlow() {
        System.out.println("-- Scheduled Appointment Check-In --");
        // Flexible filters to narrow down patients
        String firstPfx = prompt("Patient First Name starts with (blank skip): ").trim();
        String lastPfx  = prompt("Patient Last Name starts with (blank skip): ").trim();
        String phoneFrag= prompt("Phone contains digits (blank skip): ").trim();

        List<Patient> candidates = new ArrayList<>();
        for (Patient p : patientSvc.listAllUnsorted()) {
            boolean ok = true;
            if (!firstPfx.isEmpty()) ok &= p.getFirstName() != null && p.getFirstName().toLowerCase().startsWith(firstPfx.toLowerCase());
            if (!lastPfx.isEmpty())  ok &= p.getLastName()  != null && p.getLastName().toLowerCase().startsWith(lastPfx.toLowerCase());
            if (!phoneFrag.isEmpty()) ok &= p.getPhoneNumber() != null && p.getPhoneNumber().contains(phoneFrag);
            if (ok) candidates.add(p);
        }
        if (candidates.isEmpty()) { System.out.println("No patients match."); return; }

        Patient selected;
        if (candidates.size() == 1) {
            selected = candidates.get(0);
        } else {
            int i=1; for (Patient p : candidates) System.out.println(i++ + ") " + p.getId() + " " + p.getFirstName() + " " + p.getLastName());
            String pick = prompt("Select patient #: ");
            try { int idx = Integer.parseInt(pick); if (idx<1 || idx>candidates.size()) { System.out.println("Cancelled"); return; } selected = candidates.get(idx-1);} catch(Exception e){ System.out.println("Cancelled"); return; }
        }
        List<Appointment> appts = patientSvc.listAppointments(selected.getId());
        if (appts.isEmpty()) { System.out.println("No appointments for patient."); return; }
        // sort recent to later (date/time descending)
        apptSvc.sort(appts, AppointmentService.AppointmentSortField.START_DATE_TIME, EntitySorter.Direction.DESC);
        int i=1; for (Appointment a : appts) System.out.println(i++ + ") " + fmtAppt(a));
        String sel = prompt("Select appointment # to check-in (0 cancel): ");
        try { int idx = Integer.parseInt(sel); if (idx<=0 || idx>appts.size()) return; Appointment a = appts.get(idx-1); performScheduledCheckIn(a); } catch(Exception e){ System.out.println("Cancelled"); }
    }

    private void performScheduledCheckIn(Appointment a) {
        // Ensure no existing active check-in
        CheckIn existing = checkInSvc.findByAppointmentId(a.getId());
        if (existing != null && !existing.getStatus().equals("Completed") && !existing.getStatus().equals("Cancelled") && !existing.getStatus().equals("NoShow")) {
            System.out.println("Active check-in already exists (CI=" + existing.getId() + ")");
            return;
        }
        String desk = promptOptional("Desk (blank ok): ");
        String notes = promptOptional("Notes (blank ok): ");
        CheckIn c = checkInSvc.checkInScheduled(a.getId(), emptyToNull(desk), emptyToNull(notes));
        System.out.println("Created scheduled Check-In CI=" + c.getId());
    }

    private void saveAll() {
        patientSvc.saveAllPatientsToFile();
        doctorSvc.saveAllDoctorsToFile();
        apptSvc.saveAllAppointmentsToFile();
        checkInSvc.saveSnapshot();
        System.out.println("All data saved.");
    }

    // ---------------- Demo data seeding ----------------
    /**
     * Seeds the system with demonstration data for testing purposes.
     * This creates sample patients, doctors, appointments, and check-ins
     * to help demonstrate the system functionality.
     * Only seeds data if the system is empty to avoid duplication.
     */

    private void seedDemoData() {
        try {
            boolean hasPatients = !patientSvc.listAllUnsorted().isEmpty();
            boolean hasDoctors = !doctorSvc.listAllUnsorted().isEmpty();
            boolean hasAppts = !apptSvc.listOfAppointments().isEmpty();
            boolean hasCheckIns = !checkInSvc.listAll().isEmpty();

            if (hasPatients || hasDoctors || hasAppts || hasCheckIns) {
                System.out.println("Data already present. Seeding skipped to avoid duplicates.");
                return;
            }

            // Patients
            var p1 = patientSvc.createPatient("John", "Doe", 34, "5551234567");
            var p2 = patientSvc.createPatient("Jane", "Smith", 28, "5559876543");
            var p3 = patientSvc.createPatient("Alice", "Brown", 45, "5551112222");
            var p4 = patientSvc.createPatient("Bob", "Wilson", 52, "5553334444");

            // Doctors
            var d1 = doctorSvc.createDoctor("Emily", "Clark", "Cardiology", "5554445555");
            var d2 = doctorSvc.createDoctor("Michael", "Lee", "Pediatrics", "5556667777");
            var d3 = doctorSvc.createDoctor("Sarah", "Kim", "Dermatology", "5558889999");

            // Appointments (dates in dd-MM-yyyy)
            var a1 = apptSvc.createAppointment(p1.getId(), d1.getId(), "10-09-2025", "09:00", "00:30", "Scheduled", "Room A1", null, "Annual checkup", "Bring previous labs");
            var a2 = apptSvc.createAppointment(p2.getId(), d2.getId(), "10-09-2025", "10:15", "00:45", "Scheduled", "Room P2", "17-09-2025", "Pediatric follow-up", "N/A");
            var a3 = apptSvc.createAppointment(p3.getId(), d3.getId(), "11-09-2025", "14:00", "01:00", "Completed", "Derm Clinic", null, "Skin evaluation", "Biopsy performed");
            var a4 = apptSvc.createAppointment(p1.getId(), d2.getId(), "12-09-2025", "11:30", "00:30", "Cancelled", "Room P1", null, "Cold symptoms", "Rescheduled");
            var a5 = apptSvc.createAppointment(p4.getId(), d1.getId(), "13-09-2025", "08:45", "00:20", "Scheduled", "Room C3", null, "Blood pressure check", "Monitor at home");
            var a6 = apptSvc.createAppointment(p2.getId(), d1.getId(), "14-09-2025", "15:00", "00:30", "Scheduled", "Room C2", null, "Chest pain", "ECG planned");

            // Check-Ins: one scheduled, and two walk-ins (to demonstrate queue ordering)
            var ci1 = checkInSvc.checkInScheduled(a1.getId(), "FrontDesk-1", "Arrived early");
            // Adjust statuses for demonstration
            checkInSvc.markCalled(ci1.getId());

            var ci2 = checkInSvc.checkInWalkIn(p2.getId(), d1.getId(), "10-09-2025", "09:30", "00:20", 1, "FrontDesk-2", "Sore throat", null);
            var ci3 = checkInSvc.checkInWalkIn(p3.getId(), d3.getId(), "11-09-2025", "13:40", "00:15", 2, "FrontDesk-3", "Rash", "Itching");

            // Persist all
            saveAll();

            System.out.println("Seeded: 4 patients, 3 doctors, 6 appointments, 3 check-ins.");
        } catch (Exception e) {
            System.err.println("Seeding failed: " + e.getMessage());
        }
    }

    // ---- IO helpers ----
    private String prompt(String msg) { System.out.print(msg); return in.nextLine(); }
    private String promptOptional(String msg) { return prompt(msg); }
    private String emptyToNull(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }
    private String opt(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    // ===================== FILTER/SORT Methods =====================
    private List<Patient> applyPatientFilter() {
        System.out.println("\n--- Filter Patients By ---");
        System.out.println("1) Last Name starts with");
        System.out.println("2) First Name starts with");
        System.out.println("3) Phone Number (exact)");
        System.out.println("4) Age Range");
        System.out.println("5) Created Date Range");
        System.out.println("0) No Filter (all patients)");
        String c = prompt("Select filter: ");
        try {
            switch (c) {
                case "1":
                    String lastNamePrefix = prompt("Last name starts with: ");
                    return patientSvc.findByLastNamePrefix(lastNamePrefix);
                case "2":
                    String firstNamePrefix = prompt("First name starts with: ");
                    return patientSvc.findByFirstNamePrefix(firstNamePrefix);
                case "3":
                    String phone = prompt("Phone number (exact; 7-15 digits): ");
                    return patientSvc.findByPhone(phone);
                case "4":
                    int minAge = Integer.parseInt(prompt("Minimum age: "));
                    int maxAge = Integer.parseInt(prompt("Maximum age: "));
                    return patientSvc.filterByAgeRange(minAge, maxAge);
                case "5":
                    String startDate = prompt("Start date (dd-MM-yyyy HH:mm): ");
                    String endDate = prompt("End date (dd-MM-yyyy HH:mm): ");
                    return patientSvc.filterByCreatedAtRange(startDate, endDate);
                case "0":
                default:
                    return patientSvc.listAllUnsorted();
            }
        } catch (Exception e) {
            System.err.println("Error in filter: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Patient> applyPatientSort(List<Patient> list) {
        if (list.isEmpty()) return list;

        System.out.println("\n--- Sort Patients By ---");
        System.out.println("1) ID");
        System.out.println("2) First Name");
        System.out.println("3) Last Name");
        System.out.println("4) Full Name");
        System.out.println("5) Age");
        System.out.println("6) Phone Number");
        System.out.println("7) Created Date");
        System.out.println("8) Updated Date");
        System.out.println("0) No Sort (as is)");
        String c = prompt("Select sort: ");

        PatientService.PatientSortField field;
        try {
            switch (c) {
                case "1": field = PatientService.PatientSortField.ID; break;
                case "2": field = PatientService.PatientSortField.FIRST_NAME; break;
                case "3": field = PatientService.PatientSortField.LAST_NAME; break;
                case "4": field = PatientService.PatientSortField.FULL_NAME; break;
                case "5": field = PatientService.PatientSortField.AGE; break;
                case "6": field = PatientService.PatientSortField.PHONE; break;
                case "7": field = PatientService.PatientSortField.CREATED_AT; break;
                case "8": field = PatientService.PatientSortField.UPDATED_AT; break;
                case "0":
                default: return list;
            }

            System.out.println("Sort direction: 1) Ascending  2) Descending");
            String dir = prompt("Select: ");
            EntitySorter.Direction direction =
                dir.equals("2") ? EntitySorter.Direction.DESC : EntitySorter.Direction.ASC;

            return patientSvc.sort(list, field, direction);
        } catch (Exception e) {
            System.err.println("Error in sort: " + e.getMessage());
            return list;
        }
    }

    private List<Doctor> applyDoctorFilter() {
        System.out.println("\n--- Filter Doctors By ---");
        System.out.println("1) Last Name starts with");
        System.out.println("2) First Name starts with");
        System.out.println("3) Phone Number (exact)");
        System.out.println("4) Specialty (contains)");
        System.out.println("5) Created Date Range");
        System.out.println("0) No Filter (all doctors)");
        String c = prompt("Select filter: ");
        try {
            switch (c) {
                case "1":
                    String lastNamePrefix = prompt("Last name starts with: ");
                    return doctorSvc.findByLastNamePrefix(lastNamePrefix);
                case "2":
                    String firstNamePrefix = prompt("First name starts with: ");
                    return doctorSvc.findByFirstNamePrefix(firstNamePrefix);
                case "3":
                    String phone = prompt("Phone number (exact; 7-15 digits): ");
                    return doctorSvc.findByPhone(phone);
                case "4":
                    String specialty = prompt("Specialty contains: ");
                    return doctorSvc.findBySpecialty(specialty);
                case "5":
                    String startDate = prompt("Start date (dd-MM-yyyy HH:mm): ");
                    String endDate = prompt("End date (dd-MM-yyyy HH:mm): ");
                    return doctorSvc.filterByCreatedAtRange(startDate, endDate);
                case "0":
                default:
                    return doctorSvc.listAllUnsorted();
            }
        } catch (Exception e) {
            System.err.println("Error in filter: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Doctor> applyDoctorSort(List<Doctor> list) {
        if (list.isEmpty()) return list;

        System.out.println("\n--- Sort Doctors By ---");
        System.out.println("1) ID");
        System.out.println("2) First Name");
        System.out.println("3) Last Name");
        System.out.println("4) Full Name");
        System.out.println("5) Specialty");
        System.out.println("6) Phone Number");
        System.out.println("7) Created Date");
        System.out.println("8) Updated Date");
        System.out.println("0) No Sort (as is)");
        String c = prompt("Select sort: ");

        DoctorService.DoctorSortField field;
        try {
            switch (c) {
                case "1": field = DoctorService.DoctorSortField.ID; break;
                case "2": field = DoctorService.DoctorSortField.FIRST_NAME; break;
                case "3": field = DoctorService.DoctorSortField.LAST_NAME; break;
                case "4": field = DoctorService.DoctorSortField.FULL_NAME; break;
                case "5": field = DoctorService.DoctorSortField.SPECIALTY; break;
                case "6": field = DoctorService.DoctorSortField.PHONE; break;
                case "7": field = DoctorService.DoctorSortField.CREATED_AT; break;
                case "8": field = DoctorService.DoctorSortField.UPDATED_AT; break;
                case "0":
                default: return list;
            }

            System.out.println("Sort direction: 1) Ascending  2) Descending");
            String dir = prompt("Select: ");
            EntitySorter.Direction direction =
                dir.equals("2") ? EntitySorter.Direction.DESC : EntitySorter.Direction.ASC;

            return doctorSvc.sort(list, field, direction);
        } catch (Exception e) {
            System.err.println("Error in sort: " + e.getMessage());
            return list;
        }
    }

    private List<Appointment> applyAppointmentFilter() {
        System.out.println("\n--- Filter Appointments By ---");
        System.out.println("1) Patient ID");
        System.out.println("2) Doctor ID");
        System.out.println("3) Status");
        System.out.println("4) Date Range");
        System.out.println("5) Created Date Range");
        System.out.println("0) No Filter (all appointments)");
        String c = prompt("Select filter: ");
        try {
            switch (c) {
                case "1":
                    long patientId = Long.parseLong(prompt("Patient ID: "));
                    return apptSvc.findByPatientId(patientId);
                case "2":
                    long doctorId = Long.parseLong(prompt("Doctor ID: "));
                    return apptSvc.findByDoctorId(doctorId);
                case "3":
                    String status = prompt("Status (" + APPOINTMENT_STATUS_OPTIONS + "): ");
                    return apptSvc.findByStatus(status);
                case "4":
                    String startDate = prompt("Start date (dd-MM-yyyy): ");
                    String endDate = prompt("End date (dd-MM-yyyy): ");
                    return apptSvc.findByDateRange(startDate, endDate);
                case "5":
                    String startDateTime = prompt("Start date/time (dd-MM-yyyy HH:mm or HH:mm:ss): ");
                    String endDateTime = prompt("End date/time (dd-MM-yyyy HH:mm or HH:mm:ss): ");
                    return apptSvc.findByCreatedAtRange(startDateTime, endDateTime);
                case "0":
                default:
                    return new ArrayList<>(apptSvc.listOfAppointments());
            }
        } catch (Exception e) {
            System.err.println("Error in filter: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Appointment> applyAppointmentSort(List<Appointment> list) {
        if (list.isEmpty()) return list;

        System.out.println("\n--- Sort Appointments By ---");
        System.out.println("1) ID");
        System.out.println("2) Patient ID");
        System.out.println("3) Doctor ID");
        System.out.println("4) Date/Time");
        System.out.println("5) Duration");
        System.out.println("6) Status");
        System.out.println("7) Created Date");
        System.out.println("8) Updated Date");
        System.out.println("0) No Sort (as is)");
        String c = prompt("Select sort: ");

        AppointmentService.AppointmentSortField field;
        try {
            switch (c) {
                case "1": field = AppointmentService.AppointmentSortField.ID; break;
                case "2": field = AppointmentService.AppointmentSortField.PATIENT_ID; break;
                case "3": field = AppointmentService.AppointmentSortField.DOCTOR_ID; break;
                case "4": field = AppointmentService.AppointmentSortField.START_DATE_TIME; break;
                case "5": field = AppointmentService.AppointmentSortField.DURATION_MIN; break;
                case "6": field = AppointmentService.AppointmentSortField.STATUS; break;
                case "7": field = AppointmentService.AppointmentSortField.CREATED_AT; break;
                case "8": field = AppointmentService.AppointmentSortField.UPDATED_AT; break;
                case "0":
                default: return list;
            }

            System.out.println("Sort direction: 1) Ascending  2) Descending");
            String dir = prompt("Select: ");
            EntitySorter.Direction direction =
                dir.equals("2") ? EntitySorter.Direction.DESC : EntitySorter.Direction.ASC;

            return apptSvc.sort(list, field, direction);
        } catch (Exception e) {
            System.err.println("Error in sort: " + e.getMessage());
            return list;
        }
    }

    // ===================== BASIC CREATE / FORMAT HELPERS (restored) =====================
    private void createPatient() {
        String fn = prompt("First name (required): ");
        String ln = prompt("Last name (required): ");
        int age = Integer.parseInt(prompt("Age (1-120): "));
        String phone = prompt("Phone (7-15 digits): ");
        try {
            Patient p = patientSvc.createPatient(fn, ln, age, phone);
            System.out.println("Created patient ID=" + p.getId());
        } catch (Exception e) {
            System.err.println("Failed: " + e.getMessage());
        }
    }

    private void createDoctor() {
        String fn = prompt("First name (required): ");
        String ln = prompt("Last name (required): ");
        String spec = prompt("Specialty (required): ");
        String phone = prompt("Phone (7-15 digits): ");
        try {
            Doctor d = doctorSvc.createDoctor(fn, ln, spec, phone);
            System.out.println("Created doctor ID=" + d.getId());
        } catch (Exception e) {
            System.err.println("Failed: " + e.getMessage());
        }
    }

    private void createAppointment() {
        try {
            // Select patient (allow blank to search)
            Patient p;
            String pidRaw = prompt("Patient ID (leave blank to search): ").trim();
            if (pidRaw.isEmpty()) {
                p = selectPatient();
                if (p == null) { System.out.println("Cancelled."); return; }
            } else {
                p = patientSvc.getPatientById(Long.parseLong(pidRaw));
            }

            // Select doctor (allow blank to search)
            Doctor d;
            String didRaw = prompt("Doctor ID (leave blank to search): ").trim();
            if (didRaw.isEmpty()) {
                d = selectDoctor();
                if (d == null) { System.out.println("Cancelled."); return; }
            } else {
                d = doctorSvc.getDoctorById(Long.parseLong(didRaw));
            }

            String date = prompt("Date (dd-MM-yyyy): ");
            String time = prompt("Time (HH:mm, 24h): ");
            String dur = prompt("Duration (HH:mm): ");
            String status = prompt("Status (" + APPOINTMENT_STATUS_OPTIONS + "): ");
            String location = promptOptional("Location (optional; blank): ");
            String follow = promptOptional("Follow-up date (dd-MM-yyyy; blank): ");
            String reason = promptOptional("Reason (optional; blank): ");
            String notes = promptOptional("Notes (optional; blank): ");

            Appointment a = apptSvc.createAppointment(p.getId(), d.getId(), date, time, dur, status,
                    emptyToNull(location), emptyToNull(follow), emptyToNull(reason), emptyToNull(notes));
            System.out.println("Created appointment ID=" + a.getId());
        } catch (Exception e) {
            System.err.println("Failed: " + e.getMessage());
        }
    }

    private String fmtAppt(Appointment a) {
        if (a == null) return "(null)";

        // Get patient and doctor names to include in the display
        String patientName = "(Unknown patient)";
        String doctorName = "(Unknown doctor)";
        try {
            Patient p = patientSvc.getPatientById(a.getPatientId());
            patientName = p.getFirstName() + " " + p.getLastName();
        } catch (Exception ignored) {}

        try {
            Doctor d = doctorSvc.getDoctorById(a.getDoctorId());
            doctorName = "Dr. " + d.getFirstName() + " " + d.getLastName() + " (" + d.getSpecialty() + ")";
        } catch (Exception ignored) {}

        // Format with more descriptive information
        return a.getId() + ": " + a.getAppointmentDate() + " " + a.getAppointmentTime() + " - " + a.getDuration() +
               " | Patient: " + patientName + " (ID:" + a.getPatientId() + ")" +
               " | Doctor: " + doctorName + " (ID:" + a.getDoctorId() + ")" +
               " | Status: " + a.getStatus() +
               (a.getLocation() != null && !a.getLocation().isBlank() ? " | Location: " + a.getLocation() : "") +
               (a.getReason() != null && !a.getReason().isBlank() ? " | Reason: " + a.getReason() : "");
    }

    private void listWalkInQueue() {
        List<CheckIn> q = checkInSvc.listWalkInQueue();
        if (q.isEmpty()) { System.out.println("Queue empty"); return; }
        int pos = 1;
        for (CheckIn c : q) {
            System.out.println(pos++ + ") CI=" + c.getId() + " appt=" + c.getAppointmentId() +
                    " patient=" + c.getPatientId() + " prio=" + c.getPriority() + " status=" + c.getStatus());
        }
    }

    // No-op hybrid prompt to keep code compiling while disabled
    private String hybridPrompt(String summary) { return "P"; }

    // ---------- Check-In History viewing / filtering (already referenced) ----------
    private void viewCheckInHistoryFlow() {
        var all = new ArrayList<>(checkInSvc.listHistory());
        if (all.isEmpty()) { System.out.println("No history records yet."); return; }
        List<repo.CheckInHistoryRepository.Record> list = all;
        while (true) {
            System.out.println("\n--- Check-In History ---");
            printHistory(list);
            System.out.println("Options: 1) Filter  2) Flip order  0) Back");
            String ch = prompt("Select: ");
            switch (ch) {
                case "1": list = applyHistoryFilter(all); break;
                case "2": {
                    List<repo.CheckInHistoryRepository.Record> rev = new ArrayList<>(list);
                    java.util.Collections.reverse(rev); list = rev; break; }
                case "0": return;
                default: System.out.println("Unknown option");
            }
        }
    }

    // New: printer for history rows
    private void printHistory(java.util.List<repo.CheckInHistoryRepository.Record> list) {
        if (list == null || list.isEmpty()) { System.out.println("(no records)"); return; }
        int i = 1;
        for (repo.CheckInHistoryRepository.Record r : list) {
            String wi = r.walkIn ? "walk-in prio=" + r.priority : "scheduled";
            String when = r.updatedAt != null && !r.updatedAt.isBlank() ? r.updatedAt : r.checkedInAt;
            System.out.println(i++ + ") CI=" + r.checkInId + " appt=" + r.appointmentId +
                    " patient=" + r.patientId + " doctor=" + r.doctorId +
                    " status=" + r.status + " (" + wi + ")" +
                    (r.desk != null && !r.desk.isBlank() ? (" @" + r.desk) : "") +
                    (r.notes != null && !r.notes.isBlank() ? (" | notes=" + r.notes) : "") +
                    " | when=" + (when == null ? "?" : when));
        }
        System.out.println("Total: " + list.size());
    }

    // New: simple filter menu for history list
    private java.util.List<repo.CheckInHistoryRepository.Record> applyHistoryFilter(java.util.List<repo.CheckInHistoryRepository.Record> all) {
        if (all == null) return new java.util.ArrayList<>();
        System.out.println("\nHistory Filter: 1) By Patient ID  2) By Doctor ID  3) Walk-In only  4) By Date/Time range  0) Clear");
        String c = prompt("Select: ");
        try {
            switch (c) {
                case "1": {
                    long pid = Long.parseLong(prompt("Patient ID: "));
                    java.util.List<repo.CheckInHistoryRepository.Record> out = new java.util.ArrayList<>();
                    for (var r : all) if (r.patientId == pid) out.add(r);
                    return out;
                }
                case "2": {
                    long did = Long.parseLong(prompt("Doctor ID: "));
                    java.util.List<repo.CheckInHistoryRepository.Record> out = new java.util.ArrayList<>();
                    for (var r : all) if (r.doctorId == did) out.add(r);
                    return out;
                }
                case "3": {
                    java.util.List<repo.CheckInHistoryRepository.Record> out = new java.util.ArrayList<>();
                    for (var r : all) if (r.walkIn) out.add(r);
                    return out;
                }
                case "4": {
                    String start = prompt("Start (dd-MM-yyyy HH:mm[:ss]): ");
                    String end = prompt("End (dd-MM-yyyy HH:mm[:ss]): ");
                    var from = util.TimeUtil.parseDateTime(start.trim());
                    var to = util.TimeUtil.parseDateTime(end.trim());
                    if (to.isBefore(from)) { System.out.println("End before start."); return all; }
                    java.util.List<repo.CheckInHistoryRepository.Record> out = new java.util.ArrayList<>();
                    for (var r : all) {
                        String s = r.updatedAt != null && !r.updatedAt.isBlank() ? r.updatedAt : r.checkedInAt;
                        if (s == null || s.isBlank()) continue;
                        try {
                            var t = util.TimeUtil.parseDateTime(s.trim());
                            if (!t.isBefore(from) && !t.isAfter(to)) out.add(r);
                        } catch (Exception ignored) { }
                    }
                    return out;
                }
                default:
                    return all;
            }
        } catch (Exception e) {
            System.out.println("Invalid filter input.");
            return all;
        }
    }

    // ---------- Detailed view for a single Check-In (shared by walk-in and scheduled) ----------
    private void printCheckInDetailed(CheckIn c) {
        if (c == null) { System.out.println("(no check-in)"); return; }
        Appointment a = null; Patient p = null; Doctor d = null;
        try { a = apptSvc.getAppointmentById(c.getAppointmentId()); } catch (Exception ignored) {}
        try { p = patientSvc.getPatientById(c.getPatientId()); } catch (Exception ignored) {}
        if (a != null) { try { d = doctorSvc.getDoctorById(a.getDoctorId()); } catch (Exception ignored) {} }
        String pn = (p==null? ("P"+c.getPatientId()) : (p.getFirstName()+" "+p.getLastName()+" ("+p.getAge()+")"));
        String dn = (d==null? "?" : ("Dr. "+d.getFirstName()+" "+d.getLastName()));
        String ap = (a==null? "?" : (a.getAppointmentDate()+" "+a.getAppointmentTime()+" with "+dn));
        String wi = c.isWalkIn()? (" walkIn prio="+c.getPriority()) : " scheduled";
        System.out.println("CI="+c.getId()+" status="+c.getStatus()+ wi +
                " | appt="+c.getAppointmentId()+" ("+ap+")"+
                " | patient="+pn+
                (c.getDesk()!=null? (" @"+c.getDesk()): "")+
                (c.getNotes()!=null && !c.getNotes().isBlank()? (" | notes="+c.getNotes()): "")+
                " | checkedInAt="+c.getCheckedInAt()+
                (c.getCompletedAt()!=null? (" completedAt="+c.getCompletedAt()): ""));
    }

    // -------- Scheduled (Appointment-based) Check-In Submenu --------
    private void scheduledCheckInMenu() {
        while (true) {
            System.out.println("\n--- Scheduled Check-In ---");
            System.out.println("1) Check-In Existing Appointment");
            System.out.println("2) Call Next Scheduled");
            System.out.println("3) List Scheduled Queue");
            System.out.println("4) Update Scheduled Check-In");
            System.out.println("0) Back");
            String c = prompt("Select: ");
            try {
                switch (c) {
                    case "1":
                        appointmentCheckInFlow();
                        break;
                    case "2": {
                        CheckIn next = checkInSvc.callNextScheduled();
                        if (next == null) System.out.println("Scheduled queue empty");
                        else System.out.println("Called CI=" + next.getId() + " appt=" + next.getAppointmentId());
                        break;
                    }
                    case "3":
                        listScheduledQueue();
                        break;
                    case "4":
                        updateScheduledCheckInFlow();
                        break;
                    case "0": return;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }

    private void listScheduledQueue() {
        List<CheckIn> q = checkInSvc.listScheduledQueue();
        if (q.isEmpty()) { System.out.println("Queue empty"); return; }
        int pos = 1;
        for (CheckIn c : q) {
            System.out.println(pos++ + ") CI=" + c.getId() + " appt=" + c.getAppointmentId() +
                    " patient=" + c.getPatientId() + " status=" + c.getStatus());
        }
    }

    // -------- Update flow for Scheduled Check-Ins (mirrors Walk-In update, no priority) --------
    private void updateScheduledCheckInFlow() {
        boolean loop = true;
        while (loop) {
            System.out.println("\nUpdate Scheduled Check-In (each action selects a scheduled check-in)");
            System.out.println("1) Call Patient (complete & archive)");
            System.out.println("2) Update Desk");
            System.out.println("3) Update Notes");
            System.out.println("4) Show Details");
            System.out.println("0) Back");
            String ch = prompt("Select: ");
            try {
                switch (ch) {
                    case "1": {
                        CheckIn c = selectScheduledCheckIn(); if (c==null) break;
                        changeScheduledStatus(c.getId());
                        System.out.println("Status updated.");
                        break; }
                    case "2": {
                        CheckIn c = selectScheduledCheckIn(); if (c==null) break;
                        checkInSvc.updateDesk(c.getId(), prompt("Desk (optional; blank): "));
                        System.out.println("Desk updated.");
                        break; }
                    case "3": {
                        CheckIn c = selectScheduledCheckIn(); if (c==null) break;
                        checkInSvc.updateNotes(c.getId(), prompt("Notes (optional; blank): "));
                        System.out.println("Notes updated.");
                        break; }
                    case "4": {
                        CheckIn c = selectScheduledCheckIn(); if (c==null) break; printCheckInDetailed(c); break; }
                    case "0": loop = false; break;
                    default: System.out.println("Unknown option");
                }
            } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }
    }

    private void changeScheduledStatus(long id) {
        System.out.println("Status Options: 1) Call patient (complete)  2) Complete now  0) Cancel");
        String opt = prompt("Select: ");
        switch (opt) {
            case "1": checkInSvc.markCalled(id); break;
            case "2": checkInSvc.markCompleted(id); break;
            default: System.out.println("No change");
        }
    }

    private CheckIn selectScheduledCheckIn() {
        List<CheckIn> list = checkInSvc.listAllScheduled();
        if (list.isEmpty()) { System.out.println("No scheduled check-ins available"); return null; }
        // order by appointment start asc then checkedInAt asc (like scheduled queue)
        list.sort((a,b) -> {
            try {
                Appointment aa = apptSvc.getAppointmentById(a.getAppointmentId());
                Appointment bb = apptSvc.getAppointmentById(b.getAppointmentId());
                var as = util.TimeUtil.combine(aa.getAppointmentDate(), aa.getAppointmentTime());
                var bs = util.TimeUtil.combine(bb.getAppointmentDate(), bb.getAppointmentTime());
                int cmp = as.compareTo(bs);
                if (cmp != 0) return cmp;
                var at = util.TimeUtil.parseDateTime(a.getCheckedInAt());
                var bt = util.TimeUtil.parseDateTime(b.getCheckedInAt());
                return at.compareTo(bt);
            } catch (Exception e) { return 0; }
        });
        int i=1; for (CheckIn c : list) { System.out.println(i++ + ") CI=" + c.getId() + " appt=" + c.getAppointmentId() + " status=" + c.getStatus()); }
        String sel = prompt("Select # (0 cancel): ");
        try { int idx = Integer.parseInt(sel); if (idx<=0 || idx>list.size()) return null; return list.get(idx-1);} catch(Exception e){ return null; }
    }
}
