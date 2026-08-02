package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportRequestCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TpaHereCommand implements CommandContributor {

    private final TeleportRequestCommandService service;
    private final PlayerDirectory players;

    public TpaHereCommand(
            TeleportRequestCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpahere",
                "cellulosesz.teleport.tpahere",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpahere")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tpahere request",
                                players,
                                actor -> service.create(
                                        actor,
                                        EntityArgument.getPlayer(command, "player")
                                                .getGameProfile()
                                                .name(),
                                        TeleportRequestType.TARGET_TO_REQUESTER,
                                        context.permissions().has(
                                                command.getSource(),
                                                "cellulosesz.teleport.tpahere.bypass"
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpahere",
                "/tpahere <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
