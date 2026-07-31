package top.likoslupus.cellulosesz.modules.admin.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.admin.application.PlayerControlCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class IceCommand implements CommandContributor {

    private final PlayerControlCommandService service;
    private final PlayerDirectory players;

    public IceCommand(
            PlayerControlCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "ice",
                "cellulosesz.command.ice",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("ice")
                .executes(command -> AdminCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "ice self",
                        policy -> service.ice(
                                AdminCommandResults.current(policy, players),
                                Optional.empty()
                        )
                ))
                .then(Commands.argument(
                                        "player",
                                        PlayerNameArgument.playerName()
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.command.ice.others"
                                ))
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                players::onlinePlayerNames,
                                                builder
                                        )
                                )
                                .executes(command -> AdminCommandResults.async(
                                        context,
                                        command,
                                        descriptor,
                                        "ice other",
                                        policy -> service.ice(
                                                AdminCommandResults.current(
                                                        policy,
                                                        players
                                                ),
                                                Optional.of(
                                                        PlayerNameArgument.get(
                                                                command,
                                                                "player"
                                                        )
                                                )
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.ice",
                "/ice [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
