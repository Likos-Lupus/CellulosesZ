package top.likoslupus.cellulosesz.modules.messaging;

import java.time.ZoneId;

import static java.util.Objects.requireNonNull;

public final class MessagingConfig {

    public int maxMailPerPlayer = 100;
    public int mailPageSize = 10;
    public int maxMessageLength = 512;
    public long maximumTemporaryMailSeconds = 2_592_000L;
    public long expiredMailSweepSeconds = 60L;
    public int maxSendAllRecipients = 100_000;
    public String mailTimeZone = "UTC";
    public boolean allowConsolePrivateMessage = true;

    public void copyFrom(MessagingConfig source) {
        var validated = source.validatedCopy();
        maxMailPerPlayer = validated.maxMailPerPlayer;
        mailPageSize = validated.mailPageSize;
        maxMessageLength = validated.maxMessageLength;
        maximumTemporaryMailSeconds = validated.maximumTemporaryMailSeconds;
        expiredMailSweepSeconds = validated.expiredMailSweepSeconds;
        maxSendAllRecipients = validated.maxSendAllRecipients;
        mailTimeZone = validated.mailTimeZone;
        allowConsolePrivateMessage = validated.allowConsolePrivateMessage;
    }

    public MessagingConfig validatedCopy() {
        if (maxMailPerPlayer < 1) throw new IllegalArgumentException("maxMailPerPlayer must be positive");
        if (mailPageSize < 1) throw new IllegalArgumentException("mailPageSize must be positive");
        if (maxMessageLength < 1) throw new IllegalArgumentException("maxMessageLength must be positive");
        if (maximumTemporaryMailSeconds < 1L)
            throw new IllegalArgumentException("maximumTemporaryMailSeconds must be positive");
        if (expiredMailSweepSeconds < 1L)
            throw new IllegalArgumentException("expiredMailSweepSeconds must be positive");
        if (maxSendAllRecipients < 1) throw new IllegalArgumentException("maxSendAllRecipients must be positive");
        ZoneId.of(requireNonNull(mailTimeZone, "mailTimeZone"));

        var copy = new MessagingConfig();
        copy.maxMailPerPlayer = maxMailPerPlayer;
        copy.mailPageSize = mailPageSize;
        copy.maxMessageLength = maxMessageLength;
        copy.maximumTemporaryMailSeconds = maximumTemporaryMailSeconds;
        copy.expiredMailSweepSeconds = expiredMailSweepSeconds;
        copy.maxSendAllRecipients = maxSendAllRecipients;
        copy.mailTimeZone = mailTimeZone;
        copy.allowConsolePrivateMessage = allowConsolePrivateMessage;
        return copy;
    }

}
