package top.likoslupus.cellulosesz.modules.kit.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.modules.kit.application.KitCooldown;

import java.util.List;
import java.util.Locale;

public final class KitCooldowns {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid kit cooldown: " + value)
    );
    private static final List<String> SUGGESTIONS = List.of("once", "one-time");

    private KitCooldowns() {
    }

    public static KitCooldown parse(String raw) throws CommandSyntaxException {
        var token = raw.toLowerCase(Locale.ROOT);
        if (token.equals("once") || token.equals("one-time")) {
            return new KitCooldown.Once();
        }

        try {
            if (token.startsWith("+")) {
                throw new NumberFormatException();
            }

            var seconds = Long.parseLong(token);
            if (seconds < 0L) {
                throw new NumberFormatException();
            }

            return new KitCooldown.Seconds(seconds);
        } catch (NumberFormatException failure) {
            throw INVALID.create(raw);
        }
    }

    public static List<String> suggestions() {
        return SUGGESTIONS;
    }

}
