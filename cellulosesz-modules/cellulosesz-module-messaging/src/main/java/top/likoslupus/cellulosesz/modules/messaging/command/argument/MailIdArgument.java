package top.likoslupus.cellulosesz.modules.messaging.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class MailIdArgument implements ArgumentType<UUID> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(new LiteralMessage("Invalid mail id"));

    private MailIdArgument() {
    }

    public static MailIdArgument mailId() {
        return new MailIdArgument();
    }

    public static UUID get(CommandContext<?> context, String name) {
        return context.getArgument(name, UUID.class);
    }

    @Override
    public UUID parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();

        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException failure) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("123e4567-e89b-12d3-a456-426614174000");
    }

}
