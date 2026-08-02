package top.likoslupus.cellulosesz.modules.messaging.persistence;

import org.jspecify.annotations.Nullable;

public final class MailMessageDocument {

    public @Nullable String id;
    public @Nullable String fromUuid;
    public @Nullable String fromName;
    public @Nullable String toUuid;
    public @Nullable String message;
    public long sentAt;
    public @Nullable Long expiresAt;
    public boolean read;

}
