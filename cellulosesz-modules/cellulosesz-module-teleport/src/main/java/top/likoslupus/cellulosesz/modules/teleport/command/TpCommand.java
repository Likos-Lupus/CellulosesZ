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

public final class TpCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;

    public TpCommand(
            TeleportCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        register(
                context,
                "tp",
                "cellulosesz.teleport.tp",
                false
        );
    }

    void register(
            CommandRegistrationContext context,
            String name,
            String permission,
            boolean override
    ) {
        var descriptor = TeleportCommandResults.descriptor(
                name,
                permission,
                CommandSourceKind.ANY
        );

        var first = Commands.argument("first", PlayerNameArgument.playerName())
                .suggests((_, builder) ->
                        CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames, builder
                        )
                )
                .executes(command -> TeleportCommandResults.async(
                        context,
                        command,
                        descriptor,
                        name + " self",
                        policy -> service.tp(
                                TeleportCommandResults.current(policy, players),
                                PlayerNameArgument.get(command, "first"),
                                Optional.empty(),
                                override,
                                context.permissions().has(
                                        command.getSource(), permission + ".bypass"
                                )
                        )
                ))
                .then(Commands.argument("second", PlayerNameArgument.playerName())
                        .requires(source ->
                                context.permissions().has(source, permission + ".others")
                        )
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        players::onlinePlayerNames, builder
                                )
                        )
                        .executes(command -> TeleportCommandResults.async(
                                context,
                                command,
                                descriptor,
                                name + " others",
                                policy -> service.tp(
                                        TeleportCommandResults.current(policy, players),
                                        PlayerNameArgument.get(command, "first"),
                                        Optional.of(PlayerNameArgument.get(command, "second")),
                                        override,
                                        true
                                )
                        ))
                );

        var root = Commands.literal(name).then(first);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description." + name,
                "/" + name + " <target> | <player> <target>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
