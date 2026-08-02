package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TpoCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;

    public TpoCommand(
            TeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpo",
                "cellulosesz.teleport.tpo",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("tpo")
                .then(Commands.argument("first", EntityArgument.player())
                        .executes(command -> {
                            var targetName = EntityArgument.getPlayer(command, "first")
                                    .getGameProfile()
                                    .name();

                            return TeleportCommandResults.async(
                                    context,
                                    command,
                                    descriptor,
                                    "tpo self",
                                    policy -> service.tp(
                                            TeleportCommandResults.current(policy, players),
                                            targetName,
                                            Optional.empty(),
                                            true,
                                            true
                                    )
                            );
                        })
                        .then(Commands.argument("second", EntityArgument.player())
                                .requires(source -> context.permissions().has(
                                        source, "cellulosesz.teleport.tpo.others"
                                ))
                                .executes(command -> {
                                    var sourceName = EntityArgument.getPlayer(command, "first")
                                            .getGameProfile()
                                            .name();
                                    var targetName = EntityArgument.getPlayer(command, "second")
                                            .getGameProfile()
                                            .name();

                                    return TeleportCommandResults.async(
                                            context,
                                            command,
                                            descriptor,
                                            "tpo others",
                                            policy -> service.tp(
                                                    TeleportCommandResults.current(policy, players),
                                                    sourceName,
                                                    Optional.of(targetName),
                                                    true,
                                                    true
                                            )
                                    );
                                })
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpo",
                "/tpo <target> | <player> <target>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
