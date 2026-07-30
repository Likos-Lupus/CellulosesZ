package top.likoslupus.cellulosesz.modules.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.List;

/**
 * Single-token help search query that deliberately excludes integer-looking page tokens.
 */
public final class HelpQueryArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType INTEGER_QUERY = new SimpleCommandExceptionType(
            new LiteralMessage("An integer help token must be a positive page number")
    );

    private HelpQueryArgument() {
    }

    public static HelpQueryArgument query() {
        return new HelpQueryArgument();
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString();
        try {
            Integer.parseInt(value);
            reader.setCursor(start);
            throw INTEGER_QUERY.createWithContext(reader);
        } catch (NumberFormatException _) {
            return value;
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("economy", "home");
    }

}
