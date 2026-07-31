package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BackCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;

    public BackCommand(
            TeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "back",
                "cellulosesz.teleport.back",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("back")
                .executes(command ->
                        TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "back",
                                players,
                                service::back
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.back",
                "/back",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
