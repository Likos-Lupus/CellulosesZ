package top.likoslupus.cellulosesz.fabric.lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.common.lifecycle.CommonRuntimeHooks;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.fabric.FabricBanPlatformService;
import top.likoslupus.cellulosesz.fabric.FabricEntityOperations;
import top.likoslupus.cellulosesz.fabric.FabricPlatformService;
import top.likoslupus.cellulosesz.fabric.FabricPlayerCommandDispatchService;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;
import top.likoslupus.cellulosesz.fabric.hook.FabricGameplayHooks;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Fabric-only hooks left after common Architectury lifecycle wiring.
 */
public final class FabricCommonRuntimeHooks implements CommonRuntimeHooks {

    private final CellulosesZBootstrap bootstrap;
    private final FabricPlatformService platform;
    private final FabricBanPlatformService bans;
    private final FabricPlayerCommandDispatchService commandDispatch;
    private final FabricEntityOperations entities;
    private final Supplier<PermissionBackend> permissions;
    private @Nullable FabricGameplayHooks gameplay;
    private @Nullable FabricPlatformEventBridge events;

    public FabricCommonRuntimeHooks(
            CellulosesZBootstrap bootstrap,
            FabricPlatformService platform,
            FabricBanPlatformService bans,
            FabricPlayerCommandDispatchService commandDispatch,
            FabricEntityOperations entities,
            Supplier<PermissionBackend> permissions
    ) {
        this.bootstrap = requireNonNull(bootstrap, "bootstrap");
        this.platform = requireNonNull(platform, "platform");
        this.bans = requireNonNull(bans, "bans");
        this.commandDispatch = requireNonNull(commandDispatch, "commandDispatch");
        this.entities = requireNonNull(entities, "entities");
        this.permissions = requireNonNull(permissions, "permissions");
    }

    @Override
    public void initialize() {
        platform.messages(
                bootstrap.serviceRegistry().require(MessageRenderer.class),
                bootstrap.serviceRegistry().require(LocaleResolver.class)
        );
        bootstrap.permissionBackend(permissions.get());
        gameplay = new FabricGameplayHooks(
                bootstrap.serviceRegistry(),
                platform,
                bootstrap.serviceRegistry().require(MessageRenderer.class),
                bootstrap.serviceRegistry().require(LocaleResolver.class)
        );
        gameplay.register();
        events = new FabricPlatformEventBridge(
                bootstrap.eventRegistry(),
                platform,
                commandDispatch
        );
        events.register();
        FabricVanishBridge.visibility((viewer, target) -> bootstrap.serviceRegistry()
                .optional(VanishService.class)
                .flatMap(service ->
                        platform.player(viewer).map(wrapped ->
                                service.canSee(wrapped, target.getUUID())
                        )
                )
                .orElse(true));
    }

    @Override
    public void attachServer(MinecraftServer server) {
        platform.server(server);
        bans.server(server);
    }

    @Override
    public void detachServer() {
        bans.clearServer();
        platform.clearServer();
    }

    @Override
    public void beforeServerTick(MinecraftServer server) {
        commandDispatch.beginTick();
        entities.tick();
    }

    @Override
    public void afterServerTick(MinecraftServer server) {
        var currentGameplay = gameplay;
        if (currentGameplay != null) currentGameplay.tick(server);
        var currentEvents = events;
        if (currentEvents != null) currentEvents.tick(server);
    }

    @Override
    public void afterPlayerJoin(ServerPlayer player) {
        var current = events;
        if (current != null) current.playerJoined(player);
    }

    @Override
    public void afterPlayerQuit(ServerPlayer player) {
        var current = events;
        if (current != null) current.playerDisconnected(player);
        FabricDisplayNameBridge.clear(player.getUUID());
        FabricVanishBridge.vanished(player.getUUID(), false);
    }

    @Override
    public void close() {
        var current = events;
        if (current != null) current.close();
        entities.clearTrackedEntities();
        FabricVanishBridge.clear();
        bans.clearServer();
        platform.close();
    }

}
