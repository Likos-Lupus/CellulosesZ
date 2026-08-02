package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemDescriptorArgument;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class GiveCommand implements CommandContributor {

    private final ItemCommandService service;
    private final ItemService items;

    public GiveCommand(
            ItemCommandService service,
            ItemService items
    ) {
        this.service = requireNonNull(service, "service");
        this.items = requireNonNull(items, "items");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "give",
                "cellulosesz.item.give",
                CommandSourceKind.ANY
        );

        var item = Commands.argument(
                        "item",
                        ItemDescriptorArgument.itemDescriptor(items, context.buildContext())
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ItemDescriptorArgument.get(command, "item")
                ))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                withCount(
                                        ItemDescriptorArgument.get(command, "item"),
                                        IntegerArgumentType.getInteger(command, "amount")
                                )
                        ))
                );

        var target = Commands.argument("player", EntityArgument.player()).then(item);
        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.give",
                "/give <player> <item> [amount]",
                Commands.literal("give").then(target)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ItemDescriptor item
    ) {
        return ItemCommandSupport.sync(
                registration,
                command,
                descriptor,
                "give item",
                policy -> service.grant(
                        MinecraftPlayers.wrap(EntityArgument.getPlayer(command, "player")),
                        item,
                        policy.hasPermission("cellulosesz.item.give.blacklist"),
                        policy.hasPermission("cellulosesz.item.give.oversized")
                )
        );
    }

    private static ItemDescriptor withCount(ItemDescriptor item, int count) {
        return new ItemDescriptor(
                item.normalizedItem(),
                count,
                item.normalizedArgument()
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
