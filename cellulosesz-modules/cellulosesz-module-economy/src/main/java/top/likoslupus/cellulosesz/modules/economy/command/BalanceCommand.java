package top.likoslupus.cellulosesz.modules.economy.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.economy.application.BalanceCommandService;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class BalanceCommand implements CommandContributor {

    private final BalanceCommandService service;
    private final PlayerDirectory players;

    public BalanceCommand(
            BalanceCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = EconomyCommandSupport.descriptor(
                "balance",
                "cellulosesz.economy.balance",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("balance")
                .executes(command -> EconomyCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "balance self",
                        policy -> EconomyCommandSupport.currentPlayer(
                                        policy.playerUuid(),
                                        players
                                )
                                .map(service::self)
                                .orElseGet(() ->
                                        CompletableFuture.completedFuture(
                                                EconomyCommandResult.failure(
                                                        "common.player-only"
                                                )
                                        )
                                )
                ))
                .then(Commands.argument(
                                        "player",
                                        StringArgumentType.word()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.economy.balance.other"
                                ))
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                players::onlinePlayerNames,
                                                builder
                                        )
                                )
                                .executes(command -> EconomyCommandSupport.async(
                                        context,
                                        command,
                                        descriptor,
                                        "balance other",
                                        policy -> service.other(
                                                StringArgumentType.getString(command, "player"),
                                                EconomyCommandSupport.currentPlayer(
                                                                policy.playerUuid(),
                                                                players
                                                        )
                                                        .orElse(null)
                                        )
                                ))
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("bal", "money"),
                "commands.description.balance",
                "/balance [player]",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "bal",
                node
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "money",
                node
        );
    }

    @Override
    public String moduleId() {
        return EconomyCommandSupport.MODULE;
    }

}
