package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class CompassCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;

    public CompassCommand(
            PlayerInformationCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    public static double normalizeDegrees(double yaw) {
        return PlayerInformationCommandService.normalizeDegrees(yaw);
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "compass",
                "cellulosesz.command.compass",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("compass")
                .executes(command ->
                        PlayerStateCommandSupport.requirePlayer(
                                context,
                                command,
                                descriptor,
                                "compass",
                                players,
                                service::compass
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.compass",
                "/compass",
                root
        );
    }


    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
