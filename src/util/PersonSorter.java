package util;

import model.Patient;
import model.Doctor;
import model.Person;

import java.util.Comparator;
import java.util.List;

/**
 * Utility class providing sorting methods for Person objects and their subclasses.
 * Contains methods to sort lists of Person, Doctor, and Patient objects by various fields.
 * All sort operations are performed in-place and also return the original list for chaining.
 */
public final class PersonSorter {
    private PersonSorter() {}

    // ---- Generic Person field sorters ----
    /**
     * Sorts a list of Person objects by first name.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending Person
     * @return The sorted list (same instance as input)
     */
    public static <T extends Person> List<T> byFirstName(List<T> list, EntitySorter.Direction dir) {
        sortString(list, Person::getFirstName, dir);
        return list;
    }

    /**
     * Sorts a list of Person objects by last name.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending Person
     * @return The sorted list (same instance as input)
     */
    public static <T extends Person> List<T> byLastName(List<T> list, EntitySorter.Direction dir) {
        sortString(list, Person::getLastName, dir);
        return list;
    }

    /**
     * Sorts a list of Person objects by phone number.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending Person
     * @return The sorted list (same instance as input)
     */
    public static <T extends Person> List<T> byPhone(List<T> list, EntitySorter.Direction dir) {
        sortString(list, Person::getPhoneNumber, dir);
        return list;
    }

    /**
     * Sorts a list of Person objects by full name (last name, then first name).
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @param <T> Type extending Person
     * @return The sorted list (same instance as input)
     */
    public static <T extends Person> List<T> byFullName(List<T> list, EntitySorter.Direction dir) {
        Comparator<T> cmp = Comparator
                .comparing((T p) -> safeLower(p.getLastName()))
                .thenComparing(p -> safeLower(p.getFirstName()));
        if (dir == EntitySorter.Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    // ---- Patient-specific ----
    /**
     * Sorts a list of Patient objects by age.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @return The sorted list (same instance as input)
     */
    public static List<Patient> byAge(List<Patient> list, EntitySorter.Direction dir) {
        Comparator<Patient> cmp = Comparator.comparingInt(Patient::getAge);
        if (dir == EntitySorter.Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    // ---- Doctor-specific ----
    /**
     * Sorts a list of Doctor objects by specialty.
     *
     * @param list The list to sort (modified in-place)
     * @param dir Sort direction (ascending or descending)
     * @return The sorted list (same instance as input)
     */
    public static List<Doctor> bySpecialty(List<Doctor> list, EntitySorter.Direction dir) {
        Comparator<Doctor> cmp = Comparator.comparing(
                Doctor::getSpecialty,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        if (dir == EntitySorter.Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
        return list;
    }

    // ---- Helpers ----
    /**
     * Sorts a list using a string property extracted by the provided function.
     *
     * @param list The list to sort
     * @param f Function to extract the string property
     * @param dir Sort direction
     * @param <T> Type of objects in the list
     */
    private static <T> void sortString(List<T> list, java.util.function.Function<T,String> f, EntitySorter.Direction dir) {
        Comparator<T> cmp = Comparator.comparing(
                t -> {
                    String s = f.apply(t);
                    return s == null ? "" : s.trim().toLowerCase();
                }
        );
        if (dir == EntitySorter.Direction.DESC) cmp = cmp.reversed();
        list.sort(cmp);
    }

    /**
     * Returns lowercase version of string, handling null safely.
     *
     * @param s String to convert to lowercase
     * @return Lowercase string or empty string if input is null
     */
    private static String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
