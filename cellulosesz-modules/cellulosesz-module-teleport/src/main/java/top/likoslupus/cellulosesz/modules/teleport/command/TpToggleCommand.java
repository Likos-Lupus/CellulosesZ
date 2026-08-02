package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportPreferenceCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TpToggleCommand implements CommandContributor {

    private final TeleportPreferenceCommandService service;
    private final PlayerDirectory players;

    public TpToggleCommand(
            TeleportPreferenceCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tptoggle",
                "cellulosesz.teleport.tptoggle",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("tptoggle")
                .executes(command -> TeleportCommandResults.player(
                        context,
                        command,
                        descriptor,
                        "tptoggle self",
                        players,
                        player -> service.toggle(
                                player, Optional.empty(), Optional.empty()
                        )
                ))
                .then(Commands.literal("on")
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tptoggle self set",
                                players,
                                player -> service.toggle(
                                        player, Optional.empty(), Optional.of(true)
                                )
                        ))
                )
                .then(Commands.literal("off")
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tptoggle self set",
                                players,
                                player -> service.toggle(
                                        player, Optional.empty(), Optional.of(false)
                                )
                        ))
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.teleport.tptoggle.others"
                        ))
                        .executes(command -> {
                            var targetName = EntityArgument.getPlayer(command, "player")
                                    .getGameProfile()
                                    .name();

                            return TeleportCommandResults.player(
                                    context,
                                    command,
                                    descriptor,
                                    "tptoggle other",
                                    players,
                                    player -> service.toggle(
                                            player,
                                            Optional.of(targetName),
                                            Optional.empty()
                                    )
                            );
                        })
                        .then(Commands.literal("on")
                                .executes(command -> {
                                    var targetName = EntityArgument.getPlayer(command, "player")
                                            .getGameProfile()
                                            .name();

                                    return TeleportCommandResults.player(
                                            context,
                                            command,
                                            descriptor,
                                            "tptoggle other set",
                                            players,
                                            player -> service.toggle(
                                                    player,
                                                    Optional.of(targetName),
                                                    Optional.of(true)
                                            )
                                    );
                                })
                        )
                        .then(Commands.literal("off")
                                .executes(command -> {
                                    var targetName = EntityArgument.getPlayer(command, "player")
                                            .getGameProfile()
                                            .name();

                                    return TeleportCommandResults.player(
                                            context,
                                            command,
                                            descriptor,
                                            "tptoggle other set",
                                            players,
                                            player -> service.toggle(
                                                    player,
                                                    Optional.of(targetName),
                                                    Optional.of(false)
                                            )
                                    );
                                })
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tptoggle",
                "/tptoggle [on|off|player [on|off]]",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
