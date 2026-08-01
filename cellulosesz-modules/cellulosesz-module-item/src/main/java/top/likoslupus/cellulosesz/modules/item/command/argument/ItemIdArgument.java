package top.likoslupus.cellulosesz.modules.item.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.item.ItemService;

import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ItemIdArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Unknown item")
    );
    private final ItemService items;

    private ItemIdArgument(ItemService items) {
        this.items = requireNonNull(items, "items");
    }

    public static ItemIdArgument itemId(ItemService items) {
        return new ItemIdArgument(items);
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var token = reader.readUnquotedString();
        var parsed = items.parse(token);

        if (parsed.isEmpty()
                || parsed.orElseThrow().count != 1
                || !parsed.orElseThrow().normalizedComponents().isEmpty()
                || !items.valid(parsed.orElseThrow())
        ) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }

        return parsed.orElseThrow().normalizedItem();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "stone",
                "minecraft:diamond"
        );
    }

}
