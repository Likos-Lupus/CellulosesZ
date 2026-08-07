package top.likoslupus.cellulosesz.modules.item.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemService;

import static java.util.Objects.requireNonNull;

/** Converts vanilla and CellulosesZ item command inputs into the shared domain descriptor. */
public final class ItemDescriptors {

    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            new LiteralMessage("Invalid item descriptor")
    );

    private ItemDescriptors() {
    }

    public static void prepare(ItemService items) {
        requireNonNull(items, "items");
        if (items instanceof DefaultItemService service) {
            service.validateRegistryInputs();
        }
    }

    public static ItemDescriptor vanilla(
            CommandContext<CommandSourceStack> context,
            String name
    ) throws CommandSyntaxException {
        var parsed = ItemArgument.getItem(context, name);
        var stack = parsed.createItemStack(1);
        return new ItemDescriptor(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                1,
                rawArgument(context, name)
        );
    }

    private static String rawArgument(
            CommandContext<?> context,
            String name
    ) throws CommandSyntaxException {
        for (var parsed : context.getNodes()) {
            if (parsed.getNode().getName().equals(name)) {
                return parsed.getRange().get(context.getInput());
            }
        }
        throw INVALID.create();
    }

    public static ItemDescriptor custom(
            CommandContext<CommandSourceStack> context,
            String name,
            ItemService items
    ) throws CommandSyntaxException {
        var parsed = items.parse(rawArgument(context, name));
        if (!parsed.successful() || parsed.value().isEmpty()) {
            throw INVALID.create();
        }

        var descriptor = parsed.value().orElseThrow();

        var valid = items.valid(descriptor);
        if (!valid.successful() || valid.value().isEmpty() || !valid.value().orElseThrow()) {
            throw INVALID.create();
        }

        return descriptor;
    }

}
