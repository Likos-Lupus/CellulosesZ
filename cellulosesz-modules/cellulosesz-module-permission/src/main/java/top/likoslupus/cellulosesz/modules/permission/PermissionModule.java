package top.likoslupus.cellulosesz.modules.permission;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "permission",
        name = "Permission",
        description = "Permission provider integration placeholder.",
        phase = ModulePhase.CORE
)
public final class PermissionModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.permission",
                BasicModuleConfig.class,
                "modules/permission.yml",
                BasicModuleConfig::new
        );
    }

}
