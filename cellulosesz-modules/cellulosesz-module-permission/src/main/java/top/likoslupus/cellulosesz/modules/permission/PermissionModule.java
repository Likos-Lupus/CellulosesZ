package top.likoslupus.cellulosesz.modules.permission;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.core.permission.DefaultPermissionService;
import top.likoslupus.cellulosesz.modules.permission.config.PermissionConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "permission",
        name = "Permission",
        description = "Permission provider integration and cache.",
        phase = ModulePhase.CORE
)
public final class PermissionModule implements CellulosesZModule {

    private @Nullable PermissionConfig config;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.permission",
                PermissionConfig.class,
                "modules/permission.yml",
                PermissionConfig::new
        );
        config = context.configs().require("module.permission", PermissionConfig.class);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var permissions = context.services().require(DefaultPermissionService.class);
        requireNonNull(config);
        permissions.cache(
                config.cache.enabled,
                config.cache.expireSeconds
        );
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previous = requireNonNull(config, "PermissionConfig has not been initialized");
        var candidate = reload.configs().require("module.permission", PermissionConfig.class);
        var permissions = context.services().require(DefaultPermissionService.class);

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    config = candidate;
                    permissions.cache(candidate.cache.enabled, candidate.cache.expireSeconds);
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    config = previous;
                    permissions.cache(previous.cache.enabled, previous.cache.expireSeconds);
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

}
