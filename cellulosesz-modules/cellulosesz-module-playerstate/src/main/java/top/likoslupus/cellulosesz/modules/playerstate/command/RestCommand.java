package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class RestCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public RestCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "rest",
                "cellulosesz.command.rest",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("rest")
                .executes(command -> PlayerStateCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "rest self",
                        policy -> PlayerStateCommandSupport.currentPlayer(
                                        policy,
                                        players
                                )
                                .map(service::rest)
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
                                        EntityArgument.player()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.command.rest.others"
                                ))
                                .executes(command -> {
                                    var targetPlayer = MinecraftPlayers.wrap(
                                            EntityArgument.getPlayer(command, "player")
                                    );

                                    return PlayerStateCommandSupport.async(
                                            context,
                                            command,
                                            descriptor,
                                            "rest other",
                                            _ -> service.rest(targetPlayer)
                                    );
                                })
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.rest",
                "/rest [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
