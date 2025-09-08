package service;

import model.Appointment;
import model.CheckIn;
import repo.CheckInRepository;
import repo.CheckInHistoryRepository;
import repo.InMemoryStore;
import util.TimeUtil;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Service that manages patient check-in operations for the clinic.
 * Handles both scheduled appointments and walk-in patients.
 *
 * This service maintains queues of patients waiting to be seen,
 * tracks check-in states, and archives completed check-ins.
 */
public class CheckInService {

    private final InMemoryStore storage;
    private final AppointmentService apptSvc;
    private final CheckInRepository repo;
    private final CheckInHistoryRepository historyRepo;
    private final CheckInRepository.State state; // in-memory mutable snapshot

    /**
     * Creates a check-in service with default repositories.
     *
     * @param storage The in-memory data store for all entities
     * @param apptSvc The appointment service for appointment operations
     */
    public CheckInService(InMemoryStore storage, AppointmentService apptSvc) {
        this(storage, apptSvc, new CheckInRepository(), new CheckInHistoryRepository());
    }

    /**
     * Creates a check-in service with a custom check-in repository.
     *
     * @param storage The in-memory data store for all entities
     * @param apptSvc The appointment service for appointment operations
     * @param repo The repository for check-in persistence
     */
    public CheckInService(InMemoryStore storage, AppointmentService apptSvc, CheckInRepository repo) {
        this(storage, apptSvc, repo, new CheckInHistoryRepository());
    }

    /**
     * Creates a check-in service with custom repositories.
     *
     * @param storage The in-memory data store for all entities
     * @param apptSvc The appointment service for appointment operations
     * @param repo The repository for check-in persistence
     * @param historyRepo The repository for completed check-in records
     * @throws IllegalArgumentException if any parameter is null
     */
    public CheckInService(InMemoryStore storage, AppointmentService apptSvc, CheckInRepository repo, CheckInHistoryRepository historyRepo) {
        if (storage == null || apptSvc == null || repo == null || historyRepo == null) {
            throw new IllegalArgumentException("deps required");
        }
        this.storage = storage;
        this.apptSvc = apptSvc;
        this.repo = repo;
        this.historyRepo = historyRepo;
        this.state = repo.load();
        rebuildIndexesIfNeeded();
        sanitizeQueues();
    }

    // ---------------- Public API: Creation ----------------

    /**
     * Checks in a patient for an existing scheduled appointment.
     * The appointment must be in "Scheduled" status to be eligible for check-in.
     *
     * @param appointmentId ID of the existing appointment
     * @param desk Optional desk/location identifier where check-in occurred
     * @param notes Optional notes related to the check-in
     * @return The newly created check-in record
     * @throws IllegalArgumentException if appointment doesn't exist or ID is invalid
     * @throws IllegalStateException if appointment already has an active check-in or isn't in Scheduled status
     */
    public CheckIn checkInScheduled(long appointmentId, String desk, String notes) {
        requirePositive(appointmentId, "appointmentId");
        Appointment appt = storage.getAppointment(appointmentId);
        if (appt == null) throw new IllegalArgumentException("Appointment " + appointmentId + " does not exist");
        ensureApptEligibleForCheckIn(appt); // must be Scheduled
        ensureSingleActiveCheckInForAppointment(appointmentId);

        long id = nextId();
        CheckIn c = new CheckIn(id, appointmentId, appt.getPatientId(),
                "CheckedIn", desk, notes, false, 0);
        state.checkIns.put(id, c);
        indexNew(c);
        // Update appointment to Checked In
        safeUpdateAppointmentStatus(appointmentId, "Checked In");
        // enqueue into scheduled queue
        orderedEnqueueScheduled(c);
        save();
        return c;
    }

