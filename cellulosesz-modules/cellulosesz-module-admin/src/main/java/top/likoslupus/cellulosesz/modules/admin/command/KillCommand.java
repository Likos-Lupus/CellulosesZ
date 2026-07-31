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

import static java.util.Objects.requireNonNull;

public final class KillCommand implements CommandContributor {

    private final PlayerControlCommandService service;
    private final PlayerDirectory players;

    public KillCommand(
            PlayerControlCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "kill",
                "cellulosesz.command.kill",
                CommandSourceKind.ANY
        );

        var argument = Commands.argument(
                        "player",
                        PlayerNameArgument.playerName()
                )
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
                        "kill force=" + context.permissions().has(
                                command.getSource(),
                                "cellulosesz.command.kill.force"
                        ),
                        _ -> service.kill(
                                PlayerNameArgument.get(
                                        command,
                                        "player"
                                ),
                                context.permissions().has(
                                        command.getSource(),
                                        "cellulosesz.command.kill.force"
                                )
                        )
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.kill",
                "/kill <player>",
                Commands.literal("kill").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
