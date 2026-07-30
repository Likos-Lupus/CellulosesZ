package top.likoslupus.cellulosesz.common.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class ToggleArgument implements ArgumentType<ToggleMode> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid toggle value: " + value)
    );

    private ToggleArgument() {
    }

    public static ToggleArgument toggle() {
        return new ToggleArgument();
    }

    public static ToggleMode get(CommandContext<?> context, String name) {
        return context.getArgument(name, ToggleMode.class);
    }

    @Override
    public ToggleMode parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        return switch (token) {
            case "on", "true", "enable", "enabled" -> ToggleMode.ON;
            case "off", "false", "disable", "disabled" -> ToggleMode.OFF;
            default -> {
                reader.setCursor(start);
                throw INVALID.createWithContext(reader, token);
            }
        };
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("on", "off");
    }

}