    /**
     * Creates a new walk-in check-in with a dynamically generated appointment.
     * Walk-ins have a priority value that determines their queue position.
     *
     * @param patientId ID of the patient
     * @param doctorId ID of the assigned doctor
     * @param date_ddMMyyyy Appointment date in dd-MM-yyyy format
     * @param time_HHmm Appointment time in HH:mm format
     * @param duration_HHmm Appointment duration in HH:mm format
     * @param priority Priority level (higher values = higher priority)
     * @param desk Optional desk/location identifier where check-in occurred
     * @param reason Optional reason for the visit
     * @param notes Optional notes related to the check-in
     * @return The newly created check-in record
     * @throws IllegalArgumentException if any ID is invalid or referenced entities don't exist
     */
    public CheckIn checkInWalkIn(long patientId,
                                 long doctorId,
                                 String date_ddMMyyyy,
                                 String time_HHmm,
                                 String duration_HHmm,
                                 int priority,
                                 String desk,
                                 String reason,
                                 String notes) {
        requirePositive(patientId, "patientId");
        requirePositive(doctorId, "doctorId");
        if (!storage.hasPatient(patientId)) throw new IllegalArgumentException("Patient " + patientId + " does not exist");
        if (!storage.hasDoctor(doctorId)) throw new IllegalArgumentException("Doctor " + doctorId + " does not exist");
        if (priority < 0) throw new IllegalArgumentException("Priority must be >= 0");

        // Create a new appointment via AppointmentService (status Scheduled, no location/followUp)
        Appointment appt = apptSvc.createAppointment(patientId, doctorId,
                date_ddMMyyyy, time_HHmm, duration_HHmm,
                "Scheduled", null, null,
                reason, notes);

        long appointmentId = appt.getId();
        ensureSingleActiveCheckInForAppointment(appointmentId); // (should be none)

        long id = nextId();
        CheckIn c = new CheckIn(id, appointmentId, patientId,
                "CheckedIn", desk, notes, true, priority);
        state.checkIns.put(id, c);
        state.walkInAppointmentIds.add(appointmentId);
        indexNew(c);
        // Walk-in present -> set appt to Checked In
        safeUpdateAppointmentStatus(appointmentId, "Checked In");
        // enqueue into walk-in queue (priority desc, then checkedInAt asc)
        orderedEnqueueWalkIn(c);
        save();
        return c;
    }

    // ---------------- Public API: Queue operations ----------------

    /**
     * Retrieves and processes the next walk-in patient from the queue.
     * The check-in is marked as completed and archived after being retrieved.
     *
     * @return The check-in that was processed, or null if the queue is empty
     */
    public CheckIn callNextWalkIn() {
        Deque<Long> q = state.walkInQueue;
        while (true) {
            Long id = q.pollFirst();
            if (id == null) return null; // empty
            CheckIn c = state.checkIns.get(id);
            if (c == null) continue; // stale id
            if (!c.isWalkIn() || !"CheckedIn".equals(c.getStatus())) {
                // skip non-waiting entries
                continue;
            }
            // complete immediately (patient heading to room)
            completeAndArchive(c);
            return c;
        }
    }

    /**
     * Retrieves and processes the next scheduled patient from the queue.
     * The check-in is marked as completed and archived after being retrieved.
     *
     * @return The check-in that was processed, or null if the queue is empty
     */
    public CheckIn callNextScheduled() {
        Deque<Long> q = state.scheduledQueue;
        while (true) {
            Long id = q.pollFirst();
            if (id == null) return null; // empty
            CheckIn c = state.checkIns.get(id);
            if (c == null) continue; // stale
            if (c.isWalkIn() || !"CheckedIn".equals(c.getStatus())) continue;
            // complete
            completeAndArchive(c);
            return c;
        }
    }

    // ---------------- Public API: Manual completion ----------------

    /**
     * Marks a check-in as completed manually.
     * Removes the check-in from any queue it might be in, updates its status,
     * records completion time, and archives it.
     *
     * @param checkInId ID of the check-in to mark as completed
     * @return The completed check-in
     * @throws IllegalArgumentException if the check-in doesn't exist
     */
    public CheckIn markCompleted(long checkInId) {
        CheckIn c = requireCheckIn(checkInId);
        if (!"Completed".equals(c.getStatus())) {
            // remove from any queue then complete
            removeFromQueuesIfPresent(checkInId);
            c.setStatusAndTouch("Completed");
            c.setCompletedAt(TimeUtil.nowStringSeconds());
            safeUpdateAppointmentStatus(c.getAppointmentId(), "Completed");
            save();
            archiveAndDelete(c);
        }
        return c;
    }

    /**
     * Marks a check-in as called (equivalent to marking it completed).
     * This indicates the patient has been called to see the doctor.
     *
     * @param checkInId ID of the check-in to mark as called
     * @return The completed check-in
     * @throws IllegalArgumentException if the check-in doesn't exist
     */
    public CheckIn markCalled(long checkInId) {
        CheckIn c = requireCheckIn(checkInId);
        if (!"Completed".equals(c.getStatus())) {
            // No separate calledAt field in simplified model; just complete
            completeAndArchive(c);
        }
        return c;
    }

