package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemDescriptors;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class GiveCommand implements CommandContributor {

    private final ItemCommandService service;
    private final ItemService items;

    public GiveCommand(ItemCommandService service, ItemService items) {
        this.service = requireNonNull(service, "service");
        this.items = requireNonNull(items, "items");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        ItemDescriptors.prepare(items);
        var descriptor = ItemCommandSupport.descriptor(
                "give",
                "cellulosesz.item.give",
                CommandSourceKind.ANY
        );

        var target = Commands.argument("player", EntityArgument.player())
                .then(configureItem(
                        context,
                        descriptor,
                        Commands.argument("item", ItemArgument.item(context.buildContext())),
                        "item",
                        true
                ))
                .then(configureItem(
                        context,
                        descriptor,
                        Commands.argument("itemAlias", StringArgumentType.word())
                                .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                        items::itemNames,
                                        builder
                                )),
                        "itemAlias",
                        false
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.give",
                "/give <player> <item> [amount]",
                Commands.literal("give").then(target)
        );
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> configureItem(
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            RequiredArgumentBuilder<CommandSourceStack, ?> argument,
            String argumentName,
            boolean vanilla
    ) {
        return argument
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        item(command, argumentName, vanilla)
                ))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                withCount(
                                        item(command, argumentName, vanilla),
                                        IntegerArgumentType.getInteger(command, "amount")
                                )
                        ))
                );
    }

    private ItemDescriptor item(
            CommandContext<CommandSourceStack> command,
            String argumentName,
            boolean vanilla
    ) throws CommandSyntaxException {
        return vanilla
                ? ItemDescriptors.vanilla(command, argumentName)
                : ItemDescriptors.custom(command, argumentName, items);
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ItemDescriptor item
    ) throws CommandSyntaxException {
        var target = MinecraftPlayers.wrap(EntityArgument.getPlayer(command, "player"));
        return ItemCommandSupport.sync(
                registration,
                command,
                descriptor,
                "give item",
                policy -> service.grant(
                        target,
                        item,
                        policy.hasPermission("cellulosesz.item.give.blacklist"),
                        policy.hasPermission("cellulosesz.item.give.oversized")
                )
        );
    }

    private static ItemDescriptor withCount(ItemDescriptor item, int count) {
        return new ItemDescriptor(item.normalizedItem(), count, item.normalizedArgument());
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
