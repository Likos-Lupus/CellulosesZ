package top.likoslupus.cellulosesz.modules.messaging;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "messaging",
        name = "Messaging",
        description = "Private message and mail placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class MessagingModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.messaging",
                BasicModuleConfig.class,
                "modules/messaging.yml",
                BasicModuleConfig::new
        );
    }

}