    /**
     * Updates the priority level of a walk-in check-in.
     * This affects the position of the check-in in the walk-in queue.
     *
     * @param checkInId ID of the check-in to update
     * @param newPriority New priority level (higher values = higher priority)
     * @return The updated check-in
     * @throws IllegalArgumentException if the check-in doesn't exist or priority is negative
     * @throws IllegalStateException if the check-in is not a walk-in or is not in CheckedIn status
     */
    public CheckIn updateWalkInPriority(long checkInId, int newPriority) {
        CheckIn c = requireCheckIn(checkInId);
        if (!c.isWalkIn()) throw new IllegalStateException("Not a walk-in check-in");
        if (!"CheckedIn".equals(c.getStatus())) throw new IllegalStateException("Priority can only be changed while status is CheckedIn");
        if (newPriority < 0) throw new IllegalArgumentException("Priority must be >= 0");
        removeFromQueuesIfPresent(checkInId);
        c.setPriorityAndTouch(newPriority);
        orderedEnqueueWalkIn(c);
        save();
        return c;
    }

    // ---------------- Public API: Queries ----------------

    /**
     * Gets a check-in by its ID.
     *
     * @param id ID of the check-in to retrieve
     * @return The check-in
     * @throws IllegalArgumentException if the check-in doesn't exist
     */
    public CheckIn getById(long id) { return requireCheckIn(id); }

    /**
     * Finds a check-in by its associated appointment ID.
     *
     * @param appointmentId ID of the appointment
     * @return The check-in, or null if none found for the appointment
     * @throws IllegalArgumentException if appointmentId is invalid
     */
    public CheckIn findByAppointmentId(long appointmentId) {
        requirePositive(appointmentId, "appointmentId");
        Long cid = state.apptIndex.get(appointmentId);
        if (cid == null) return null;
        return state.checkIns.get(cid);
    }

    /**
     * Lists all check-ins for a specific patient.
     *
     * @param patientId ID of the patient
     * @return List of check-ins (may be empty but never null)
     * @throws IllegalArgumentException if patientId is invalid
     */
    public List<CheckIn> listByPatient(long patientId) {
        requirePositive(patientId, "patientId");
        List<CheckIn> list = new ArrayList<>();
        List<Long> ids = state.patientIndex.get(patientId);
        if (ids == null) return list;
        for (Long id : ids) {
            CheckIn c = state.checkIns.get(id);
            if (c != null) list.add(c);
        }
        return list;
    }

    /**
     * Lists only walk-in check-ins for a specific patient.
     *
     * @param patientId ID of the patient
     * @return List of walk-in check-ins (may be empty but never null)
     */
    public List<CheckIn> listWalkInsByPatient(long patientId) {
        List<CheckIn> base = listByPatient(patientId);
        List<CheckIn> out = new ArrayList<>();
        for (CheckIn c : base) if (c.isWalkIn()) out.add(c);
        return out;
    }

    /**
     * Lists all check-ins currently in the walk-in queue.
     *
     * @return List of check-ins in the walk-in queue (may be empty but never null)
     */
    public List<CheckIn> listWalkInQueue() {
        List<CheckIn> list = new ArrayList<>();
        for (Long id : state.walkInQueue) {
            CheckIn c = state.checkIns.get(id);
            if (c != null) list.add(c);
        }
        return list;
    }

    /**
     * Lists all check-ins currently in the scheduled queue.
     *
     * @return List of check-ins in the scheduled queue (may be empty but never null)
     */
    public List<CheckIn> listScheduledQueue() {
        List<CheckIn> list = new ArrayList<>();
        for (Long id : state.scheduledQueue) {
            CheckIn c = state.checkIns.get(id);
            if (c != null) list.add(c);
        }
        return list;
    }

    /**
     * Lists all active check-ins in the system.
     *
     * @return List of all check-ins (may be empty but never null)
     */
    public List<CheckIn> listAll() { return new ArrayList<>(state.checkIns.values()); }

    /**
     * Checks if an appointment was created as part of a walk-in check-in.
     *
     * @param appointmentId ID of the appointment to check
     * @return true if the appointment was created for a walk-in, false otherwise
     * @throws IllegalArgumentException if appointmentId is invalid
     */
    public boolean isWalkInAppointment(long appointmentId) {
        requirePositive(appointmentId, "appointmentId");
        return state.walkInAppointmentIds.contains(appointmentId);
    }

    // ---------------- Public API: Persistence hooks ----------------

    /**
     * Saves the current state to persistent storage.
     */
    public void saveSnapshot() { save(); }

