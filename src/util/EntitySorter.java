package util;

import model.BaseEntity;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class providing sorting methods for BaseEntity objects.
 * Contains methods to sort lists of entities by common fields like ID and timestamps.
 * All sort operations are performed in-place and also return the original list for chaining.
 */
public final class EntitySorter {

    private EntitySorter() {}

    /**
     * Defines sort directions for entity collections.
     * ASC for ascending (smallest to largest) order.
     * DESC for descending (largest to smallest) order.
     */
    public enum Direction { ASC, DESC }

    // -------- BaseEntity common only --------

    /**
     * Sorts a list of entities by their ID.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending BaseEntity
     * @return The sorted list (same instance as input)
     */
    public static <T extends BaseEntity> List<T> byId(List<T> list, Direction dir) {
        list.sort(dir == Direction.ASC
                ? Comparator.comparingLong(BaseEntity::getId)
                : Comparator.comparingLong(BaseEntity::getId).reversed());
        return list;
    }

    /**
     * Sorts a list of entities by their creation timestamp.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending BaseEntity
     * @return The sorted list (same instance as input)
     */
    public static <T extends BaseEntity> List<T> byCreatedAt(List<T> list, Direction dir) {
        Comparator<T> cmp = Comparator.comparing(e -> safeParse(e.getCreatedAt(), dir == Direction.ASC));
        if (dir == Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    /**
     * Sorts a list of entities by their last update timestamp.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending BaseEntity
     * @return The sorted list (same instance as input)
     */
    public static <T extends BaseEntity> List<T> byUpdatedAt(List<T> list, Direction dir) {
        Comparator<T> cmp = Comparator.comparing(e -> safeParse(e.getUpdatedAt(), dir == Direction.ASC));
        if (dir == Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    // -------- Helpers --------

    /**
     * Safely parses a datetime string, handling potential format errors.
     * If parsing fails, returns a sentinel value based on the sort direction.
     *
     * @param s String to parse
     * @param asc Whether sorting is ascending
     * @return Parsed LocalDateTime or a sentinel value for sorting
     */
    private static LocalDateTime safeParse(String s, boolean asc) {
        try {
            return TimeUtil.parseDateTime(s);
        } catch (Exception e) {
            return asc ? LocalDateTime.MAX : LocalDateTime.MIN;
        }
    }
}
