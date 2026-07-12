package top.likoslupus.cellulosesz.api.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Platform-neutral reference to a localized message template and its placeholders.
 */
public record LocalizedMessage(
        String key,
        Map<String, Object> placeholders
) {

    public LocalizedMessage {
        key = Objects.requireNonNull(key, "key").trim();
        if (key.isEmpty()) throw new IllegalArgumentException("Message key must not be blank");
        placeholders = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(placeholders, "placeholders")));
    }

    public static LocalizedMessage of(String key) {
        return new LocalizedMessage(key, Map.of());
    }

    public static LocalizedMessage of(String key, Map<String, ?> placeholders) {
        var copied = new LinkedHashMap<String, Object>(placeholders);
        return new LocalizedMessage(key, copied);
    }

}
