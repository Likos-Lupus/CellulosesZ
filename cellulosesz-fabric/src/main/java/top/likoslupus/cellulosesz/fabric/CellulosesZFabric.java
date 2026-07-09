package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import org.slf4j.LoggerFactory;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;

public final class CellulosesZFabric implements DedicatedServerModInitializer {

    private CellulosesZBootstrap bootstrap;

    @Override
    public void onInitializeServer() {
        var configDirectory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cellulosesz");
        var version = FabricLoader.getInstance()
                .getModContainer("cellulosesz")
                .map(container ->
                        container.getMetadata()
                                .getVersion()
                                .getFriendlyString()
                )
                .orElse("unknown");

        bootstrap = new CellulosesZBootstrap(
                configDirectory,
                version,
                new Slf4jCellulosesZLogger(LoggerFactory.getLogger("CellulosesZ"))
        );
        bootstrap.initialize();
        bootstrap.permissionBackend(this::hasPermissionFallback);

        CommandRegistrationCallback.EVENT.register((
                dispatcher,
                registryAccess,
                environment
        ) -> new FabricCommandBinder(bootstrap).bind(dispatcher, registryAccess, environment));

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                bootstrap.onServerStarting(server)
        );
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                bootstrap.onServerStarted(server)
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                bootstrap.onServerStopping(server)
        );
        ServerTickEvents.END_SERVER_TICK.register(_ ->
                bootstrap.tick()
        );

        ServerPlayConnectionEvents.JOIN.register((
                handler,
                _,
                _
        ) -> bootstrap.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((
                handler,
                _
        ) -> bootstrap.onPlayerDisconnect(handler.getPlayer()));
    }

    private boolean hasPermissionFallback(Object source, String permission) {
        if (permission.isBlank()) return true;

        if (source instanceof CommandSourceStack commandSource) {
            var config = bootstrap.coreConfig();
            int opLevel = config.permissions.opFallbackLevel;
            return commandSource.permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.byId(opLevel))
            );
        }
        return false;
    }

}
