package top.likoslupus.cellulosesz.api.text;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Platform-neutral reference to a localized message template and typed placeholders. */
public record LocalizedMessage(
        String key,
        MessageArguments placeholders
) {

    public LocalizedMessage {
        key = requireNonBlank(requireNonNull(key, "key").trim(), "key");
        requireNonNull(placeholders, "placeholders");
    }

    public static LocalizedMessage of(String key) {
        return new LocalizedMessage(key, MessageArguments.empty());
    }

    public static LocalizedMessage of(String key, MessageArguments placeholders) {
        return new LocalizedMessage(key, placeholders);
    }

}
