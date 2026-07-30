package top.likoslupus.cellulosesz.modules.messaging.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.messaging.application.PrivateMessageCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class IgnoreCommand implements CommandContributor {

    private final PrivateMessageCommandService service;
    private final PlayerDirectory players;

    public IgnoreCommand(
            PrivateMessageCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = MessagingCommandSupport.descriptor(
                "ignore",
                "cellulosesz.messaging.ignore",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("ignore")
                .then(Commands.argument(
                                        "player",
                                        PlayerNameArgument.playerName()
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                service::knownNames,
                                                builder
                                        )
                                )
                                .executes(command ->
                                        MessagingCommandSupport.requirePlayer(
                                                context,
                                                command,
                                                descriptor,
                                                "ignore",
                                                players,
                                                actor -> service.ignore(
                                                        actor,
                                                        PlayerNameArgument.get(
                                                                command,
                                                                "player"
                                                        )
                                                )
                                        )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.ignore",
                "/ignore <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return MessagingCommandSupport.MODULE;
    }

}
