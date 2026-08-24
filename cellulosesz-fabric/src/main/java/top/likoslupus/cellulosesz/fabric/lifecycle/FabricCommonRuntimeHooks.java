package top.likoslupus.cellulosesz.fabric.lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.common.command.MinecraftPlayerCommandDispatchService;
import top.likoslupus.cellulosesz.common.entity.MinecraftEntityOperations;
import top.likoslupus.cellulosesz.common.item.ItemPlatformService;
import top.likoslupus.cellulosesz.common.lifecycle.CommonRuntimeHooks;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.common.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.common.teleport.TeleportOperations;
import top.likoslupus.cellulosesz.common.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.common.world.MinecraftBackupOperations;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;
import top.likoslupus.cellulosesz.fabric.hook.FabricGameplayHooks;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** Fabric-only hooks left after common Architectury lifecycle wiring. */
public final class FabricCommonRuntimeHooks implements CommonRuntimeHooks {

    private final CellulosesZBootstrap bootstrap;
    private final MinecraftPlayerCommandDispatchService commandDispatch;
    private final MinecraftEntityOperations entities;
    private final MinecraftBackupOperations backups;
    private final Supplier<PermissionBackend> permissions;
    private @Nullable FabricGameplayHooks gameplay;
    private @Nullable FabricPlatformEventBridge events;

    public FabricCommonRuntimeHooks(
            CellulosesZBootstrap bootstrap,
            MinecraftPlayerCommandDispatchService commandDispatch,
            MinecraftEntityOperations entities,
            MinecraftBackupOperations backups,
            Supplier<PermissionBackend> permissions
    ) {
        this.bootstrap = requireNonNull(bootstrap, "bootstrap");
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

        FabricVanishBridge.visibility((viewer, target) -> {
            var service = services.find(VanishService.class);
            return service == null || service.canSee(
                    MinecraftPlayers.wrap(viewer),
                    target.getUUID()
            );
        });
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
    }

}
