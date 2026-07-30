package top.likoslupus.cellulosesz.common.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight player-name token that reserves toggle words for sibling branches.
 */
public final class PlayerNameArgument implements ArgumentType<String> {

    private static final Set<String> RESERVED = Set.of(
            "on",
            "off",
            "true",
            "false",
            "enable",
            "disable",
            "enabled",
            "disabled"
    );
    private static final SimpleCommandExceptionType RESERVED_ERROR = new SimpleCommandExceptionType(
            new LiteralMessage("This token is reserved for a toggle state")
    );
    private final boolean reserveToggleWords;

    private PlayerNameArgument(boolean reserveToggleWords) {
        this.reserveToggleWords = reserveToggleWords;
    }

    public static PlayerNameArgument playerName() {
        return new PlayerNameArgument(false);
    }

    public static PlayerNameArgument playerNameWithoutToggleWords() {
        return new PlayerNameArgument(true);
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString();
        if (value.isBlank() || reserveToggleWords && RESERVED.contains(value.toLowerCase(Locale.ROOT))) {
            reader.setCursor(start);
            throw RESERVED_ERROR.createWithContext(reader);
        }
        return value;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("Alice", "Steve");
    }

}
