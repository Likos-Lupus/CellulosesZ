package top.likoslupus.cellulosesz.common.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import java.util.List;
import java.util.Locale;

public final class ToggleModes {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid toggle value: " + value)
    );
    private static final List<String> SUGGESTIONS = List.of("on", "off");

    private ToggleModes() {
    }

    public static ToggleMode parse(String raw) throws CommandSyntaxException {
        var token = raw.toLowerCase(Locale.ROOT);
        return switch (token) {
            case "on", "true", "enable", "enabled" -> ToggleMode.ON;
            case "off", "false", "disable", "disabled" -> ToggleMode.OFF;
            default -> throw INVALID.create(raw);
        };
    }

    public static List<String> suggestions() {
        return SUGGESTIONS;
    }

}
