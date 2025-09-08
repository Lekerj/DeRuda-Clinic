package model;

/**
 * Represents a patient in the clinic system.
 * Extends the Person class with patient-specific attributes like age.
 */
public class Patient extends Person {
    private static final long serialVersionUID = 1L;

    private int age;

    /**
     * Creates a new patient with the specified details and current timestamp.
     *
     * @param firstName The patient's first name
     * @param lastName The patient's last name
     * @param age The patient's age
     * @param id The patient's ID
     * @param phoneNumber The patient's phone number
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Patient(String firstName, String lastName, int age, long id, String phoneNumber) {
        super(id);
        super.setFirstName(firstName);
        super.setLastName(lastName);
        setAge(age);
        super.setPhoneNumber(phoneNumber);
        touch(); // Single touch after all fields are set
    }

    /**
     * Creates a patient with the specified details and timestamps.
     * Used primarily for importing data.
     *
     * @param firstName The patient's first name
     * @param lastName The patient's last name
     * @param age The patient's age
     * @param id The patient's ID
     * @param phoneNumber The patient's phone number
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Patient(String firstName, String lastName, int age, long id, String phoneNumber,
                  String createdAt, String updatedAt) {
        super(id, createdAt, updatedAt); // preserve audit stamps (normalized in BaseEntity)
        // Use setters for validation without touching; DO NOT call touch() here to preserve imported timestamps
        super.setFirstName(firstName);
        super.setLastName(lastName);
        setAge(age);
        super.setPhoneNumber(phoneNumber);
        // Removed redundant setCreatedAtFromImport / setUpdatedAtFromImport calls
    }

    /**
     * Gets the patient's age.
     *
     * @return The age
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets the patient's age without updating the timestamp.
     *
     * @param age The age to set
     * @throws IllegalArgumentException if the age is invalid
     */
    public void setAge(int age) {
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120, got: " + age);
        }
        this.age = age;
    }

    /**
     * Sets the patient's age and updates the timestamp.
     *
     * @param age The age to set
     * @throws IllegalArgumentException if the age is invalid
     */
    public void setAgeAndTouch(int age) {
        setAge(age);
        touch();
    }

    @Override
    public String toString() {
        // Matches FileIO.PATIENT_FORMAT (serializePatient)
        return getId() + "|" +
               getFirstName() + "|" +
               getLastName() + "|" +
               getAge() + "|" +
               getPhoneNumber() + "|" +
               getUpdatedAt() + "|" +
               getCreatedAt();
    }
}
