package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import top.likoslupus.cellulosesz.api.command.service.CommandTreeService;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.platform.PlatformCapability;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.platform.admin.BanPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.permission.CompositePermissionBackend;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.core.permission.ReflectionLuckPermsPermissionBackend;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;
import top.likoslupus.cellulosesz.fabric.hook.FabricGameplayHooks;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;
import top.likoslupus.cellulosesz.modules.permission.config.PermissionConfig;

import java.util.ArrayList;
import java.util.EnumSet;

import static java.util.Objects.requireNonNull;

public final class CellulosesZFabric implements DedicatedServerModInitializer {

    private @Nullable CellulosesZBootstrap bootstrap;
    private @Nullable FabricPlatformService platform;
    private @Nullable FabricVanillaCommandBridge vanillaCommands;
    private @Nullable FabricGameplayHooks gameplayHooks;
    private @Nullable FabricPlatformEventBridge platformEvents;
    private @Nullable FabricPlayerCommandDispatchService commandDispatch;
    private @Nullable FabricBanPlatformService banPlatform;
    private @Nullable FabricEntityOperations entityOperations;

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
        vanillaCommands = new FabricVanillaCommandBridge();
        platform = new FabricPlatformService();
        validatePlatformCapabilities(platform);

        bootstrap.registerService(PlatformService.class, platform);
        bootstrap.registerService(FabricPlatformService.class, platform);
        banPlatform = new FabricBanPlatformService();
        bootstrap.registerService(BanPlatformService.class, banPlatform);
        commandDispatch = new FabricPlayerCommandDispatchService(platform);
        bootstrap.registerService(PlayerCommandDispatchService.class, commandDispatch);
        bootstrap.registerService(PlayerStatePlatformService.class, new FabricPlayerStateOperations(platform));
        bootstrap.registerService(InventoryPlatformService.class, new FabricInventoryOperations(platform));
        bootstrap.registerService(WorldPlatformService.class, new FabricWorldOperations(platform));
        entityOperations = new FabricEntityOperations(platform);
        bootstrap.registerService(EntityPlatformService.class, entityOperations);
        bootstrap.registerService(RecipePlatformService.class, new FabricRecipeOperations(platform));
        bootstrap.initialize();
        platform.messages(
                bootstrap.serviceRegistry().require(MessageRenderer.class),
                bootstrap.serviceRegistry().require(LocaleResolver.class)
        );
        bootstrap.permissionBackend(permissionBackend());

        gameplayHooks = new FabricGameplayHooks(
                bootstrap.serviceRegistry(),
                platform,
                bootstrap.serviceRegistry().require(MessageRenderer.class),
                bootstrap.serviceRegistry().require(LocaleResolver.class)
        );
        gameplayHooks.register();
        platformEvents = new FabricPlatformEventBridge(bootstrap.eventRegistry(), platform, commandDispatch);
        platformEvents.register();
        FabricVanishBridge.visibility((viewer, target) ->
                bootstrap.serviceRegistry()
                        .optional(VanishService.class)
                        .flatMap(service ->
                                platform.player(viewer)
                                        .map(wrapped -> service.canSee(wrapped, target.getUUID()))
                        )
                        .orElse(true)
        );

        var binder = new FabricCommandBinder(bootstrap, vanillaCommands);
        bootstrap.registerService(CommandTreeService.class, binder);
        CommandRegistrationCallback.EVENT.register(binder::bind);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            platform.server(server);
            banPlatform.server(server);
            bootstrap.onServerStarting(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                bootstrap.onServerStarted(server)
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                bootstrap.onServerStopping(server);
            } finally {
                var events = platformEvents;
                if (events != null) events.close();
                var tracked = entityOperations;
                if (tracked != null) tracked.clearTrackedEntities();
                var bans = banPlatform;
                if (bans != null) bans.clearServer();
                platform.close();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            commandDispatch.beginTick();
            var tracked = entityOperations;
            if (tracked != null) tracked.tick();
            bootstrap.tick();
            gameplayHooks.tick(server);
            platformEvents.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> {
            platformEvents.playerJoined(handler.getPlayer());
            bootstrap.onPlayerJoin(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> {
            platformEvents.playerDisconnected(handler.getPlayer());
            FabricDisplayNameBridge.clear(handler.getPlayer().getUUID());
            bootstrap.onPlayerDisconnect(handler.getPlayer());
        });
    }


    private static void validatePlatformCapabilities(PlatformService platform) {
        var required = EnumSet.allOf(PlatformCapability.class);
        required.removeAll(platform.capabilities());
        if (!required.isEmpty()) {
            throw new IllegalStateException("Fabric platform is missing required capabilities: " + required);
        }
    }

    private PermissionBackend permissionBackend() {
        requireNonNull(bootstrap, "CellulosesZBootstrap has not been initialized");
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
