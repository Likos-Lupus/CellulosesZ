package top.likoslupus.cellulosesz.modules.messaging.persistence;

import top.likoslupus.cellulosesz.api.messaging.MailMessage;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class MailMapper {

    private MailMapper() {
        throw new AssertionError("No instances");
    }

    public static MailMessage toDomain(MailMessageDocument document) {
        requireNonNull(document, "document");
        try {
            return new MailMessage(
                    UUID.fromString(requireNonNull(document.id, "id")),
                    parseOptionalUuid(document.fromUuid),
                    requireNonNull(document.fromName, "fromName"),
                    UUID.fromString(requireNonNull(document.toUuid, "toUuid")),
                    requireNonNull(document.message, "message"),
                    document.sentAt,
                    document.expiresAt,
                    document.read
            );
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(
                    "Invalid persisted mail message " + document.id,
                    failure
            );
        }
    }

    private static @Nullable UUID parseOptionalUuid(@Nullable String value) {
        return value == null
                ? null
                : UUID.fromString(value);
    }

    public static MailMessageDocument fromDomain(MailMessage message) {
        requireNonNull(message, "message");
        var document = new MailMessageDocument();
        document.id = message.id().toString();
        document.fromUuid = message.fromUuid() == null
                ? null
                : message.fromUuid().toString();
        document.fromName = message.fromName();
        document.toUuid = message.toUuid().toString();
        document.message = message.message();
        document.sentAt = message.sentAt();
        document.expiresAt = message.expiresAt();
        document.read = message.read();
        return document;
    }

    public static MailMessageDocument copy(MailMessageDocument source) {
        var copy = new MailMessageDocument();
        copy.id = source.id;
        copy.fromUuid = source.fromUuid;
        copy.fromName = source.fromName;
        copy.toUuid = source.toUuid;
        copy.message = source.message;
        copy.sentAt = source.sentAt;
        copy.expiresAt = source.expiresAt;
        copy.read = source.read;
        return copy;
    }

}
