package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportRequestCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TpaAllCommand implements CommandContributor {

    private final TeleportRequestCommandService service;
    private final PlayerDirectory players;

    public TpaAllCommand(
            TeleportRequestCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpaall",
                "cellulosesz.teleport.tpaall",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("tpaall")
                .executes(command ->
                        TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tpaall",
                                players,
                                actor -> service.createAll(
                                        actor,
                                        context.permissions().has(
                                                command.getSource(),
                                                "cellulosesz.teleport.tpaall.bypass"
                                        )
                                )
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpaall",
                "/tpaall",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
