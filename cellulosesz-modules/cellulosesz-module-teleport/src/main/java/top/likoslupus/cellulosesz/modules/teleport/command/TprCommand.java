package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.RandomTeleportCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TprCommand implements CommandContributor {

    private final RandomTeleportCommandService service;
    private final PlayerDirectory players;

    public TprCommand(
            RandomTeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpr",
                "cellulosesz.teleport.random",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("tpr")
                .executes(command ->
                        TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tpr",
                                players,
                                service::random
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpr",
                "/tpr",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
