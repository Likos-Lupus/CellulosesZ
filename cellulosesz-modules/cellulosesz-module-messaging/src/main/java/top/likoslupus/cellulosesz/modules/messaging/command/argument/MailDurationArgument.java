package top.likoslupus.cellulosesz.modules.messaging.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.modules.messaging.service.MailDurationParser;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public final class MailDurationArgument implements ArgumentType<Duration> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(new LiteralMessage("Invalid mail duration"));

    private MailDurationArgument() {
    }

    public static MailDurationArgument duration() {
        return new MailDurationArgument();
    }

    public static Duration get(CommandContext<?> context, String name) {
        return context.getArgument(name, Duration.class);
    }

    @Override
    public Duration parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();
        var millis = MailDurationParser.parseMillis(token);

        if (millis.isEmpty() || millis.getAsLong() <= 0L) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }

        return Duration.ofMillis(millis.getAsLong());
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("30m", "2h", "7d");
    }

}
