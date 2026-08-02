package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class GetPosCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;

    public GetPosCommand(
            PlayerInformationCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "getpos",
                "cellulosesz.command.getpos",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("getpos")
                .executes(command -> PlayerStateCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "getpos self",
                        policy -> PlayerStateCommandSupport.currentPlayer(
                                        policy,
                                        players
                                )
                                .map(player -> service.getPos(
                                        Optional.of(player),
                                        player
                                ))
                                .orElseGet(() ->
                                        CompletableFuture.completedFuture(
                                                PlayerStateCommandResult.failure(
                                                        "commands.playerstate.getpos.console-target-required"
                                                )
                                        )
                                )
                ))
                .then(Commands.argument(
                                        "player",
                                        EntityArgument.player()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.command.getpos.others"
                                ))
                                .executes(command -> {
                                    var targetPlayer = MinecraftPlayers.wrap(
                                            EntityArgument.getPlayer(command, "player")
                                    );

                                    return PlayerStateCommandSupport.async(
                                            context,
                                            command,
                                            descriptor,
                                            "getpos other",
                                            policy -> service.getPos(
                                                    PlayerStateCommandSupport.currentPlayer(
                                                            policy,
                                                            players
                                                    ),
                                                    targetPlayer
                                            )
                                    );
                                })
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.getpos",
                "/getpos [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
