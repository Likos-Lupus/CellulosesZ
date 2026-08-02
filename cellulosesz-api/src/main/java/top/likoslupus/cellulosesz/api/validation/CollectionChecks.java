package top.likoslupus.cellulosesz.api.validation;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/** Focused validation checks. */
@SuppressWarnings("UnusedReturnValue")
public final class CollectionChecks {

    private CollectionChecks() {
        throw new AssertionError("No instances");
    }

    public static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
        collection = requireArgumentNonNull(collection, name);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return collection;
    }

    private static <T> T requireArgumentNonNull(T value, String name) {
        name = requireName(name);
        return requireNonNull(value, name + " must not be null");
    }

    private static String requireName(String name) {
        requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    public static <M extends Map<?, ?>> M requireNonEmpty(M map, String name) {
        map = requireArgumentNonNull(map, name);
        if (map.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return map;
    }

}
