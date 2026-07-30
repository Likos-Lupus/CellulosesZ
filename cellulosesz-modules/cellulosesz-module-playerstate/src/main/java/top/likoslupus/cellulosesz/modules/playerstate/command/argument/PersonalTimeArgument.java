package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import top.likoslupus.cellulosesz.api.playerstate.PersonalTimeSetting;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class PersonalTimeArgument implements ArgumentType<PersonalTimeSetting> {

    private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
            value -> new LiteralMessage("Invalid personal time: " + value)
    );

    private PersonalTimeArgument() {
    }

    public static PersonalTimeArgument time() {
        return new PersonalTimeArgument();
    }

    public static PersonalTimeSetting get(CommandContext<?> context, String name) {
        return context.getArgument(name, PersonalTimeSetting.class);
    }

    @Override
    public PersonalTimeSetting parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString().toLowerCase(Locale.ROOT);

        return switch (token) {
            case "day" -> new PersonalTimeSetting.Fixed(1000L);
            case "dawn" -> new PersonalTimeSetting.Fixed(23000L);
            case "noon" -> new PersonalTimeSetting.Fixed(6000L);
            case "night" -> new PersonalTimeSetting.Fixed(13000L);
            case "midnight" -> new PersonalTimeSetting.Fixed(18000L);
            case "reset" -> new PersonalTimeSetting.Reset();
            default -> {
                try {
                    long ticks = Long.parseLong(token);
                    if (ticks < 0L) {
                        reader.setCursor(start);
                        throw INVALID.createWithContext(reader, token);
                    }
                    yield new PersonalTimeSetting.Fixed(Math.floorMod(ticks, 24000L));
                } catch (NumberFormatException _) {
                    reader.setCursor(start);
                    throw INVALID.createWithContext(reader, token);
                }
            }
        };
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("day", "night", "reset", "6000");
    }

}
