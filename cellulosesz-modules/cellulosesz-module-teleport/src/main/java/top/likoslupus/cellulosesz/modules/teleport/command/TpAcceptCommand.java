package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportRequestCommandService;
import top.likoslupus.cellulosesz.modules.teleport.command.argument.TeleportRequestSelectorArgument;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class TpAcceptCommand implements CommandContributor {

    private final TeleportRequestCommandService service;
    private final TeleportRequestService requests;
    private final PlayerDirectory players;

    public TpAcceptCommand(
            TeleportRequestCommandService service,
            TeleportRequestService requests,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.requests = requireNonNull(requests, "requests");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpaccept",
                "cellulosesz.teleport.tpaccept",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpaccept")
                .executes(command -> TeleportCommandResults.player(
                        context,
                        command,
                        descriptor,
                        "tpaccept",
                        players,
                        player -> service.accept(player, Optional.empty(), false)
                ))
                .then(Commands.argument(
                                        "selector",
                                        TeleportRequestSelectorArgument.selector()
                                )
                                .suggests((command, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                () -> context.player(command.getSource()).stream()
                                                        .flatMap(player ->
                                                                requests.pendingFor(player.uuid()).stream()
                                                        )
                                                        .flatMap(request -> Stream.of(
                                                                request.id().toString(),
                                                                players.onlinePlayer(request.requester())
                                                                        .map(CellPlayer::name)
                                                                        .orElse("")
                                                        ))
                                                        .filter(value -> !value.isBlank())
                                                        .distinct()
                                                        .toList(),
                                                builder
                                        )
                                )
                                .executes(command -> TeleportCommandResults.player(
                                        context,
                                        command,
                                        descriptor,
                                        "tpaccept selector",
                                        players,
                                        player -> service.accept(
                                                player,
                                                Optional.of(TeleportRequestSelectorArgument.get(
                                                        command, "selector"
                                                )),
                                                false
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpaccept",
                "/tpaccept [request-id|player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
