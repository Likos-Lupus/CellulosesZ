package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.world.TreeType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TreeTypeArgument implements ArgumentType<TreeType> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported tree type")
    );
    private final Set<TreeType> allowed;
    private final boolean large;

    private TreeTypeArgument(
            Set<TreeType> allowed,
            boolean large
    ) {
        this.allowed = Set.copyOf(allowed);
        this.large = large;
    }

    public static TreeTypeArgument treeType(Set<TreeType> allowed) {
        return new TreeTypeArgument(allowed, false);
    }

    public static TreeTypeArgument bigTreeType(Set<TreeType> allowed) {
        return new TreeTypeArgument(allowed, true);
    }

    public static TreeType get(CommandContext<?> context, String name) {
        return context.getArgument(name, TreeType.class);
    }

    @Override
    public TreeType parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var value = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        var type = large
                ?
                switch (value) {
                    case "tree", "oak" -> TreeType.LARGE_OAK;
                    case "redwood", "spruce" -> TreeType.LARGE_SPRUCE;
                    case "jungle" -> TreeType.LARGE_JUNGLE;
                    case "darkoak", "dark_oak" -> TreeType.DARK_OAK;
                    default -> null;
                }
                : switch (value) {
                    case "tree", "oak" -> TreeType.OAK;
                    case "birch" -> TreeType.BIRCH;
                    case "redwood", "spruce" -> TreeType.SPRUCE;
                    case "redmushroom", "red_mushroom" -> TreeType.RED_MUSHROOM;
                    case "brownmushroom", "brown_mushroom" -> TreeType.BROWN_MUSHROOM;
                    case "jungle" -> TreeType.JUNGLE;
                    case "junglebush", "jungle_bush" -> TreeType.JUNGLE_BUSH;
                    case "swamp" -> TreeType.SWAMP;
                    default -> null;
                };

        if (type == null || !allowed.contains(type)) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }
        return type;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("oak", "spruce", "jungle");
    }

}
