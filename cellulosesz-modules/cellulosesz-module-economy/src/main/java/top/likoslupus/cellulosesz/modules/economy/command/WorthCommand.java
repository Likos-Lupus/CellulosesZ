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
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandResult;
import top.likoslupus.cellulosesz.modules.economy.application.ItemValueCommandService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class WorthCommand implements CommandContributor {

    private final ItemValueCommandService service;
    private final PlayerDirectory players;

    public WorthCommand(
            ItemValueCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "worth",
                "cellulosesz.economy.worth",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("worth")
                .executes(command -> playerWorth(
                        context,
                        command,
                        descriptor,
                        ItemValueCommandService.WorthSelector.HAND
                ))
                .then(Commands.literal("hand")
                        .executes(command -> playerWorth(
                                context,
                                command,
                                descriptor,
                                ItemValueCommandService.WorthSelector.HAND
                        ))
                )
                .then(Commands.literal("inventory")
                        .executes(command -> playerWorth(
                                context,
                                command,
                                descriptor,
                                ItemValueCommandService
                                        .WorthSelector.INVENTORY
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
                                .executes(command -> itemWorth(
                                        context,
                                        command,
                                        descriptor,
                                        1
                                ))
                                .then(Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType.integer(1)
                                                )
                                                .executes(command -> itemWorth(
                                                        context,
                                                        command,
                                                        descriptor,
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
                "commands.description.worth",
                "/worth [hand|inventory|item [amount]]",
                root
        );
    }

    private int playerWorth(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ItemValueCommandService.WorthSelector selector
    ) {
        return EconomyCommandSupport.async(
                context,
                command,
                descriptor,
                "worth inventory",
                policy -> EconomyCommandSupport.currentPlayer(
                                policy.playerUuid(),
                                players
                        )
                        .map(player -> service.worth(
                                player,
                                selector
                        ))
                        .orElseGet(() ->
                                CompletableFuture.completedFuture(
                                        EconomyCommandResult.failure(
                                                "commands.economy"
                                                        + ".worth-command"
                                                        + ".error.usage"
                                        )
                                )
                        )
        );
    }

    private int itemWorth(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int amount
    ) {
        return EconomyCommandSupport.async(
                context,
                command,
                descriptor,
                "worth item",
                _ -> service.worthItem(
                        StringArgumentType.getString(
                                command,
                                "item"
                        ),
                        amount
                )
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
