package model;

import util.TimeUtil;

/**
 * Base class for all entities in the system.
 * Provides common functionality for ID management and timestamp tracking.
 * All domain objects extend this class to inherit basic properties and behaviors.
 */
public abstract class BaseEntity implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected final long id;
    protected String createdAt; // dd-MM-yyyy HH:mm:ss
    protected String updatedAt; // dd-MM-yyyy HH:mm:ss

    /**
     * Creates a new entity with the specified ID and current timestamp.
     * Sets both creation and update timestamps to the current time.
     *
     * @param id The entity ID
     * @throws IllegalArgumentException if ID is not positive
     */
    public BaseEntity(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Entity ID must be positive, got: " + id);
        }
        this.id = id;
        this.createdAt = TimeUtil.nowStringSeconds();
        this.updatedAt = this.createdAt;
    }

    /**
     * Creates an entity with the specified ID and timestamps.
     * Used primarily for importing data from external sources or reconstructing
     * persisted entities while preserving their original timestamps.
     *
     * @param id The entity ID
     * @param createdAt The creation timestamp
     * @param updatedAt The last update timestamp
     * @throws IllegalArgumentException if ID is not positive or timestamps are invalid
     */
    public BaseEntity(long id, String createdAt, String updatedAt) {
        if (id <= 0) {
            throw new IllegalArgumentException("Entity ID must be positive, got: " + id);
        }
        this.id = id;
        this.createdAt = TimeUtil.normalizeDateTime(createdAt);

        if (updatedAt == null || updatedAt.trim().isEmpty()) {
            this.updatedAt = this.createdAt;
        } else {
            this.updatedAt = TimeUtil.normalizeDateTime(updatedAt);
        }
    }

    /**
     * Gets the entity ID.
     * IDs are immutable and unique within each entity type.
     *
     * @return The ID
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the entity creation timestamp.
     * Format is dd-MM-yyyy HH:mm:ss.
     *
     * @return The creation timestamp as string
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the entity last update timestamp.
     * Format is dd-MM-yyyy HH:mm:ss.
     *
     * @return The last update timestamp as string
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Updates the updatedAt timestamp to the current time.
     * Should be called by every setter that modifies entity state.
     */
    public final void touch() {

        this.updatedAt = TimeUtil.nowStringSeconds();
    }

    /**
     * Sets the createdAt timestamp from an import source.
     * Should only be used during data import operations.
     *
     * @param dateTime The datetime string to set
     */
    protected final void setCreatedAtFromImport(String dateTime) {
        this.createdAt = TimeUtil.normalizeDateTime(dateTime);
    }

    /**
     * Sets the updatedAt timestamp from an import source.
     * Should only be used during data import operations.
     *
     * @param dateTime The datetime string to set
     */
    protected final void setUpdatedAtFromImport(String dateTime) {
        this.updatedAt = TimeUtil.normalizeDateTime(dateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Intentional use of getClass() (not instanceof) to prevent cross-type equality
        // (e.g., Patient id=1 must not equal Doctor id=1) to avoid Set/Map key collisions.
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
