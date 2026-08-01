package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

public final class EntityTypeArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(
            new LiteralMessage("Unknown living entity")
    );
    private final EntityPlatformService entities;

    private EntityTypeArgument(EntityPlatformService entities) {
        this.entities = requireNonNull(entities, "entities");
    }

    public static EntityTypeArgument livingEntity(EntityPlatformService entities) {
        return new EntityTypeArgument(entities);
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        var normalized = value.indexOf(':') < 0
                ? "minecraft:" + value
                : value;

        if (!entities.validLivingEntity(normalized)) {
            reader.setCursor(start);
            throw UNKNOWN.createWithContext(reader);
        }

        return normalized;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("minecraft:zombie");
    }

}
