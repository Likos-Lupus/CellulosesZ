package top.likoslupus.cellulosesz.common;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandTreeService;
import top.likoslupus.cellulosesz.api.player.PlayerConnectionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerNetworkService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOperations;
import top.likoslupus.cellulosesz.api.text.ClientLocaleService;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.common.bootstrap.CommonRuntime;
import top.likoslupus.cellulosesz.common.command.CommandManager;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.common.command.CommandTreeRefreshService;
import top.likoslupus.cellulosesz.common.command.MinecraftServerThreadExecutor;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.*;
import top.likoslupus.cellulosesz.common.playerstate.MinecraftPlayerStateService;
import top.likoslupus.cellulosesz.common.teleport.MinecraftTeleportOperations;
import top.likoslupus.cellulosesz.common.world.MinecraftWorldDirectory;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * Common Minecraft initialization shared by Fabric and future NeoForge loaders.
 */
public final class CellulosesZCommon {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private CellulosesZCommon() {
    }

    public static void initialize(CommonRuntime runtime) {
        requireNonNull(runtime, "runtime");
        if (!INITIALIZED.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "CellulosesZ common runtime has already been initialized"
            );
        }

        var bootstrap = runtime.bootstrap();
        var server = runtime.server();
        var commands = new CommandRegistry();
        var serverThread = new MinecraftServerThreadExecutor(server);
        var treeRefresh = new CommandTreeRefreshService(server);

        bootstrap.registerService(
                MinecraftServerHandle.class,
                server
        );
        bootstrap.registerService(
                CommandRegistry.class,
                commands
        );
        bootstrap.registerService(
                ServerThreadExecutor.class,
                serverThread
        );
        bootstrap.registerService(
                CommandTreeRefreshService.class,
                treeRefresh
        );
        bootstrap.registerService(
                PlayerDirectory.class,
                new MinecraftPlayerDirectory(server)
        );
        bootstrap.registerService(
                ClientLocaleService.class,
                new MinecraftClientLocaleService()
        );
        bootstrap.registerService(
                PlayerAudienceService.class,
                new MinecraftPlayerAudienceService(
                        bootstrap.serviceRegistry(),
                        bootstrap.logger()
                )
        );
        bootstrap.registerService(
                PlayerConnectionService.class,
                new MinecraftPlayerConnectionService(bootstrap.logger())
        );
        bootstrap.registerService(
                PlayerLocationPlatformService.class,
                new MinecraftPlayerLocationService()
        );
        bootstrap.registerService(
                PlayerNetworkService.class,
                new MinecraftPlayerNetworkService()
        );
        bootstrap.registerService(
                TeleportOperations.class,
                new MinecraftTeleportOperations(server)
        );
        bootstrap.registerService(
                PlayerStatePlatformService.class,
                new MinecraftPlayerStateService(server)
        );
        bootstrap.registerService(
                WorldDirectory.class,
                new MinecraftWorldDirectory(server)
        );
        bootstrap.initialize();
        runtime.hooks().initialize();

        var commandManager = new CommandManager(
                bootstrap,
                commands,
                runtime.commandRoots(),
                treeRefresh
        );
        bootstrap.registerService(CommandManager.class, commandManager);
        bootstrap.registerService(CommandTreeService.class, commandManager);

        CommandRegistrationEvent.EVENT.register(commandManager::register);
        LifecycleEvent.SERVER_BEFORE_START.register(current -> {
            server.attach(current);
            bootstrap.onServerStarting(current);
        });
        LifecycleEvent.SERVER_STARTED.register(bootstrap::onServerStarted);
        LifecycleEvent.SERVER_STOPPING.register(current -> {
            server.beginStopping(current);
            bootstrap
                    .onServerStopping(current)
                    .whenComplete((_, failure) -> {
                        try {
                            if (failure != null) {
                                bootstrap.logger().error(
                                        "CellulosesZ asynchronous shutdown failed.",
                                        failure
                                );
                            }
                            runtime.hooks().close();
                        } finally {
                            server.detach(current);
                        }
                    });
        });
        TickEvent.SERVER_POST.register(current -> {
            runtime.hooks().beforeServerTick(current);
            bootstrap.tick();
            runtime.hooks().afterServerTick(current);
        });
        PlayerEvent.PLAYER_JOIN.register(player -> {
            runtime.hooks().afterPlayerJoin(player);
            bootstrap.onPlayerJoin(MinecraftPlayers.wrap(player));
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            runtime.hooks().afterPlayerQuit(player);
            bootstrap.onPlayerDisconnect(MinecraftPlayers.wrap(player));
        });
    }

}
