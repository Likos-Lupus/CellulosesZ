package top.likoslupus.cellulosesz.modules.item.command.argument;

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
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class RegistryIdArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unknown registry value")
    );
    private final Supplier<Set<String>> values;

    RegistryIdArgument(Supplier<Set<String>> values) {
        this.values = requireNonNull(values, "values");
    }

    static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var raw = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        var normalized = raw.indexOf(':') < 0 ? "minecraft:" + raw : raw;

        if (!values.get().contains(normalized)) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }

        return normalized;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("minecraft:speed");
    }

}
