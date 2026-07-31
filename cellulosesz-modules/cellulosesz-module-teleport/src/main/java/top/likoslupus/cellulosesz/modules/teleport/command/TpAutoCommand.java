package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.argument.ToggleArgument;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportPreferenceCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TpAutoCommand implements CommandContributor {

    private final TeleportPreferenceCommandService service;
    private final PlayerDirectory players;

    public TpAutoCommand(
            TeleportPreferenceCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpauto",
                "cellulosesz.teleport.tpauto",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpauto")
                .executes(command -> TeleportCommandResults.player(
                        context,
                        command,
                        descriptor,
                        "tpauto toggle",
                        players,
                        player -> service.autoAccept(player, Optional.empty())
                ))
                .then(Commands.argument("state", ToggleArgument.toggle())
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tpauto set",
                                players,
                                player -> service.autoAccept(
                                        player,
                                        Optional.of(ToggleArgument.get(
                                                command,
                                                "state"
                                        ).enabled())
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpauto",
                "/tpauto [on|off]",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
