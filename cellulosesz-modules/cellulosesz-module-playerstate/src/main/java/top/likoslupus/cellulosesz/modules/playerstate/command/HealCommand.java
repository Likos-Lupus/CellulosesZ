package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class HealCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public HealCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "heal",
                "cellulosesz.playerstate.heal",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("heal")
                .executes(command -> PlayerStateCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "heal self",
                        policy -> PlayerStateCommandSupport.currentPlayer(
                                        policy,
                                        players
                                )
                                .map(service::heal)
                                .orElseGet(() ->
                                        CompletableFuture.completedFuture(
                                                PlayerStateCommandResult.failure(
                                                        "common.player-only"
                                                )
                                        )
                                )
                ))
                .then(Commands.argument(
                                        "player",
                                        PlayerNameArgument.playerName()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.playerstate.heal.other"
                                ))
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                players::onlinePlayerNames,
                                                builder
                                        )
                                )
                                .executes(command -> PlayerStateCommandSupport.async(
                                        context,
                                        command,
                                        descriptor,
                                        "heal other",
                                        _ -> {
                                            var name = PlayerNameArgument.get(
                                                    command,
                                                    "player"
                                            );

                                            return players.onlinePlayer(name)
                                                    .map(service::heal)
                                                    .orElseGet(() ->
                                                            PlayerStateCommandSupport.offline(
                                                                    name
                                                            )
                                                    );
                                        }
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.heal",
                "/heal [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
