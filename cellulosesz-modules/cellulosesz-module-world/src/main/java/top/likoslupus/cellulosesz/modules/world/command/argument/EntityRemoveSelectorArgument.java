package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.world.EntityRemoveSelector;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class EntityRemoveSelectorArgument implements ArgumentType<EntityRemoveSelector> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported entity removal selector")
    );

    public static EntityRemoveSelectorArgument selector() {
        return new EntityRemoveSelectorArgument();
    }

    public static EntityRemoveSelector get(CommandContext<?> context, String name) {
        return context.getArgument(name, EntityRemoveSelector.class);
    }

    @Override
    public EntityRemoveSelector parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        var selector = switch (value) {
            case "all" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.ALL);
            case "animals" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.ANIMALS);
            case "monsters" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.MONSTERS);
            case "items" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.ITEMS);
            case "projectiles" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.PROJECTILES);
            case "boats" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.BOATS);
            case "minecarts" -> EntityRemoveSelector.of(EntityRemoveSelector.Kind.MINECARTS);
            default -> value.matches("[a-z0-9_.-]+(?::[a-z0-9_./-]+)?")
                    ?
                    EntityRemoveSelector.entity(
                            value.indexOf(':') < 0
                                    ? "minecraft:" + value
                                    : value
                    )
                    : null;
        };

        if (selector == null) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }
        return selector;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "all",
                "monsters",
                "minecraft:zombie"
        );
    }

}