    /**
     * Reloads state from persistent storage.
     * This discards any unsaved changes to the current state.
     */
    public void reload() {
        CheckInRepository.State fresh = repo.load();
        state.checkIns.clear();
        state.checkIns.putAll(fresh.checkIns);
        state.walkInQueue.clear();
        state.walkInQueue.addAll(fresh.walkInQueue);
        state.scheduledQueue.clear();
        state.scheduledQueue.addAll(fresh.scheduledQueue);
        state.walkInAppointmentIds.clear();
        state.walkInAppointmentIds.addAll(fresh.walkInAppointmentIds);
        state.nextCheckInId = fresh.nextCheckInId;
        rebuildIndexesIfNeeded();
        sanitizeQueues();
    }

    // ---------------- Additional Helper APIs ----------------

    /**
     * Lists all active walk-in check-ins.
     *
     * @return List of all walk-in check-ins (may be empty but never null)
     */
    public List<CheckIn> listAllWalkIns() {
        List<CheckIn> list = new ArrayList<>();
        for (CheckIn c : state.checkIns.values()) if (c.isWalkIn()) list.add(c);
        return list;
    }

    /**
     * Lists all active scheduled (non-walk-in) check-ins.
     *
     * @return List of all scheduled check-ins (may be empty but never null)
     */
    public List<CheckIn> listAllScheduled() {
        List<CheckIn> list = new ArrayList<>();
        for (CheckIn c : state.checkIns.values()) if (!c.isWalkIn()) list.add(c);
        return list;
    }

    /**
     * Updates the desk/location field for a check-in.
     *
     * @param checkInId ID of the check-in to update
     * @param desk New desk/location value
     * @return The updated check-in
     * @throws IllegalArgumentException if the check-in doesn't exist
     */
    public CheckIn updateDesk(long checkInId, String desk) {
        CheckIn c = requireCheckIn(checkInId);
        c.setDesk(desk == null || desk.isBlank()? null : desk.trim());
        c.touch();
        save();
        return c;
    }

    /**
     * Updates the notes field for a check-in.
     *
     * @param checkInId ID of the check-in to update
     * @param notes New notes value
     * @return The updated check-in
     * @throws IllegalArgumentException if the check-in doesn't exist
     */
    public CheckIn updateNotes(long checkInId, String notes) {
        CheckIn c = requireCheckIn(checkInId);
        c.setNotes(notes == null || notes.isBlank()? null : notes.trim());
        c.touch();
        save();
        return c;
    }

    /**
     * Deletes a check-in from the system.
     * This removes all references to the check-in from indexes and queues.
     *
     * @param id ID of the check-in to delete
     * @return true if the check-in was deleted, false if it didn't exist
     */
    public boolean deleteCheckIn(long id) {
        CheckIn c = state.checkIns.remove(id);
        if (c == null) return false;
        // remove from queues
        removeFromQueuesIfPresent(id);
        // appt index
        Long mapped = state.apptIndex.get(c.getAppointmentId());
        if (mapped != null && mapped == id) state.apptIndex.remove(c.getAppointmentId());
        // patient index
        List<Long> plist = state.patientIndex.get(c.getPatientId());
        if (plist != null) {
            plist.remove(Long.valueOf(id));
            if (plist.isEmpty()) state.patientIndex.remove(c.getPatientId());
        }
        // walk-in set
        if (c.isWalkIn()) state.walkInAppointmentIds.remove(c.getAppointmentId());
        save();
        return true;
    }

    // Append to history and remove from active state
    private void archiveAndDelete(CheckIn c) {
        try {
            Appointment a = storage.getAppointment(c.getAppointmentId());
            long doctorId = (a != null ? a.getDoctorId() : 0L);
            var rec = CheckInHistoryRepository.Record.from(c, doctorId);
            historyRepo.append(rec);
        } catch (Exception e) {
            System.err.println("[History] Failed to append: " + e.getMessage());
        }
        deleteCheckIn(c.getId());
    }

    /**
     * Lists all completed check-ins from the history repository.
     *
     * @return List of historical check-in records
     */
    public List<CheckInHistoryRepository.Record> listHistory() { return historyRepo.loadAll(); }

    // ---------------- Helpers ----------------

    private void completeAndArchive(CheckIn c) {
        removeFromQueuesIfPresent(c.getId());
        if (!"Completed".equals(c.getStatus())) {
            c.setStatusAndTouch("Completed");
            c.setCompletedAt(TimeUtil.nowStringSeconds());
            safeUpdateAppointmentStatus(c.getAppointmentId(), "Completed");
            save();
        }
        archiveAndDelete(c);
    }

