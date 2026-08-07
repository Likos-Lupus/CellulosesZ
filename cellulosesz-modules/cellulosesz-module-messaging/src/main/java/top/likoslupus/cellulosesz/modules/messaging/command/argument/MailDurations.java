package top.likoslupus.cellulosesz.modules.messaging.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.modules.messaging.service.MailDurationParser;

import java.time.Duration;

public final class MailDurations {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Invalid mail duration")
    );

    private MailDurations() {
    }

    public static Duration parse(String token) throws CommandSyntaxException {
        var millis = MailDurationParser.parseMillis(token);
        if (millis.isEmpty() || millis.getAsLong() <= 0L) {
            throw INVALID.create();
        }

        return Duration.ofMillis(millis.getAsLong());
    }

}
