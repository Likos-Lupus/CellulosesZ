package top.likoslupus.cellulosesz.api.text;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Immutable typed placeholder collection. */
public final class MessageArguments {

    private static final MessageArguments EMPTY = new MessageArguments(Map.of());

    private final Map<String, MessageArgument> values;

    private MessageArguments(Map<String, MessageArgument> values) {
        var copied = new LinkedHashMap<String, MessageArgument>();
        requireNonNull(values, "values").forEach((key, value) -> copied.put(
                normalizeKey(key),
                requireNonNull(value, "argument")
        ));
        this.values = Map.copyOf(copied);
    }

    private static String normalizeKey(String key) {
        return requireNonBlank(requireNonNull(key, "key").trim(), "key");
    }

    public static MessageArguments empty() {
        return EMPTY;
    }

    public static MessageArguments of(String key, MessageArgument value) {
        return builder().put(key, value).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MessageArguments of(String key, String value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, int value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, long value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, double value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, boolean value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, BigDecimal value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, UUID value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, RichText value) {
        return builder().put(key, value).build();
    }

    public static MessageArguments of(String key, LocalizedMessage value) {
        return builder().put(key, value).build();
    }

    public Map<String, MessageArgument> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public static final class Builder {

        private final Map<String, MessageArgument> values = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder put(String key, String value) {
            return put(key, MessageArgument.text(value));
        }

        public Builder put(String key, MessageArgument value) {
            values.put(normalizeKey(key), requireNonNull(value, "value"));
            return this;
        }

        public Builder put(String key, int value) {
            return put(key, MessageArgument.number(value));
        }

        public Builder put(String key, long value) {
            return put(key, MessageArgument.number(value));
        }

        public Builder put(String key, double value) {
            return put(key, MessageArgument.number(value));
        }

        public Builder put(String key, boolean value) {
            return put(key, MessageArgument.bool(value));
        }

        public Builder put(String key, BigDecimal value) {
            return put(key, MessageArgument.number(value));
        }

        public Builder put(String key, UUID value) {
            return put(key, MessageArgument.uuid(value));
        }

        public Builder put(String key, RichText value) {
            return put(key, MessageArgument.richText(value));
        }

        public Builder put(String key, LocalizedMessage value) {
            return put(key, MessageArgument.nested(value));
        }

        public Builder putAll(MessageArguments arguments) {
            requireNonNull(arguments, "arguments").values.forEach(this::put);
            return this;
        }

        public MessageArguments build() {
            return values.isEmpty()
                    ? EMPTY
                    : new MessageArguments(values);
        }

    }

}
