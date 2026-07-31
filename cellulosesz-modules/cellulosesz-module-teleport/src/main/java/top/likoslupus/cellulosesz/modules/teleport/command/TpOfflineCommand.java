package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TpOfflineCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;

    public TpOfflineCommand(
            TeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpoffline",
                "cellulosesz.teleport.tpoffline",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpoffline")
                .then(Commands.argument("player", PlayerNameArgument.playerName())
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tpoffline",
                                players,
                                player -> service.offline(
                                        player,
                                        PlayerNameArgument.get(command, "player")
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpoffline",
                "/tpoffline <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
