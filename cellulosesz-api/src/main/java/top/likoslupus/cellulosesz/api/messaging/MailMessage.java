package top.likoslupus.cellulosesz.api.messaging;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class MailMessage {

    public String id = UUID.randomUUID().toString();
    public UUID fromUuid = new UUID(0L, 0L);
    public String fromName = "console";
    public @Nullable UUID toUuid;
    public String message = "";
    public long sentAt = System.currentTimeMillis();
    public boolean read;

    public MailMessage() {
    }

}
