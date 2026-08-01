package top.likoslupus.cellulosesz.fabric.lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOperations;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.common.lifecycle.CommonRuntimeHooks;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.fabric.*;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;
import top.likoslupus.cellulosesz.fabric.hook.FabricGameplayHooks;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Fabric-only hooks left after common Architectury lifecycle wiring.
 */
public final class FabricCommonRuntimeHooks implements CommonRuntimeHooks {

    private final CellulosesZBootstrap bootstrap;
    private final FabricServerAccess access;
    private final FabricBanPlatformService bans;
    private final FabricPlayerCommandDispatchService commandDispatch;
    private final FabricEntityOperations entities;
    private final FabricBackupOperations backups;
    private final Supplier<PermissionBackend> permissions;
    private @Nullable FabricGameplayHooks gameplay;
    private @Nullable FabricPlatformEventBridge events;

    public FabricCommonRuntimeHooks(
            CellulosesZBootstrap bootstrap,
            FabricServerAccess access,
            FabricBanPlatformService bans,
            FabricPlayerCommandDispatchService commandDispatch,
            FabricEntityOperations entities,
            FabricBackupOperations backups,
            Supplier<PermissionBackend> permissions
    ) {
        this.bootstrap = requireNonNull(bootstrap, "bootstrap");
        this.access = requireNonNull(access, "access");
        this.bans = requireNonNull(bans, "bans");
        this.commandDispatch = requireNonNull(commandDispatch, "commandDispatch");
        this.entities = requireNonNull(entities, "entities");
        this.backups = requireNonNull(backups, "backups");
        this.permissions = requireNonNull(permissions, "permissions");
    }

    @Override
    public void initialize() {
        bootstrap.permissionBackend(permissions.get());

        var services = bootstrap.serviceRegistry();
        gameplay = new FabricGameplayHooks(
                services,
                services.require(ItemPlatformService.class),
                services.require(ServerThreadExecutor.class),
                services.require(PlayerAudienceService.class),
                services.require(MessageRenderer.class),
                services.require(LocaleResolver.class)
        );
        gameplay.register();

        events = new FabricPlatformEventBridge(
                bootstrap.eventRegistry(),
                services.require(PlayerLocationPlatformService.class),
                services.require(TeleportOperations.class),
                services.require(PlayerStatePlatformService.class),
                commandDispatch
        );
        events.register();

        FabricVanishBridge.visibility((viewer, target) -> services
                .optional(VanishService.class)
                .map(service -> service.canSee(
                        MinecraftPlayers.wrap(viewer),
                        target.getUUID()
                ))
                .orElse(true)
        );
    }

    @Override
    public void attachServer(MinecraftServer server) {
        access.attach(server);
        bans.server(server);
    }

    @Override
    public void detachServer() {
        bans.clearServer();
        access.clear();
    }

    @Override
    public void beforeServerTick(MinecraftServer server) {
        commandDispatch.beginTick();
        entities.tick();
    }

    @Override
    public void afterServerTick(MinecraftServer server) {
        var currentGameplay = gameplay;
        if (currentGameplay != null) {
            currentGameplay.tick(server);
        }

        var currentEvents = events;
        if (currentEvents != null) {
            currentEvents.tick(server);
        }
    }

    @Override
    public void afterPlayerJoin(ServerPlayer player) {
        var current = events;
        if (current != null) {
            current.playerJoined(player);
        }
    }

    @Override
    public void afterPlayerQuit(ServerPlayer player) {
        var current = events;
        if (current != null) {
            current.playerDisconnected(player);
        }

        FabricDisplayNameBridge.clear(player.getUUID());
        FabricVanishBridge.vanished(player.getUUID(), false);
    }

    @Override
    public void close() {
        var current = events;
        if (current != null) {
            current.close();
        }

        entities.clearTrackedEntities();
        backups.close();
        FabricVanishBridge.clear();
        bans.clearServer();
        access.clear();
    }

}
