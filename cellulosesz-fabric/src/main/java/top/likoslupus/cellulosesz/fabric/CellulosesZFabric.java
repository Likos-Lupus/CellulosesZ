package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.platform.PlatformCapability;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.platform.admin.BanPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
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
import java.util.EnumSet;

import static java.util.Objects.requireNonNull;

/**
 * Thin Fabric composition root; common lifecycle and command registration are owned by Architectury common code.
 */
public final class CellulosesZFabric implements DedicatedServerModInitializer {

    private @Nullable CellulosesZBootstrap bootstrap;

    @Override
    public void onInitializeServer() {
        var loader = FabricLoader.getInstance();
        var configDirectory = loader.getConfigDir().resolve("cellulosesz");
        var version = loader.getModContainer("cellulosesz")
                .map(container ->
                        container.getMetadata()
                                .getVersion()
                                .getFriendlyString()
                ).orElse("unknown");

        var logger = new Slf4jCellulosesZLogger(LoggerFactory.getLogger("CellulosesZ"));
        var currentBootstrap = new CellulosesZBootstrap(configDirectory, version, logger);
        bootstrap = currentBootstrap;
        var platform = new FabricPlatformService(logger);
        validatePlatformCapabilities(platform);

        currentBootstrap.registerService(
                PlatformService.class,
                platform
        );
        currentBootstrap.registerService(
                FabricPlatformService.class,
                platform
        );

        var bans = new FabricBanPlatformService();
        currentBootstrap.registerService(BanPlatformService.class, bans);

        var dispatch = new FabricPlayerCommandDispatchService(platform);
        currentBootstrap.registerService(
                PlayerCommandDispatchService.class,
                dispatch
        );
        currentBootstrap.registerService(
                InventoryPlatformService.class,
                new FabricInventoryOperations(platform)
        );
        currentBootstrap.registerService(
                VanishPlatformService.class,
                new FabricVanishPlatformService()
        );
        currentBootstrap.registerService(
                WorldPlatformService.class,
                new FabricWorldOperations(platform)
        );

        var entities = new FabricEntityOperations(platform);
        currentBootstrap.registerService(
                EntityPlatformService.class,
                entities
        );
        currentBootstrap.registerService(
                RecipePlatformService.class,
                new FabricRecipeOperations(platform)
        );

        var hooks = new FabricCommonRuntimeHooks(
                currentBootstrap,
                platform,
                bans,
                dispatch,
                entities,
                this::permissionBackend
        );
        CellulosesZCommon.initialize(new CommonRuntime(
                currentBootstrap,
                platform,
                hooks,
                new FabricCommandRootMutator()
        ));
    }

    private static void validatePlatformCapabilities(PlatformService platform) {
        var required = EnumSet.allOf(PlatformCapability.class);
        required.removeAll(platform.capabilities());
        if (!required.isEmpty()) {
            throw new IllegalStateException("Fabric platform is missing required capabilities: " + required);
        }
    }

    private PermissionBackend permissionBackend() {
        var current = requireNonNull(bootstrap, "CellulosesZBootstrap has not been initialized");
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
            backends.add(new FabricOpPermissionBackend(current.coreConfig().permissions.opFallbackLevel));
        }
        return new CompositePermissionBackend(backends);
    }

}
