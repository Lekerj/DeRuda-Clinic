package util;

import model.Appointment;
import model.Patient;
import model.Doctor;
import repo.InMemoryStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Flat-file persistence for clinic entities.
 */
public final class FileIO {

    private FileIO() {}

    // Patient persistence format
    public static final String PATIENT_FORMAT = "id|firstname|lastname|age|phonenumber|lastupdated|createdat";

    // Doctor persistence format
    public static final String DOCTOR_FORMAT = "id|firstname|lastname|specialty|phonenumber|lastupdated|createdat";

    // Appointment persistence format
    public static final String APPOINTMENT_FORMAT = "id|patientId|doctorId|appointmentDate|appointmentTime|duration|status|location|followUpDate|reason|notes|lastupdated|createdat";

    private static final char SEP = '|';
    private static final int PATIENT_FIELD_COUNT = 7;
    private static final int DOCTOR_FIELD_COUNT = 7;
    private static final int APPOINTMENT_FIELD_COUNT = 13;

    // ---------- Patient Methods ----------

    /**
     * Loads patients from file; malformed lines are skipped with a warning.
     * Ensures InMemoryStore next ID is advanced (if provided).
     */
    public static List<Patient> loadPatients(Path file, InMemoryStore storeOrNull) {
        List<Patient> patients = new ArrayList<>();
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);

        if (!Files.exists(file)) return patients;

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String raw;
            int lineNo = 0;
            while ((raw = br.readLine()) != null) {
                lineNo++;
                String line = raw;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length != PATIENT_FIELD_COUNT) {
                    System.err.println("[FileIO] Bad token count at line " + lineNo + ": got " + parts.length + " -> " + Arrays.toString(parts));
                    continue;
                }
                for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();

                try {
                    long id = parsePositiveLong(parts[0], "id");
                    String first = parts[1];
                    String last = parts[2];
                    int age = parseAge(parts[3]);
                    String phone = parts[4];
                    String updatedAt = parts[5]; // index for updatedAt per spec
                    String createdAt = parts[6]; // index for createdAt per spec

                    updatedAt = TimeUtil.normalizeDateTime(parts[5]);
                    createdAt = TimeUtil.normalizeDateTime(parts[6]);

                    Patient p = new Patient(first, last, age, id, phone, createdAt, updatedAt);
                    patients.add(p);

                    if (storeOrNull != null) storeOrNull.ensureNextPatientIdAbove(id);
                } catch (Exception ex) {
                    warn("Skipping malformed patient line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            warn("Error reading patients: " + e.getMessage());
        }
        return patients;
    }

    /**
     * Loads patients from file with default path.
     */
    public static List<Patient> loadPatients(String filePath) {
        return loadPatients(Paths.get(filePath), null);
    }

