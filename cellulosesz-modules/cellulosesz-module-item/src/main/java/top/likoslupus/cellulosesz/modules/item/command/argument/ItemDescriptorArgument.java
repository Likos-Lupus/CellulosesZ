package top.likoslupus.cellulosesz.modules.item.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * Business item descriptor argument. Vanilla owns item/component parsing; this wrapper only adds
 * CellulosesZ aliases and custom-item expansion.
 */
public final class ItemDescriptorArgument implements ArgumentType<ItemDescriptor> {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Invalid item descriptor")
    );

    private final ItemService items;
    private final ItemArgument vanilla;

    private ItemDescriptorArgument(
            ItemService items,
            CommandBuildContext buildContext
    ) {
        this.items = requireNonNull(items, "items");
        if (items instanceof DefaultItemService service) {
            service.validateRegistryInputs();
        }
        this.vanilla = ItemArgument.item(requireNonNull(buildContext, "buildContext"));
    }

    public static ItemDescriptorArgument itemDescriptor(
            ItemService items,
            CommandBuildContext buildContext
    ) {
        return new ItemDescriptorArgument(items, buildContext);
    }

    public static ItemDescriptor get(CommandContext<?> context, String name) {
        return context.getArgument(name, ItemDescriptor.class);
    }

    @Override
    public ItemDescriptor parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        try {
            ItemInput parsed = vanilla.parse(reader);
            var itemEnd = reader.getCursor();

            var stack = parsed.createItemStack(1);
            return new ItemDescriptor(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    1,
                    reader.getString().substring(start, itemEnd)
            );
        } catch (CommandSyntaxException vanillaFailure) {
            reader.setCursor(start);
            var fallback = items.parse(reader.readUnquotedString());
            if (fallback.isEmpty() || !items.valid(fallback.orElseThrow())) {
                reader.setCursor(start);
                throw INVALID.createWithContext(reader);
            }

            return fallback.orElseThrow().copy();
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> context,
            SuggestionsBuilder builder
    ) {
        return vanilla.listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "minecraft:stone",
                "minecraft:book[minecraft:custom_name='Guide']"
        );
    }

}
