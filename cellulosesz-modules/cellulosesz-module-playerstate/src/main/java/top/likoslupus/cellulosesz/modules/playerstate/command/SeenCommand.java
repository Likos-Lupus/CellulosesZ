package top.likoslupus.cellulosesz.modules.playerstate.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class SeenCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;
    private final NameCacheService names;

    public SeenCommand(
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
                "seen",
                "cellulosesz.playerstate.seen",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("seen")
                .then(Commands.argument(
                                        "player",
                                        StringArgumentType.word()
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
                                                "seen",
                                                policy -> service.seen(
                                                        PlayerStateCommandSupport.currentPlayer(
                                                                policy,
                                                                players
                                                        ),
                                                        StringArgumentType.getString(command, "player")
                                                )
                                        )
                                )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.seen",
                "/seen <player>",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
