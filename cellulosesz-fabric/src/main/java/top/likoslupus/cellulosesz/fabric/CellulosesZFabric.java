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
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.core.permission.CompositePermissionBackend;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.core.permission.ReflectionLuckPermsPermissionBackend;
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

        var access = new FabricServerAccess();
        var bans = new FabricBanPlatformService();
        var dispatch = new FabricPlayerCommandDispatchService(access);
        var inventory = new FabricInventoryOperations(access);
        var items = new FabricItemOperations(access, logger);
        var workstations = new FabricWorkstationOperations(access);
        var backups = new FabricBackupOperations(access);
        var signs = new FabricSignOperations(access);
        var entities = new FabricEntityOperations(access);

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
                new FabricPlayerChatDispatchService(access)
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
                new FabricDisplayNameOperations(access, logger)
        );
        currentBootstrap.registerService(
                VanishPlatformService.class,
                new FabricVanishPlatformService()
        );
        currentBootstrap.registerService(
                WorldStatePlatformService.class,
                new FabricWorldStateOperations(access)
        );
        currentBootstrap.registerService(
                PlayerTargetingService.class,
                new FabricPlayerTargetingOperations(access)
        );
        currentBootstrap.registerService(
                EntityRemovalPlatformService.class,
                new FabricEntityRemovalOperations(access)
        );
        currentBootstrap.registerService(
                WorldPlatformService.class,
                new FabricWorldOperations(access, signs)
        );
        currentBootstrap.registerService(
                EntityPlatformService.class,
                entities
        );
        currentBootstrap.registerService(
                RecipePlatformService.class,
                new FabricRecipeOperations(access, items)
        );

        var hooks = new FabricCommonRuntimeHooks(
                currentBootstrap,
                access,
                bans,
                dispatch,
                entities,
                backups,
                this::permissionBackend
        );
        CellulosesZCommon.initialize(new CommonRuntime(
                currentBootstrap,
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
            backends.add(new ReflectionLuckPermsPermissionBackend());
        }

        if (permissionConfig.provider.opFallback) {
            backends.add(new FabricOpPermissionBackend(permissionConfig.provider.opLevel));
        }

        if (backends.isEmpty()) {
            backends.add(new FabricOpPermissionBackend(
                    current.coreConfig().permissions.opFallbackLevel
            ));
        }

        return new CompositePermissionBackend(backends);
    }

}
