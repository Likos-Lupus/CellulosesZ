package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportRequestCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class TpaCommand implements CommandContributor {

    private final TeleportRequestCommandService service;
    private final PlayerDirectory players;

    public TpaCommand(
            TeleportRequestCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpa",
                "cellulosesz.teleport.tpa",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpa")
                .then(Commands.argument("player", PlayerNameArgument.playerName())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        players::onlinePlayerNames, builder
                                )
                        )
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "tpa request",
                                players,
                                actor -> service.create(
                                        actor,
                                        PlayerNameArgument.get(command, "player"),
                                        TeleportRequestType.REQUESTER_TO_TARGET,
                                        context.permissions().has(
                                                command.getSource(),
                                                "cellulosesz.teleport.tpa.bypass"
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpa",
                "/tpa <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
