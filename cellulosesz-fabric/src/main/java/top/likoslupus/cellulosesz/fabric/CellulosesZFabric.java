package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.permission.CompositePermissionBackend;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.core.permission.ReflectionLuckPermsPermissionBackend;
import top.likoslupus.cellulosesz.modules.permission.config.PermissionConfig;

import java.util.ArrayList;

public final class CellulosesZFabric implements DedicatedServerModInitializer {

    private CellulosesZBootstrap bootstrap;
    private FabricPlatformService platform;

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
        platform = new FabricPlatformService();

        bootstrap.registerService(PlatformService.class, platform);
        bootstrap.registerService(FabricPlatformService.class, platform);
        bootstrap.initialize();
        bootstrap.permissionBackend(permissionBackend());

        CommandRegistrationCallback.EVENT.register((
                dispatcher,
                registryAccess,
                environment
        ) -> new FabricCommandBinder(bootstrap).bind(dispatcher, registryAccess, environment));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            platform.server(server);
            bootstrap.onServerStarting(server);
        });
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

    private PermissionBackend permissionBackend() {
        var permissionConfig = bootstrap.configRegistry()
                .optional("module.permission", PermissionConfig.class)
                .orElseGet(PermissionConfig::new);
        var backends = new ArrayList<PermissionBackend>();

        if (permissionConfig.provider.preferLuckPerms && FabricLoader.getInstance().isModLoaded("luckperms")) {
            backends.add(new ReflectionLuckPermsPermissionBackend());
        }
        if (permissionConfig.provider.opFallback) {
            backends.add(new FabricOpPermissionBackend(permissionConfig.provider.opLevel));
        }
        if (backends.isEmpty()) {
            backends.add(new FabricOpPermissionBackend(bootstrap.coreConfig().permissions.opFallbackLevel));
        }

        return new CompositePermissionBackend(backends);
    }

}
