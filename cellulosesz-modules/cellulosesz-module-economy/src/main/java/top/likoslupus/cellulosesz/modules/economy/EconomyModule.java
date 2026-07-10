package top.likoslupus.cellulosesz.modules.economy;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.command.*;
import top.likoslupus.cellulosesz.modules.economy.service.JsonEconomyService;
import top.likoslupus.cellulosesz.modules.economy.service.JsonWorthService;

@CellulosesModule(
        id = "economy",
        name = "Economy",
        description = "Internal economy, balance, payments, balance top, and worth services.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class EconomyModule implements CellulosesZModule {

    private @Nullable EconomyConfig config;
    private @Nullable EconomyService economy;
    private @Nullable WorthService worths;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.economy",
                EconomyConfig.class,
                "modules/economy.yml",
                EconomyConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent().resolve("economy");

        economy = new JsonEconomyService(storage, config, root, context.logger());
        worths = new JsonWorthService(storage, root);

        context.services().register(EconomyService.class, economy);
        context.services().register(JsonEconomyService.class, (JsonEconomyService) economy);
        context.services().register(WorthService.class, worths);
        context.services().register(JsonWorthService.class, (JsonWorthService) worths);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        context.commands().register(new BalanceCommand(platform, users, economy, config));
        context.commands().register(new BalanceTopCommand(platform, users, economy, config));
        context.commands().register(new PayCommand(platform, users, economy, config));
        context.commands().register(new PayToggleCommand(platform, users, economy, config));
        context.commands().register(new PayConfirmToggleCommand(platform, users, economy, config));
        context.commands().register(new EcoCommand(platform, users, economy, config));
        context.commands().register(new WorthCommand(worths));
        context.commands().register(new SetWorthCommand(worths));
    }

}
