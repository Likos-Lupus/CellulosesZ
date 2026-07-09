package top.likoslupus.cellulosesz.modules.permission;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.permission.DefaultPermissionService;
import top.likoslupus.cellulosesz.modules.permission.config.PermissionConfig;

@CellulosesModule(
        id = "permission",
        name = "Permission",
        description = "Permission provider integration and cache.",
        phase = ModulePhase.CORE
)
public final class PermissionModule implements CellulosesZModule {

    private PermissionConfig config;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.permission",
                PermissionConfig.class,
                "modules/permission.yml",
                PermissionConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var permissions = context.services().require(DefaultPermissionService.class);
        permissions.cache(config.cache.enabled, config.cache.expireSeconds);
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.permission", PermissionConfig.class);
        var permissions = context.services().require(DefaultPermissionService.class);
        permissions.cache(config.cache.enabled, config.cache.expireSeconds);
    }

}
