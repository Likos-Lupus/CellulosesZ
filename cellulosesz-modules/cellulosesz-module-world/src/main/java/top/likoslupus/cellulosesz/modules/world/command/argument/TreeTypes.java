package top.likoslupus.cellulosesz.modules.world.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.common.world.TreeType;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TreeTypes {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unsupported tree type")
    );
    private static final List<String> NORMAL_SUGGESTIONS = List.of(
            "oak", "tree", "birch", "redwood", "spruce", "redmushroom", "red_mushroom",
            "brownmushroom", "brown_mushroom", "jungle", "junglebush", "jungle_bush", "swamp"
    );
    private static final List<String> LARGE_SUGGESTIONS = List.of(
            "oak", "tree", "redwood", "spruce", "jungle", "darkoak", "dark_oak"
    );

    private TreeTypes() {
    }

    public static TreeType parseNormal(
            String raw,
            Set<TreeType> allowed
    ) throws CommandSyntaxException {
        return parse(raw, allowed, false);
    }

    private static TreeType parse(
            String raw,
            Set<TreeType> allowed,
            boolean large
    ) throws CommandSyntaxException {
        var value = raw.toLowerCase(Locale.ROOT);
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
            throw INVALID.create();
        }

        return type;
    }

    public static TreeType parseLarge(
            String raw,
            Set<TreeType> allowed
    ) throws CommandSyntaxException {
        return parse(raw, allowed, true);
    }

    public static List<String> normalSuggestions() {
        return NORMAL_SUGGESTIONS;
    }

    public static List<String> largeSuggestions() {
        return LARGE_SUGGESTIONS;
    }

}
