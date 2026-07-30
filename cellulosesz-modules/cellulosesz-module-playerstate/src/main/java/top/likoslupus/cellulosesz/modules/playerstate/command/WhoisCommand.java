package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class WhoisCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;
    private final NameCacheService names;

    public WhoisCommand(
            PlayerInformationCommandService service,
            PlayerDirectory players,
            NameCacheService names
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.names = requireNonNull(names, "names");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "whois",
                "cellulosesz.playerstate.whois",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("whois")
                .then(Commands.argument(
                                        "player",
                                        PlayerNameArgument.playerName()
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                () -> names.entries().values(),
                                                builder
                                        )
                                )
                                .executes(command ->
                                        PlayerStateCommandSupport.async(
                                                context,
                                                command,
                                                descriptor,
                                                "whois",
                                                policy -> service.whois(
                                                        PlayerStateCommandSupport.currentPlayer(
                                                                policy,
                                                                players
                                                        ),
                                                        PlayerNameArgument.get(
                                                                command,
                                                                "player"
                                                        ),
                                                        policy.hasPermission(
                                                                "cellulosesz.playerstate.whois.uuid"
                                                        )
                                                )
                                        )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.whois",
                "/whois <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
