package top.likoslupus.cellulosesz.modules.admin.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.admin.application.BanCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class UnbanCommand implements CommandContributor {

    private final BanCommandService service;
    private final PlayerDirectory players;

    public UnbanCommand(
            BanCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = AdminCommandResults.descriptor(
                "unban",
                "cellulosesz.admin.unban",
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
                        "unban",
                        policy -> service.unban(
                                PlayerNameArgument.get(
                                        command,
                                        "player"
                                ),
                                AdminCommandResults.actor(
                                        policy,
                                        players
                                )
                        )
                ));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.unban",
                "/unban <player>",
                Commands.literal("unban").then(argument)
        );
    }

    @Override
    public String moduleId() {
        return AdminCommandResults.MODULE;
    }

}
