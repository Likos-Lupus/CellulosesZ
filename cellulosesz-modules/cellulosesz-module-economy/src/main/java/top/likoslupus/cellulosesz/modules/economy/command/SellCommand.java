package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.economy.application.ItemValueCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SellCommand implements CommandContributor {

    private final ItemValueCommandService service;
    private final PlayerDirectory players;

    public SellCommand(
            ItemValueCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "sell",
                "cellulosesz.economy.sell",
                CommandSourceKind.PLAYER_ONLY
        );

        var hand = Commands.literal("hand")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ItemValueCommandService.SellSelector.HAND,
                        Optional.empty(),
                        1
                ))
                .then(Commands.argument(
                                        "amount",
                                        IntegerArgumentType.integer(1)
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        ItemValueCommandService.SellSelector.HAND,
                                        Optional.empty(),
                                        IntegerArgumentType.getInteger(
                                                command,
                                                "amount"
                                        )
                                ))
                );

        var root = Commands.literal("sell")
                .then(hand)
                .then(Commands.literal("all")
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                ItemValueCommandService.SellSelector.ALL,
                                Optional.empty(),
                                1
                        ))
                )
                .then(Commands.argument(
                                        "item",
                                        StringArgumentType.word()
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                service::itemNames,
                                                builder
                                        )
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        ItemValueCommandService.SellSelector.ITEM,
                                        Optional.of(
                                                StringArgumentType.getString(
                                                        command,
                                                        "item"
                                                )
                                        ),
                                        1
                                ))
                                .then(Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType.integer(1)
                                                )
                                                .executes(command -> execute(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        ItemValueCommandService.SellSelector.ITEM,
                                                        Optional.of(
                                                                StringArgumentType.getString(
                                                                        command,
                                                                        "item"
                                                                )
                                                        ),
                                                        IntegerArgumentType.getInteger(
                                                                command,
                                                                "amount"
                                                        )
                                                ))
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.sell",
                "/sell <hand [amount]|all|item [amount]>",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ItemValueCommandService.SellSelector selector,
            Optional<String> item,
            int amount
    ) {
        return EconomyCommandSupport.requirePlayer(
                context,
                command,
                descriptor,
                "sell",
                players,
                player -> service.sell(
                        player,
                        selector,
                        item,
                        amount
                )
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
