package top.likoslupus.cellulosesz.api.messaging;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record MailMessage(
        UUID id,
        @Nullable UUID fromUuid,
        String fromName,
        UUID toUuid,
        String message,
        long sentAt,
        @Nullable Long expiresAt,
        boolean read
) {

    public MailMessage {
        requireNonNull(id, "id");
        fromName = requireText(fromName, "fromName");
        requireNonNull(toUuid, "toUuid");
        message = requireText(message, "message");
        if (sentAt <= 0L) throw new IllegalArgumentException("sentAt must be positive");
        if (expiresAt != null && expiresAt <= sentAt) {
            throw new IllegalArgumentException("expiresAt must be after sentAt");
        }
    }

    private static String requireText(String value, String name) {
        requireNonNull(value, name);
        var trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return trimmed;
    }

    public boolean expired(long now) {
        return expiresAt != null && expiresAt <= now;
    }

    public MailMessage withRead(boolean value) {
        return new MailMessage(id, fromUuid, fromName, toUuid, message, sentAt, expiresAt, value);
    }

}