    private long nextId() { return state.nextCheckInId++; }

    private CheckIn requireCheckIn(long id) {
        requirePositive(id, "checkInId");
        CheckIn c = state.checkIns.get(id);
        if (c == null) throw new IllegalArgumentException("CheckIn " + id + " not found");
        return c;
    }

    private void ensureSingleActiveCheckInForAppointment(long appointmentId) {
        Long cid = state.apptIndex.get(appointmentId);
        if (cid != null) {
            CheckIn existing = state.checkIns.get(cid);
            if (existing != null && !isTerminal(existing.getStatus())) {
                throw new IllegalStateException("Active check-in already exists for appointment " + appointmentId);
            }
        }
        if (cid == null) {
            for (CheckIn c : state.checkIns.values()) {
                if (c.getAppointmentId() == appointmentId && !isTerminal(c.getStatus())) {
                    throw new IllegalStateException("Active check-in already exists for appointment " + appointmentId);
                }
            }
        }
    }

    private boolean isTerminal(String status) { return "Completed".equals(status); }

    private void removeFromQueuesIfPresent(long checkInId) {
        // walk-in queue
        Iterator<Long> it = state.walkInQueue.iterator();
        while (it.hasNext()) { if (Long.valueOf(checkInId).equals(it.next())) { it.remove(); break; } }
        // scheduled queue
        it = state.scheduledQueue.iterator();
        while (it.hasNext()) { if (Long.valueOf(checkInId).equals(it.next())) { it.remove(); break; } }
    }

    private void orderedEnqueueWalkIn(CheckIn c) {
        if (!c.isWalkIn()) return;
        if (!"CheckedIn".equals(c.getStatus())) return; // only waiting state
        String cTime = c.getCheckedInAt();
        int cPriority = c.getPriority();
        var it = state.walkInQueue.iterator();
        int index = 0;
        while (it.hasNext()) {
            Long existingId = it.next();
            CheckIn ex = state.checkIns.get(existingId);
            if (ex == null) { it.remove(); continue; }
            int cmpP = Integer.compare(cPriority, ex.getPriority());
            if (cmpP > 0) break; // higher priority first
            if (cmpP == 0) {
                try {
                    var ct = TimeUtil.parseDateTime(cTime);
                    var et = TimeUtil.parseDateTime(ex.getCheckedInAt());
                    if (ct.isBefore(et)) break; // earlier check-in first
                } catch (Exception ignored) {}
            }
            index++;
        }
        insertIntoDequeAt(state.walkInQueue, c.getId(), index);
    }

    private void orderedEnqueueScheduled(CheckIn c) {
        if (c.isWalkIn()) return;
        if (!"CheckedIn".equals(c.getStatus())) return;
        // order by appointment start asc, then checkedInAt asc
        String cCheckInAt = c.getCheckedInAt();
        var it = state.scheduledQueue.iterator();
        int index = 0;
        while (it.hasNext()) {
            Long existingId = it.next();
            CheckIn ex = state.checkIns.get(existingId);
            if (ex == null) { it.remove(); continue; }
            Appointment aC = storage.getAppointment(c.getAppointmentId());
            Appointment aE = storage.getAppointment(ex.getAppointmentId());
            if (aE == null || aC == null) { index++; continue; }
            try {
                var cStart = TimeUtil.combine(aC.getAppointmentDate(), aC.getAppointmentTime());
                var eStart = TimeUtil.combine(aE.getAppointmentDate(), aE.getAppointmentTime());
                int cmp = cStart.compareTo(eStart); // asc
                if (cmp < 0) break; // earlier appt first
                if (cmp == 0) {
                    var ct = TimeUtil.parseDateTime(cCheckInAt);
                    var et = TimeUtil.parseDateTime(ex.getCheckedInAt());
                    if (ct.isBefore(et)) break; // earlier check-in first
                }
            } catch (Exception ignored) { }
            index++;
        }
        insertIntoDequeAt(state.scheduledQueue, c.getId(), index);
    }

    private void insertIntoDequeAt(Deque<Long> deque, long id, int index) {
        if (index < 0) index = 0;
        if (index >= deque.size()) {
            deque.addLast(id);
        } else {
            List<Long> temp = new ArrayList<>(deque);
            temp.add(index, id);
            deque.clear();
            deque.addAll(temp);
        }
    }

    private void save() { repo.save(state); }

    private void requirePositive(long v, String field) { if (v <= 0) throw new IllegalArgumentException(field + " must be positive"); }

