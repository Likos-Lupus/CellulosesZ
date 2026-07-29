package top.likoslupus.cellulosesz.modules.warp.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.List;

/**
 * Separates a warp name from the positive integer page branch without post-parse guessing.
 */
public final class WarpNameArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType NUMERIC = new SimpleCommandExceptionType(
            new LiteralMessage("A numeric warp selector is reserved for page numbers")
    );

    private WarpNameArgument() {
    }

    public static WarpNameArgument warpName() {
        return new WarpNameArgument();
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString();

        if (value.matches("[+-]?\\d+")) {
            reader.setCursor(start);
            throw NUMERIC.createWithContext(reader);
        }

        return value;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("spawn", "market");
    }

}
