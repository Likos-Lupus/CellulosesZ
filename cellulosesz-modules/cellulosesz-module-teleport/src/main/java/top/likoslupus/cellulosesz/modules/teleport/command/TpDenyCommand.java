package top.likoslupus.cellulosesz.modules.teleport.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportRequestCommandService;
import top.likoslupus.cellulosesz.modules.teleport.command.argument.TeleportRequestSelectors;
import top.likoslupus.cellulosesz.modules.teleport.service.TeleportRequestService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class TpDenyCommand implements CommandContributor {

    private final TeleportRequestCommandService service;
    private final TeleportRequestService requests;
    private final PlayerDirectory players;

    public TpDenyCommand(
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
                "tpdeny",
                "cellulosesz.teleport.tpdeny",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("tpdeny")
                .executes(command -> TeleportCommandResults.player(
                        context,
                        command,
                        descriptor,
                        "tpdeny",
                        players,
                        player -> service.deny(player, Optional.empty())
                ))
                .then(Commands.argument("selector", StringArgumentType.word())
                        .suggests((command, builder) -> CommandSuggestionSupport.suggest(
                                () -> context.player(command.getSource())
                                        .map(player -> requests.pendingFor(player.uuid()).stream()
                                                .flatMap(request -> Stream.of(
                                                        request.id().toString(),
                                                        Optional.ofNullable(
                                                                        players.onlinePlayer(request.requester())
                                                                )
                                                                .map(CellPlayer::name)
                                                                .orElse("")
                                                ))
                                                .filter(value -> !value.isBlank())
                                                .distinct()
                                                .toList()
                                        )
                                        .orElseGet(List::of),
                                builder
                        ))
                        .executes(command -> selected(
                                context,
                                command,
                                descriptor
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpdeny",
                "/tpdeny [request-id|player]",
                root
        );
    }

    private int selected(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) throws CommandSyntaxException {
        var selector = TeleportRequestSelectors.parse(
                StringArgumentType.getString(command, "selector")
        );
        return TeleportCommandResults.player(
                context,
                command,
                descriptor,
                "tpdeny selector",
                players,
                player -> service.deny(
                        player,
                        Optional.of(selector)
                )
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
