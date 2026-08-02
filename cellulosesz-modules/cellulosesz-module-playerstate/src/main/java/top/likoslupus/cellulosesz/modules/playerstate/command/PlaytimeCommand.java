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
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class PlaytimeCommand implements CommandContributor {

    private final PlayerInformationCommandService service;
    private final PlayerDirectory players;
    private final NameCacheService names;

    public PlaytimeCommand(
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
                "playtime",
                "cellulosesz.playerstate.playtime",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("playtime")
                .executes(command -> PlayerStateCommandSupport.async(
                        context,
                        command,
                        descriptor,
                        "playtime self",
                        policy -> PlayerStateCommandSupport.currentPlayer(
                                        policy,
                                        players
                                )
                                .map(player -> service.playtime(
                                        Optional.of(player),
                                        player.name()
                                ))
                                .orElseGet(() ->
                                        CompletableFuture.completedFuture(
                                                PlayerStateCommandResult.failure(
                                                        "common.player-only"
                                                )
                                        )
                                )
                ))
                .then(Commands.argument(
                                        "player",
                                        StringArgumentType.word()
                                )
                                .requires(source -> context.hasPermission(
                                        source,
                                        "cellulosesz.playerstate.playtime.others"
                                ))
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                () -> names.entries().values(),
                                                builder
                                        )
                                )
                                .executes(command -> PlayerStateCommandSupport.async(
                                        context,
                                        command,
                                        descriptor,
                                        "playtime",
                                        policy -> service.playtime(
                                                PlayerStateCommandSupport.currentPlayer(
                                                        policy,
                                                        players
                                                ),
                                                StringArgumentType.getString(command, "player")
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.playtime",
                "/playtime [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
