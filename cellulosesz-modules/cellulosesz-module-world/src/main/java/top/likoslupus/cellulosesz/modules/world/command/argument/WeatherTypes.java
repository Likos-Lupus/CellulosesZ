package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.world.WeatherType;

import java.util.List;
import java.util.Locale;

public final class WeatherTypes {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported weather type")
    );
    private static final List<String> SUGGESTIONS = List.of("clear", "rain", "thunder");

    private WeatherTypes() {
    }

    public static WeatherType parse(String raw) throws CommandSyntaxException {
        try {
            return WeatherType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw INVALID.create();
        }
    }

    public static List<String> suggestions() {
        return SUGGESTIONS;
    }

}
