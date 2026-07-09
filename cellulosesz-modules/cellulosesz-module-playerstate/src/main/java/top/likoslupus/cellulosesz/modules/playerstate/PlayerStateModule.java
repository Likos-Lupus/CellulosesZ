package top.likoslupus.cellulosesz.modules.playerstate;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "playerstate",
        name = "PlayerState",
        description = "Player state service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class PlayerStateModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.playerstate",
                BasicModuleConfig.class,
                "modules/playerstate.yml",
                BasicModuleConfig::new
        );
    }

}
