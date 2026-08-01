package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.world.WeatherType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class WeatherTypeArgument implements ArgumentType<WeatherType> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported weather type")
    );

    public static WeatherTypeArgument weatherType() {
        return new WeatherTypeArgument();
    }

    public static WeatherType get(CommandContext<?> context, String name) {
        return context.getArgument(name, WeatherType.class);
    }

    @Override
    public WeatherType parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        try {
            return WeatherType.valueOf(reader.readUnquotedString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "clear",
                "rain",
                "thunder"
        );
    }

}
