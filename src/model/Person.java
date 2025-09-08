package model;

/**
 * Base class for all person entities in the system.
 * Contains common attributes and behavior for Doctor and Patient classes.
 */
public abstract class Person extends BaseEntity {
    private static final long serialVersionUID = 1L;

    protected String firstName;
    protected String lastName;
    protected String phoneNumber;

    /**
     * Creates a new person with the specified ID and current timestamp.
     *
     * @param id The entity ID
     * @throws IllegalArgumentException if ID is not positive
     */
    public Person(long id) {
        super(id);
    }

    /**
     * Creates a person with the specified ID and timestamps.
     * Used primarily for importing data.
     *
     * @param id The entity ID
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @throws IllegalArgumentException if ID is not positive or timestamps are invalid
     */
    public Person(long id, String createdAt, String updatedAt) {
        super(id, createdAt, updatedAt);
    }

    /**
     * Gets the person's first name.
     *
     * @return The first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the person's first name without updating the timestamp.
     *
     * @param firstName The first name to set
     * @throws IllegalArgumentException if the name is invalid
     */
    public void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or blank");
        }
        this.firstName = firstName.trim();
    }

    /**
     * Sets the person's first name and updates the timestamp.
     *
     * @param firstName The first name to set
     * @throws IllegalArgumentException if the name is invalid
     */
    public void setFirstNameAndTouch(String firstName) {
        setFirstName(firstName);
        touch();
    }

    /**
     * Gets the person's last name.
     *
     * @return The last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the person's last name without updating the timestamp.
     *
     * @param lastName The last name to set
     * @throws IllegalArgumentException if the name is invalid
     */
    public void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or blank");
        }
        this.lastName = lastName.trim();
    }

    /**
     * Sets the person's last name and updates the timestamp.
     *
     * @param lastName The last name to set
     * @throws IllegalArgumentException if the name is invalid
     */
    public void setLastNameAndTouch(String lastName) {
        setLastName(lastName);
        touch();
    }

    /**
     * Gets the person's phone number.
     *
     * @return The phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the person's phone number without updating the timestamp.
     *
     * @param phoneNumber The phone number to set
     * @throws IllegalArgumentException if the phone number is invalid
     */
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }
        String trimmed = phoneNumber.trim();
        if (!trimmed.matches("\\d{7,15}")) {
            throw new IllegalArgumentException("Phone number must contain only digits and be 7-15 characters long");
        }
        this.phoneNumber = trimmed;
    }

    /**
     * Sets the person's phone number and updates the timestamp.
     *
     * @param phoneNumber The phone number to set
     * @throws IllegalArgumentException if the phone number is invalid
     */
    public void setPhoneNumberAndTouch(String phoneNumber) {
        setPhoneNumber(phoneNumber);
        touch();
    }
}
