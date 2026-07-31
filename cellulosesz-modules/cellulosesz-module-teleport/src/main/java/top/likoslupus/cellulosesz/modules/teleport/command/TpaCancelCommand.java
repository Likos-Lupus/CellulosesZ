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

public final class TpaCancelCommand implements CommandContributor {

    private final TeleportRequestCommandService service;
    private final TeleportRequestService requests;
    private final PlayerDirectory players;

    public TpaCancelCommand(
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
                "tpacancel",
                "cellulosesz.teleport.tpacancel",
                CommandSourceKind.PLAYER_ONLY
        );

        @SuppressWarnings("NullableProblems")
        var root = Commands.literal("tpacancel")
                .executes(command -> TeleportCommandResults.player(
                        context,
                        command,
                        descriptor,
                        "tpacancel",
                        players,
                        player -> service.cancel(player, Optional.empty())
                ))
                .then(Commands.argument(
                                        "selector",
                                        TeleportRequestSelectorArgument.selector()
                                )
                                .suggests((command, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                () -> context.player(command.getSource()).stream()
                                                        .flatMap(player ->
                                                                requests.outgoingFor(player.uuid()).stream()
                                                        )
                                                        .flatMap(request -> Stream.of(
                                                                request.id().toString(),
                                                                players.onlinePlayer(request.target())
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
                                        "tpacancel selector",
                                        players,
                                        player -> service.cancel(
                                                player,
                                                Optional.of(TeleportRequestSelectorArgument.get(
                                                        command, "selector"
                                                ))
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpacancel",
                "/tpacancel [request-id|player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
