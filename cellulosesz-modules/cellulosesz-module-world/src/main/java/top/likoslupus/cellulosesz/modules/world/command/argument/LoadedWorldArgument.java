package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;

import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class LoadedWorldArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(
            new LiteralMessage("Unknown loaded world")
    );
    private final WorldDirectory worlds;

    private LoadedWorldArgument(WorldDirectory worlds) {
        this.worlds = requireNonNull(worlds, "worlds");
    }

    public static LoadedWorldArgument loadedWorld(WorldDirectory worlds) {
        return new LoadedWorldArgument(worlds);
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString();
        var resolved = worlds.resolveLoadedWorld(value);

        if (resolved.isEmpty()) {
            reader.setCursor(start);
            throw UNKNOWN.createWithContext(reader);
        }
        return resolved.orElseThrow();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("minecraft:overworld");
    }

}