    private void sanitizeQueues() {
        // Walk-in queue: keep only walk-ins in CheckedIn
        Iterator<Long> it = state.walkInQueue.iterator();
        while (it.hasNext()) {
            Long id = it.next();
            CheckIn c = state.checkIns.get(id);
            if (c == null || !c.isWalkIn() || !"CheckedIn".equals(c.getStatus())) it.remove();
        }
        dedupeDeque(state.walkInQueue);
        // reorder walk-in
        List<CheckIn> waiting = new ArrayList<>();
        for (Long id : state.walkInQueue) { CheckIn c = state.checkIns.get(id); if (c != null) waiting.add(c); }
        waiting.sort((a,b) -> {
            int p = Integer.compare(b.getPriority(), a.getPriority()); // desc
            if (p != 0) return p;
            try {
                var at = TimeUtil.parseDateTime(a.getCheckedInAt());
                var bt = TimeUtil.parseDateTime(b.getCheckedInAt());
                return at.compareTo(bt); // asc
            } catch (Exception e) { return 0; }
        });
        state.walkInQueue.clear();
        for (CheckIn c : waiting) state.walkInQueue.addLast(c.getId());

        // Scheduled queue: keep only scheduled (non-walk-in) in CheckedIn
        it = state.scheduledQueue.iterator();
        while (it.hasNext()) {
            Long id = it.next();
            CheckIn c = state.checkIns.get(id);
            if (c == null || c.isWalkIn() || !"CheckedIn".equals(c.getStatus())) it.remove();
        }
        dedupeDeque(state.scheduledQueue);
        // reorder scheduled by appt start asc, then checkedInAt asc
        List<CheckIn> sched = new ArrayList<>();
        for (Long id : state.scheduledQueue) { CheckIn c = state.checkIns.get(id); if (c != null) sched.add(c); }
        sched.sort((a,b) -> {
            try {
                var as = TimeUtil.combine(storage.getAppointment(a.getAppointmentId()).getAppointmentDate(), storage.getAppointment(a.getAppointmentId()).getAppointmentTime());
                var bs = TimeUtil.combine(storage.getAppointment(b.getAppointmentId()).getAppointmentDate(), storage.getAppointment(b.getAppointmentId()).getAppointmentTime());
                int cmp = as.compareTo(bs);
                if (cmp != 0) return cmp;
                var at = TimeUtil.parseDateTime(a.getCheckedInAt());
                var bt = TimeUtil.parseDateTime(b.getCheckedInAt());
                return at.compareTo(bt);
            } catch (Exception e) { return 0; }
        });
        state.scheduledQueue.clear();
        for (CheckIn c : sched) state.scheduledQueue.addLast(c.getId());
    }

    private void dedupeDeque(Deque<Long> dq) {
        List<Long> ordered = new ArrayList<>();
        for (Long id : dq) if (!ordered.contains(id)) ordered.add(id);
        dq.clear(); dq.addAll(ordered);
    }

    // ---- New helpers: appointment coupling ----
    private void ensureApptEligibleForCheckIn(Appointment appt) {
        String st = appt.getStatus() == null ? "" : appt.getStatus().trim();
        if (!"Scheduled".equalsIgnoreCase(st)) {
            throw new IllegalStateException("Cannot check in. Appointment status must be 'Scheduled', found: '" + appt.getStatus() + "'");
        }
    }

    private void safeUpdateAppointmentStatus(long appointmentId, String status) {
        try {
            apptSvc.updateStatus(appointmentId, status);
        } catch (Exception e) {
            System.err.println("[CheckIn] Failed to update appointment " + appointmentId + " status -> '" + status + "': " + e.getMessage());
        }
    }

    private void rebuildIndexesIfNeeded() {
        if (state.apptIndex == null) state.apptIndex = new HashMap<>();
        if (state.patientIndex == null) state.patientIndex = new HashMap<>();
        state.apptIndex.clear();
        state.patientIndex.clear();
        for (CheckIn c : state.checkIns.values()) {
            state.apptIndex.put(c.getAppointmentId(), c.getId());
            state.patientIndex.computeIfAbsent(c.getPatientId(), k -> new ArrayList<>()).add(c.getId());
        }
    }

    private void indexNew(CheckIn c) {
        state.apptIndex.put(c.getAppointmentId(), c.getId());
        state.patientIndex.computeIfAbsent(c.getPatientId(), k -> new ArrayList<>()).add(c.getId());
    }
}
