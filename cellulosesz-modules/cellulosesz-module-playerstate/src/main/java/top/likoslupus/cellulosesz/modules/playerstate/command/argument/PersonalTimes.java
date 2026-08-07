package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.api.playerstate.PersonalTimeSetting;

import java.util.List;
import java.util.Locale;

public final class PersonalTimes {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid personal time: " + value)
    );
    private static final List<String> SUGGESTIONS = List.of(
            "day", "dawn", "noon", "night", "midnight", "reset"
    );

    private PersonalTimes() {
    }

    public static PersonalTimeSetting parse(String raw) throws CommandSyntaxException {
        var token = raw.toLowerCase(Locale.ROOT);
        return switch (token) {
            case "day" -> new PersonalTimeSetting.Fixed(1_000L);
            case "dawn" -> new PersonalTimeSetting.Fixed(23_000L);
            case "noon" -> new PersonalTimeSetting.Fixed(6_000L);
            case "night" -> new PersonalTimeSetting.Fixed(13_000L);
            case "midnight" -> new PersonalTimeSetting.Fixed(18_000L);
            case "reset" -> new PersonalTimeSetting.Reset();
            default -> parseTicks(raw, token);
        };
    }

    private static PersonalTimeSetting parseTicks(
            String raw,
            String token
    ) throws CommandSyntaxException {
        try {
            var ticks = Long.parseLong(token);
            if (ticks < 0L) {
                throw new NumberFormatException();
            }

            return new PersonalTimeSetting.Fixed(Math.floorMod(ticks, 24_000L));
        } catch (NumberFormatException failure) {
            throw INVALID.create(raw);
        }
    }

    public static List<String> suggestions() {
        return SUGGESTIONS;
    }

}
