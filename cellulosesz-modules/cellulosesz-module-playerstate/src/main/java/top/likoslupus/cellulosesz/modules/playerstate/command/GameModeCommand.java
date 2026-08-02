package top.likoslupus.cellulosesz.modules.playerstate.command;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.GameModeKind;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandResult;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class GameModeCommand implements CommandContributor {

    private final PlayerAbilityCommandService service;
    private final PlayerDirectory players;

    public GameModeCommand(
            PlayerAbilityCommandService service,
            PlayerDirectory players
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = PlayerStateCommandSupport.descriptor(
                "gamemode",
                "cellulosesz.playerstate.gamemode",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("gamemode");

        Arrays.stream(GameModeKind.values())
                .map(mode -> Commands.literal(mode.name().toLowerCase(Locale.ROOT))
                        .executes(command -> PlayerStateCommandSupport.async(
                                context,
                                command,
                                descriptor,
                                "gamemode self",
                                policy -> PlayerStateCommandSupport.currentPlayer(
                                                policy,
                                                players
                                        )
                                        .map(player ->
                                                service.gameMode(
                                                        player,
                                                        mode
                                                )
                                        )
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
                                                EntityArgument.player()
                                        )
                                        .requires(source -> context.permissions().has(
                                                source,
                                                "cellulosesz.playerstate.gamemode.others"
                                        ))
                                        .executes(command ->
                                                PlayerStateCommandSupport.async(
                                                        context,
                                                        command,
                                                        descriptor,
                                                        "gamemode other",
                                                        _ -> service.gameMode(
                                                                MinecraftPlayers.wrap(
                                                                        EntityArgument.getPlayer(
                                                                                command,
                                                                                "player"
                                                                        )
                                                                ),
                                                                mode
                                                        )
                                                )
                                        )
                        )
                )
                .forEach(root::then);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.gamemode",
                "/gamemode <survival|creative|adventure|spectator> [player]",
                root
        );
    }

    @Override
    public String moduleId() {
        return PlayerStateCommandSupport.MODULE;
    }

}
