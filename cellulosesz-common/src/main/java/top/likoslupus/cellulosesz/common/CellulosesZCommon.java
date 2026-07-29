package top.likoslupus.cellulosesz.common;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandTreeService;
import top.likoslupus.cellulosesz.common.bootstrap.CommonRuntime;
import top.likoslupus.cellulosesz.common.command.CommandManager;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.common.command.CommandTreeRefreshService;
import top.likoslupus.cellulosesz.common.command.MinecraftServerThreadExecutor;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * Common Minecraft initialization shared by Fabric and future NeoForge loaders.
 */
public final class CellulosesZCommon {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private CellulosesZCommon() {
    }

    @SuppressWarnings("resource")
    public static void initialize(CommonRuntime runtime) {
        requireNonNull(runtime, "runtime");
        if (!INITIALIZED.compareAndSet(false, true)) {
            throw new IllegalStateException("CellulosesZ common runtime has already been initialized");
        }

        var bootstrap = runtime.bootstrap();
        var server = new MinecraftServerHandle();
        var commands = new CommandRegistry();
        var serverThread = new MinecraftServerThreadExecutor(server);
        var treeRefresh = new CommandTreeRefreshService(server);

        bootstrap.registerService(MinecraftServerHandle.class, server);
        bootstrap.registerService(CommandRegistry.class, commands);
        bootstrap.registerService(ServerThreadExecutor.class, serverThread);
        bootstrap.registerService(CommandTreeRefreshService.class, treeRefresh);
        bootstrap.initialize();
        runtime.hooks().initialize();

        var commandManager = new CommandManager(
                bootstrap,
                runtime.platform(),
                commands,
                runtime.commandRoots(),
                treeRefresh
        );
        bootstrap.registerService(CommandManager.class, commandManager);
        bootstrap.registerService(CommandTreeService.class, commandManager);

        CommandRegistrationEvent.EVENT.register(commandManager::register);
        LifecycleEvent.SERVER_BEFORE_START.register(current -> {
            server.attach(current);
            runtime.hooks().attachServer(current);
            bootstrap.onServerStarting(current);
        });
        LifecycleEvent.SERVER_STARTED.register(bootstrap::onServerStarted);
        LifecycleEvent.SERVER_STOPPING.register(current -> {
            server.beginStopping(current);
            try {
                bootstrap.onServerStopping(current);
            } finally {
                try {
                    runtime.hooks().close();
                } finally {
                    runtime.hooks().detachServer();
                    server.detach(current);
                }
            }
        });
        TickEvent.SERVER_POST.register(current -> {
            runtime.hooks().beforeServerTick(current);
            bootstrap.tick();
            runtime.hooks().afterServerTick(current);
        });
        PlayerEvent.PLAYER_JOIN.register(player -> {
            runtime.hooks().afterPlayerJoin(player);
            bootstrap.onPlayerJoin(player);
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            runtime.hooks().afterPlayerQuit(player);
            bootstrap.onPlayerDisconnect(player);
        });
    }

}
