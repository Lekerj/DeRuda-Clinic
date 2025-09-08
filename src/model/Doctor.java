package model;

/**
 * Represents a doctor in the clinic system.
 * Extends the Person class with doctor-specific attributes like specialty.
 */
public class Doctor extends Person {
    private static final long serialVersionUID = 1L;

    private String specialty;

    /**
     * Creates a new doctor with the specified details and current timestamp.
     *
     * @param firstName The doctor's first name
     * @param lastName The doctor's last name
     * @param id The doctor's ID
     * @param phoneNumber The doctor's phone number
     * @param specialty The doctor's specialty
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Doctor(String firstName, String lastName, long id, String phoneNumber, String specialty) {
        super(id);
        super.setFirstName(firstName);
        super.setLastName(lastName);
        super.setPhoneNumber(phoneNumber);
        setSpecialty(specialty);
        touch(); // Single touch after all fields are set
    }

    /**
     * Creates a doctor with the specified details and timestamps.
     * Used primarily for importing data.
     *
     * @param firstName The doctor's first name
     * @param lastName The doctor's last name
     * @param id The doctor's ID
     * @param phoneNumber The doctor's phone number
     * @param specialty The doctor's specialty
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Doctor(String firstName, String lastName, long id, String phoneNumber, String specialty,
                  String createdAt, String updatedAt) {
        super(id, createdAt, updatedAt); // preserve audit stamps
        // Use setters for validation without touching
        super.setFirstName(firstName);
        super.setLastName(lastName);
        super.setPhoneNumber(phoneNumber);
        setSpecialty(specialty);
    }

    /**
     * Gets the doctor's specialty.
     *
     * @return The specialty
     */
    public String getSpecialty() {
        return specialty;
    }

    /**
     * Sets the doctor's specialty without updating the timestamp.
     *
     * @param specialty The specialty to set
     * @throws IllegalArgumentException if the specialty is invalid
     */
    public void setSpecialty(String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
            throw new IllegalArgumentException("Specialty cannot be null or blank");
        }
        this.specialty = specialty.trim();
    }

    /**
     * Sets the doctor's specialty and updates the timestamp.
     *
     * @param specialty The specialty to set
     * @throws IllegalArgumentException if the specialty is invalid
     */
    public void setSpecialtyAndTouch(String specialty) {
        setSpecialty(specialty);
        touch();
    }

    @Override
    public String toString() {
        // Matches FileIO.DOCTOR_FORMAT (serializeDoctor)
        return getId() + "|" +
                getFirstName() + "|" +
                getLastName() + "|" +
                getSpecialty() + "|" +
                getPhoneNumber() + "|" +
                getUpdatedAt() + "|" +
                getCreatedAt();
    }
}