package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import top.likoslupus.cellulosesz.api.command.service.PlayerChatDispatchService;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.item.WorkstationPlatformService;
import top.likoslupus.cellulosesz.api.platform.admin.BanPlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNamePlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.api.sign.SignPlatformService;
import top.likoslupus.cellulosesz.api.world.*;
import top.likoslupus.cellulosesz.common.CellulosesZCommon;
import top.likoslupus.cellulosesz.common.bootstrap.CommonRuntime;
import top.likoslupus.cellulosesz.common.command.MinecraftPlayerChatDispatchService;
import top.likoslupus.cellulosesz.common.command.MinecraftPlayerCommandDispatchService;
import top.likoslupus.cellulosesz.common.entity.MinecraftEntityOperations;
import top.likoslupus.cellulosesz.common.item.MinecraftInventoryOperations;
import top.likoslupus.cellulosesz.common.item.MinecraftItemOperations;
import top.likoslupus.cellulosesz.common.item.MinecraftWorkstationOperations;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.permission.LuckPermsPermissionBackend;
import top.likoslupus.cellulosesz.common.permission.MinecraftOpPermissionBackend;
import top.likoslupus.cellulosesz.common.world.*;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.permission.CompositePermissionBackend;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.fabric.bridge.FabricCommandRootMutator;
import top.likoslupus.cellulosesz.fabric.lifecycle.FabricCommonRuntimeHooks;
import top.likoslupus.cellulosesz.modules.permission.config.PermissionConfig;

import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Thin Fabric composition root; feature commands remain in common modules.
 */
public final class CellulosesZFabric implements DedicatedServerModInitializer {

    private @Nullable CellulosesZBootstrap bootstrap;

    @Override
    public void onInitializeServer() {
        var loader = FabricLoader.getInstance();
        var configDirectory = loader.getConfigDir().resolve("cellulosesz");
        var version = loader.getModContainer("cellulosesz")
                .map(container -> container
                        .getMetadata()
                        .getVersion()
                        .getFriendlyString()
                )
                .orElse("unknown");

        var logger = new Slf4jCellulosesZLogger(LoggerFactory.getLogger("CellulosesZ"));
        var currentBootstrap = new CellulosesZBootstrap(
                configDirectory,
                version,
                logger
        );
        bootstrap = currentBootstrap;

        var server = new MinecraftServerHandle();
        var bans = new MinecraftBanPlatformService(server);
        var dispatch = new MinecraftPlayerCommandDispatchService(server);
        var inventory = new MinecraftInventoryOperations(server);
        var items = new MinecraftItemOperations(server, logger);
        var workstations = new MinecraftWorkstationOperations(server);
        var backups = new MinecraftBackupOperations(server);
        var signs = new FabricSignOperations(server);
        var entities = new MinecraftEntityOperations(server);
        var removals = new MinecraftEntityRemovalOperations(server);

        currentBootstrap.registerService(
                BanPlatformService.class,
                bans
        );
        currentBootstrap.registerService(
                PlayerCommandDispatchService.class,
                dispatch
        );
        currentBootstrap.registerService(
                PlayerChatDispatchService.class,
                new MinecraftPlayerChatDispatchService(server)
        );
        currentBootstrap.registerService(
                InventoryPlatformService.class,
                inventory
        );
        currentBootstrap.registerService(
                ItemPlatformService.class,
                items
        );
        currentBootstrap.registerService(
                WorkstationPlatformService.class,
                workstations
        );
        currentBootstrap.registerService(
                BackupPlatformService.class,
                backups
        );
        currentBootstrap.registerService(
                SignPlatformService.class,
                signs
        );
        currentBootstrap.registerService(
                DisplayNamePlatformService.class,
                new FabricDisplayNameOperations(server, logger)
        );
        currentBootstrap.registerService(
                VanishPlatformService.class,
                new FabricVanishPlatformService()
        );
        currentBootstrap.registerService(
                WorldStatePlatformService.class,
                new MinecraftWorldStateOperations(server)
        );
        currentBootstrap.registerService(
                PlayerTargetingService.class,
                new MinecraftPlayerTargetingOperations(server)
        );
        currentBootstrap.registerService(
                EntityRemovalPlatformService.class,
                removals
        );
        currentBootstrap.registerService(
                MinecraftEntityRemovalOperations.class,
                removals
        );
        currentBootstrap.registerService(
                WorldPlatformService.class,
                new FabricWorldOperations(server, signs)
        );
        currentBootstrap.registerService(
                EntityPlatformService.class,
                entities
        );
        currentBootstrap.registerService(
                RecipePlatformService.class,
                new FabricRecipeOperations(server, items)
        );
        var hooks = new FabricCommonRuntimeHooks(
                currentBootstrap,
                dispatch,
                entities,
                backups,
                this::permissionBackend
        );
        CellulosesZCommon.initialize(new CommonRuntime(
                currentBootstrap,
                server,
                hooks,
                new FabricCommandRootMutator()
        ));
    }

    private PermissionBackend permissionBackend() {
        var current = requireNonNull(
                bootstrap,
                "CellulosesZBootstrap has not been initialized"
        );
        var permissionConfig = current.configRegistry()
                .optional("module.permission", PermissionConfig.class)
                .orElseGet(PermissionConfig::new);
        var backends = new ArrayList<PermissionBackend>();

        if (permissionConfig.provider.preferLuckPerms
                && FabricLoader.getInstance().isModLoaded("luckperms")
        ) {
            try {
                backends.add(LuckPermsPermissionBackend.fromProvider());
            } catch (IllegalStateException unavailable) {
                current.logger().warn(
                        "LuckPerms is installed but its API provider is not available; using fallback permissions"
                );
            }
        }

        if (permissionConfig.provider.opFallback) {
            backends.add(new MinecraftOpPermissionBackend(permissionConfig.provider.opLevel));
        }

        if (backends.isEmpty()) {
            backends.add(new MinecraftOpPermissionBackend(
                    current.coreConfig().permissions.opFallbackLevel
            ));
        }

        return new CompositePermissionBackend(backends);
    }

}
