package top.likoslupus.cellulosesz.modules.teleport.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldResolution;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandResult;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class WorldCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;
    private final WorldDirectory worlds;

    public WorldCommand(
            TeleportCommandService service,
            PlayerDirectory players,
            WorldDirectory worlds
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.worlds = requireNonNull(worlds, "worlds");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "world",
                "cellulosesz.teleport.world",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("world")
                .then(Commands.argument("world", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        worlds::loadedWorldIds, builder
                                )
                        )
                        .executes(command -> TeleportCommandResults.player(
                                context,
                                command,
                                descriptor,
                                "world",
                                players,
                                player -> {
                                    var input = StringArgumentType.getString(command, "world");
                                    var resolution = worlds.resolve(input);
                                    var worldId = resolution instanceof WorldResolution.Resolved resolved
                                            ? resolved.worldId()
                                            : null;

                                    if (worldId != null) {
                                        var permission = "cellulosesz.teleport.world."
                                                + worldId
                                                .replace(':', '.');

                                        if (!context.hasPermission(
                                                command.getSource(), permission
                                        )) {
                                            return CompletableFuture.completedFuture(
                                                    TeleportCommandResult.failure(
                                                            TeleportCommandStatus.BLOCKED,
                                                            "commands.common.no-permission"
                                                    )
                                            );
                                        }
                                    }

                                    return service.world(player, input);
                                }
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.world",
                "/world <world>",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
