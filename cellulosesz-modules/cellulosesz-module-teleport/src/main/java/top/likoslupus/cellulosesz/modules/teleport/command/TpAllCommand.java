package top.likoslupus.cellulosesz.modules.teleport.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TpAllCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;

    public TpAllCommand(
            TeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tpall",
                "cellulosesz.teleport.tpall",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("tpall")
                .executes(command -> TeleportCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "tpall self",
                        policy -> service.all(
                                TeleportCommandResults.current(policy, players),
                                Optional.empty(),
                                context.permissions().has(
                                        command.getSource(),
                                        "cellulosesz.teleport.tpall.bypass"
                                )
                        )
                ))
                .then(Commands.argument("player", PlayerNameArgument.playerName())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        players::onlinePlayerNames, builder
                                )
                        )
                        .executes(command -> TeleportCommandResults.async(
                                context,
                                command,
                                descriptor,
                                "tpall target",
                                policy -> service.all(
                                        TeleportCommandResults.current(policy, players),
                                        Optional.of(PlayerNameArgument.get(command, "player")),
                                        context.permissions().has(
                                                command.getSource(),
                                                "cellulosesz.teleport.tpall.bypass"
                                        )
                                )
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tpall",
                "/tpall [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