    /**
     * Saves full patient list (overwrite).
     */
    public static void savePatients(List<Patient> patients, Path file) {
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            if (patients != null) {
                for (Patient p : patients) {
                    bw.write(serializePatient(p));
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            warn("Error saving patients: " + e.getMessage());
        }
    }

    /**
     * Upsert (create or replace) a single patient line by ID without rewriting unrelated lines unnecessarily.
     */
    public static void upsertPatientLine(Patient p, String filePath) {
        upsertPatientLine(p, Paths.get(filePath));
    }

    /**
     * Upsert (create or replace) a single patient line by ID without rewriting unrelated lines unnecessarily.
     */
    public static void upsertPatientLine(Patient p, Path file) {
        if (p == null) throw new IllegalArgumentException("Patient cannot be null");
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);

        List<String> lines = readAllLines(file);
        String idStr = Long.toString(p.getId());
        String serialized = serializePatient(p);
        boolean replaced = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            int pipe = line.indexOf(SEP);
            if (pipe <= 0) continue;
            if (line.substring(0, pipe).equals(idStr)) {
                lines.set(i, serialized);
                replaced = true;
                break;
            }
        }
        if (!replaced) lines.add(serialized);
        writeAllLines(file, lines);
    }

    /**
     * Deletes a patient line by ID.
     */
    public static void deletePatientLine(long id, String filePath) {
        deletePatientLine(id, Paths.get(filePath));
    }

    /**
     * Deletes a patient line by ID.
     */
    public static void deletePatientLine(long id, Path file) {
        if (id <= 0) throw new IllegalArgumentException("Invalid id");
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        if (!Files.exists(file)) return;
        List<String> lines = readAllLines(file);
        String idStr = Long.toString(id);
        boolean changed = lines.removeIf(l -> {
            String t = l.trim();
            if (t.isEmpty()) return false;
            int pipe = t.indexOf(SEP);
            return pipe > 0 && t.substring(0, pipe).equals(idStr);
        });
        if (changed) writeAllLines(file, lines);
    }

    // ---------- Doctor Methods ----------

    /**
     * Loads doctors from file; malformed lines are skipped with a warning.
     * Ensures InMemoryStore next ID is advanced (if provided).
     */
    public static List<Doctor> loadDoctors(Path file, InMemoryStore storeOrNull) {
        List<Doctor> doctors = new ArrayList<>();
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);

        if (!Files.exists(file)) return doctors;

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String raw;
            int lineNo = 0;
            while ((raw = br.readLine()) != null) {
                lineNo++;
                String line = raw;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length != DOCTOR_FIELD_COUNT) {
                    System.err.println("[FileIO] Bad token count at line " + lineNo + ": got " + parts.length + " -> " + Arrays.toString(parts));
                    continue;
                }
                for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();

                try {
                    long id = parsePositiveLong(parts[0], "id");
                    String first = parts[1];
                    String last = parts[2];
                    String specialty = parts[3];
                    String phone = parts[4];
                    String updatedAt = parts[5]; // index for updatedAt per spec

                    String createdAt = parts[6]; // index for createdAt per spec
                    updatedAt = TimeUtil.normalizeDateTime(parts[5]);
                    createdAt = TimeUtil.normalizeDateTime(parts[6]);

                    Doctor d = new Doctor(first, last, id, phone, specialty, createdAt, updatedAt);
                    doctors.add(d);

                    if (storeOrNull != null) storeOrNull.ensureNextDoctorIdAbove(id);
                } catch (Exception ex) {
                    warn("Skipping malformed doctor line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            warn("Error reading doctors: " + e.getMessage());
        }
        return doctors;
    }

    /**
     * Loads doctors from file with default path.
     */
    public static List<Doctor> loadDoctors(String filePath) {
        return loadDoctors(Paths.get(filePath), null);
    }

    /**
     * Saves full doctor list (overwrite).
     */
    public static void saveDoctors(List<Doctor> doctors, Path file) {
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            if (doctors != null) {
                for (Doctor d : doctors) {
                    bw.write(serializeDoctor(d));
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            warn("Error saving doctors: " + e.getMessage());
        }
    }

    /**
     * Upsert (create or replace) a single doctor line by ID without rewriting unrelated lines unnecessarily.
     */
    public static void upsertDoctorLine(Doctor d, String filePath) {
        upsertDoctorLine(d, Paths.get(filePath));
    }

    /**
     * Upsert (create or replace) a single doctor line by ID without rewriting unrelated lines unnecessarily.
     */
    public static void upsertDoctorLine(Doctor d, Path file) {
        if (d == null) throw new IllegalArgumentException("Doctor cannot be null");
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);

        List<String> lines = readAllLines(file);
        String idStr = Long.toString(d.getId());
        String serialized = serializeDoctor(d);
        boolean replaced = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            int pipe = line.indexOf(SEP);
            if (pipe <= 0) continue;
            if (line.substring(0, pipe).equals(idStr)) {
                lines.set(i, serialized);
                replaced = true;
                break;
            }
        }
        if (!replaced) lines.add(serialized);
        writeAllLines(file, lines);
    }

    /**
     * Deletes a doctor line by ID.
     */
    public static void deleteDoctorLine(long id, String filePath) {
        deleteDoctorLine(id, Paths.get(filePath));
    }

    /**
     * Deletes a doctor line by ID.
     */
    public static void deleteDoctorLine(long id, Path file) {
        if (id <= 0) throw new IllegalArgumentException("Invalid id");
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        if (!Files.exists(file)) return;
        List<String> lines = readAllLines(file);
        String idStr = Long.toString(id);
        boolean changed = lines.removeIf(l -> {
            String t = l.trim();
            if (t.isEmpty()) return false;
            int pipe = t.indexOf(SEP);
            return pipe > 0 && t.substring(0, pipe).equals(idStr);
        });
        if (changed) writeAllLines(file, lines);
    }

    // ---------- Appointment Methods ----------

    /**
     * Loads appointments from file; malformed lines are skipped with a warning.
     * Ensures InMemoryStore next ID is advanced (if provided).
     */
    public static List<Appointment> loadAppointments(Path file, InMemoryStore storeOrNull) {
        List<Appointment> appointments = new ArrayList<>();
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);

        if (!Files.exists(file)) return appointments;

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String raw;
            int lineNo = 0;
            while ((raw = br.readLine()) != null) {
                lineNo++;
                String line = raw;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length != APPOINTMENT_FIELD_COUNT) {
                    System.err.println("[FileIO] Bad token count at line " + lineNo + ": got " + parts.length + " -> " + Arrays.toString(parts));
                    continue;
                }
                for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();

                try {
                    long id = parsePositiveLong(parts[0], "id");
                    long patientId = parsePositiveLong(parts[1], "patientId");
                    long doctorId = parsePositiveLong(parts[2], "doctorId");
                    String appointmentDate = parts[3];
                    String appointmentTime = parts[4];
                    String duration = parts[5];
                    String status = parts[6];

                    // Normalize from the correct fields (fix: was using parts[5]/[6])
                    appointmentDate = TimeUtil.normalizeDate(parts[3]);
                    appointmentTime = TimeUtil.normalizeTime(parts[4]);

                    String location = parts[7];
                    String followUpRaw = parts[8];
                    String followUpDate = followUpRaw.isEmpty() ? null : followUpRaw;
                    String reason = parts[9];
                    String notes = parts[10];
                    String updatedAt = parts[11]; // index for updatedAt per spec
                    String createdAt = parts[12]; // index for createdAt per spec

                    updatedAt=TimeUtil.normalizeDateTime(parts[11]);
                    createdAt=TimeUtil.normalizeDateTime(parts[12]);

                    Appointment a = new Appointment(id, patientId, doctorId,
                            appointmentDate, appointmentTime, duration,
                            status, location, followUpDate,
                            reason, notes, createdAt, updatedAt);
                    appointments.add(a);

                    if (storeOrNull != null) storeOrNull.ensureNextAppointmentIdAbove(id);
                } catch (Exception ex) {
                    warn("Skipping malformed appointment line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            warn("Error reading appointments: " + e.getMessage());
        }
        return appointments;
    }

    /**
     * Loads appointments from file with default path.
     */
    public static List<Appointment> loadAppointments(String filePath) {
        return loadAppointments(Paths.get(filePath), null);
    }

    /**
     * Saves full appointment list (overwrite).
     */
    public static void saveAppointments(List<Appointment> appointments, Path file) {
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            if (appointments != null) {
                for (Appointment a : appointments) {
                    bw.write(serializeAppointment(a));
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            warn("Error saving appointments: " + e.getMessage());
        }
    }

    /**
     * Upsert (create or replace) a single appointment line by ID without rewriting unrelated lines unnecessarily.
     */
    public static void upsertAppointmentLine(Appointment a, String filePath) {
        upsertAppointmentLine(a, Paths.get(filePath));
    }

    /**
     * Upsert (create or replace) a single appointment line by ID without rewriting unrelated lines unnecessarily.
     */
    public static void upsertAppointmentLine(Appointment a, Path file) {
        if (a == null) throw new IllegalArgumentException("Appointment cannot be null");
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        ensureParentDir(file);

        List<String> lines = readAllLines(file);
        String idStr = Long.toString(a.getId());
        String serialized = serializeAppointment(a);
        boolean replaced = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            int pipe = line.indexOf(SEP);
            if (pipe <= 0) continue;
            if (line.substring(0, pipe).equals(idStr)) {
                lines.set(i, serialized);
                replaced = true;
                break;
            }
        }
        if (!replaced) lines.add(serialized);
        writeAllLines(file, lines);
    }

    /**
     * Deletes an appointment line by ID.
     */
    public static void deleteAppointmentLine(long id, String filePath) {
        deleteAppointmentLine(id, Paths.get(filePath));
    }

    /**
     * Deletes an appointment line by ID.
     */
    public static void deleteAppointmentLine(long id, Path file) {
        if (id <= 0) throw new IllegalArgumentException("Invalid id");
        if (file == null) throw new IllegalArgumentException("File path cannot be null");
        if (!Files.exists(file)) return;
        List<String> lines = readAllLines(file);
        String idStr = Long.toString(id);
        boolean changed = lines.removeIf(l -> {
            String t = l.trim();
            if (t.isEmpty()) return false;
            int pipe = t.indexOf(SEP);
            return pipe > 0 && t.substring(0, pipe).equals(idStr);
        });
        if (changed) writeAllLines(file, lines);
    }

    private static String serializePatient(Patient p) {
        // Validation only; actual line comes from Patient.toString()
        checkNoDelimiter(p.getFirstName(), "firstName");
        checkNoDelimiter(p.getLastName(), "lastName");
        checkNoDelimiter(p.getPhoneNumber(), "phoneNumber");
        return p.toString(); // Uses model's canonical persistence format
    }

    private static String serializeDoctor(Doctor d) {
        // Validation only; actual line comes from Doctor.toString()
        checkNoDelimiter(d.getFirstName(), "firstName");
        checkNoDelimiter(d.getLastName(), "lastName");
        checkNoDelimiter(d.getSpecialty(), "specialty");
        checkNoDelimiter(d.getPhoneNumber(), "phoneNumber");
        return d.toString(); // Uses model's canonical persistence format
    }

    private static String serializeAppointment(Appointment a) {
        // Validation only; actual line comes from Appointment.toString()
        checkNoDelimiter(a.getStatus(), "status");
        checkNoDelimiter(a.getLocation(), "location");
        checkNoDelimiter(a.getReason(), "reason");
        checkNoDelimiter(a.getNotes(), "notes");
        return a.toString(); // Uses model's canonical persistence format
    }

    private static int parseAge(String s) {
        int age = Integer.parseInt(s);
        if (age < 1 || age > 120) throw new IllegalArgumentException("Age out of range: " + age);
        return age;
    }

    private static long parsePositiveLong(String s, String field) {
        long v = Long.parseLong(s);
        if (v <= 0) throw new IllegalArgumentException(field + " must be positive");
        return v;
    }

    private static void ensureParentDir(Path file) {
        Path parent = file.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot create directory: " + parent, e);
            }
        }
    }

    private static List<String> readAllLines(Path file) {
        try {
            if (!Files.exists(file)) return new ArrayList<>();
            return new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeAllLines(Path file, List<String> lines) {
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkNoDelimiter(String v, String field) {
        if (v != null && v.indexOf(SEP) >= 0) {
            throw new IllegalArgumentException(field + " contains illegal delimiter '|'");
        }
    }

    private static void warn(String msg) {
        System.err.println("[FileIO] " + msg);
    }


    /**
     * Resolves a data file under the project's src/data directory regardless of current working directory.
     * It searches up the directory tree for a folder containing "src/data" and returns the absolute path to the file.
     * Falls back to the provided relative path under src/data if not found, creating parent directories as needed.
     */
    public static Path resolveProjectDataPath(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName cannot be null or blank");
        }
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && cwd != null; i++) {
            Path candidateDir = cwd.resolve("src").resolve("data");
            if (Files.exists(candidateDir) && Files.isDirectory(candidateDir)) {
                return candidateDir.resolve(fileName).toAbsolutePath().normalize();
            }
            cwd = cwd.getParent();
        }
        // Fallback: assume relative src/data from current dir
        Path fallback = Paths.get("src", "data", fileName).toAbsolutePath().normalize();
        Path parent = fallback.getParent();
        if (parent != null) {
            try { Files.createDirectories(parent); } catch (IOException ignored) {}
        }
        return fallback;
    }
}
