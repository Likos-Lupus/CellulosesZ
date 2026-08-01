package top.likoslupus.cellulosesz.modules.item.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;

import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Terminal typed item argument. It consumes the descriptor tail, including count and components.
 */
public final class ItemDescriptorArgument implements ArgumentType<ItemDescriptor> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Invalid item descriptor")
    );
    private final ItemService items;

    private ItemDescriptorArgument(ItemService items) {
        this.items = requireNonNull(items, "items");
    }

    public static ItemDescriptorArgument itemDescriptor(ItemService items) {
        return new ItemDescriptorArgument(items);
    }

    public static ItemDescriptor get(CommandContext<?> context, String name) {
        return context.getArgument(name, ItemDescriptor.class);
    }

    @Override
    public ItemDescriptor parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var input = reader.getRemaining();
        var parsed = items.parse(input);

        if (parsed.isEmpty() || !items.valid(parsed.orElseThrow())) {
            reader.setCursor(start);
            throw INVALID.createWithContext(reader);
        }

        reader.setCursor(reader.getTotalLength());
        return parsed.orElseThrow().copy();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "minecraft:stone",
                "diamond 16",
                "minecraft:book[minecraft:custom_name='Guide']"
        );
    }

}
