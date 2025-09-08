# DeRuda Clinic Management System

A Java-based clinic management application that handles patient records, doctor information, appointments, and check-ins. Built as a practical exercise to apply Java programming concepts in a real-world scenario.

## Project Overview

DeRuda Clinic is a console-based application that helps medical clinics manage their day-to-day operations. The system keeps track of patients and doctors, schedules appointments, processes patient check-ins (both scheduled and walk-ins), and maintains historical records of visits.

## Features

### Patient Management
- Register new patients with personal information
- Update existing patient details
- Search patients by name, phone number, or age range
- View patient appointment history
- Delete patients (if they have no appointments)

### Doctor Management
- Register doctors with their specialties
- Update doctor information
- Search by name or specialty
- View doctor schedules
- Delete doctors (if they have no appointments)

### Appointment Scheduling
- Create appointments linking patients with doctors
- Set appointment dates, times, and durations
- Track appointment status (Scheduled, Checked In, In Progress, Completed, Cancelled, No Show)
- Update appointment details
- Schedule follow-up appointments
- Cancel appointments

### Check-in System
- Process arrivals for scheduled appointments
- Handle walk-in patients with priority levels
- Maintain queues for both scheduled and walk-in patients
- Mark check-ins as completed when patients are seen
- Archive completed check-ins for historical record-keeping

### Data Persistence
- All data is saved to text files (patients, doctors, appointments)
- Check-in queue state is serialized for system restarts
- Completed check-ins are stored in a history file

### Search and Filtering
- Filter patients/doctors by various criteria
- Search for appointments by patient, doctor, date range, or status
- Sort results by different fields (name, date, etc.)
- Reverse sort order to customize views

## How the System Works

### Main Menu Navigation
The program runs through a menu-driven interface with these main sections:
1. Patients - All patient-related operations
2. Doctors - All doctor-related operations
3. Appointments - Creating and managing appointments
4. Check-Ins - Processing patient arrivals and queues
5. Save All - Persist all data to files
0. Exit - Close the application

### Patient and Doctor Registration
When registering patients or doctors, you enter basic information (name, contact details). For patients, you also enter age; for doctors, you specify their specialty. Each entity receives a unique ID automatically.

### Appointment Workflow
1. Select a patient (existing or create new)
2. Choose a doctor
3. Set appointment details (date, time, duration)
4. Add optional information (reason, notes, location)
5. Appointment is created and can be viewed in both patient and doctor records

### Check-in Process
- For scheduled appointments: Find the patient's appointment and mark them as checked in
- For walk-ins: Enter patient and doctor info, set priority level, and add to the walk-in queue
- Staff can call the next patient from either queue based on priority or appointment time
- After the visit, check-ins are completed and archived to history

## Technical Implementation

### Class Structure
- **Model Layer**: BaseEntity, Person, Patient, Doctor, Appointment, CheckIn
- **Repository Layer**: InMemoryStore, CheckInRepository, CheckInHistoryRepository
- **Service Layer**: PatientService, DoctorService, AppointmentService, CheckInService
- **Application Layer**: Main, Menu
- **Utility Layer**: TimeUtil, EntitySorter, PersonSorter, FileIO

### Key Programming Concepts Applied

- **Object-Oriented Design**
  - Inheritance: Person → Patient/Doctor, BaseEntity as the foundation class
  - Encapsulation: Private fields with getter/setter methods
  - Polymorphism: Shared behavior across entity types

- **Data Structures**
  - HashMaps for ID-based entity storage and fast lookup
  - ArrayLists for search results and filtered collections
  - Deques for priority-based patient queues

- **File I/O and Persistence**
  - Text file storage using pipe-delimited format
  - Binary serialization for complex queue state
  - Append-only history for audit records

- **CRUD Operations**
  - Create: New patients, doctors, appointments, check-ins
  - Read: Search and retrieve entities by various criteria
  - Update: Modify entity attributes with validation
  - Delete: Remove entities with referential integrity checks

- **Sorting and Filtering**
  - Custom comparators for different entity attributes
  - Filters by date ranges, text patterns, and numeric ranges
  - Customizable sort direction (ascending/descending)

- **Data Validation**
  - Input validation for all fields
  - Format checking for dates, times, phone numbers
  - Referential integrity enforcement

## Limitations and Areas for Improvement

- Console-based UI limits user experience compared to a GUI
- No authentication or user permission system
- Limited reporting capabilities
- No backup mechanism for data files
- Limited concurrency support (single-user system)
- No external database integration
- Limited error recovery for file corruption
- No network/multi-device capabilities

## Learning Goals Achieved

This project was developed to practice applying Java programming concepts in a realistic application:

- Designing a layered architecture with clear separation of concerns
- Implementing complex business logic with proper validation
- Managing entity relationships and referential integrity
- Handling data persistence without relying on databases
- Creating flexible search and sort mechanisms
- Building a usable command-line interface
- Applying proper error handling and validation

The focus was on writing clean, well-organized code that demonstrates good software design principles while solving practical problems in a healthcare setting.
Created to demonstrate software design principles and Java programming techniques in a medical clinic domain.

---
Developed by Ahmed Gara Ali
