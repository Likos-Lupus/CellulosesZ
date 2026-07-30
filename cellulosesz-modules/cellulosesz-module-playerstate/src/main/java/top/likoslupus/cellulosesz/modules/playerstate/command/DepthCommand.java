package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class DepthCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;

    public DepthCommand(
            PlayerInformationCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "depth",
                "cellulosesz.command.depth",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("depth")
                .executes(command ->
                        PlayerStateCommandSupport.requirePlayer(
                                context,
                                command,
                                descriptor,
                                "depth",
                                players,
                                service::depth
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.depth",
                "/depth",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
