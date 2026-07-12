package top.likoslupus.cellulosesz.modules.economy;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.command.*;
import top.likoslupus.cellulosesz.modules.economy.service.JsonEconomyService;
import top.likoslupus.cellulosesz.modules.economy.service.JsonWorthService;

import java.util.Objects;

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

        Objects.requireNonNull(config, "EconomyConfig has not been initialized");

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
        var players = context.services().require(PlayerResolver.class);
        var confirmations = context.services().require(ConfirmationService.class);
        var messages = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);

        Objects.requireNonNull(economy, "EconomyService has not been initialized");
        Objects.requireNonNull(config, "EconomyConfig has not been initialized");
        Objects.requireNonNull(worths, "WorthService has not been initialized");

        context.commands().register(new BalanceCommand(platform, users, economy, config));
        context.commands().register(new BalanceTopCommand(platform, users, economy, config));
        context.commands().register(new PayCommand(platform, users, economy, config, players,
                confirmations, messages, locales));
        context.commands().register(new PayToggleCommand(platform, users, economy, config));
        context.commands().register(new PayConfirmToggleCommand(platform, users, economy, config));
        context.commands().register(new EcoCommand(platform, users, economy, config));
        context.commands().register(new WorthCommand(worths));
        context.commands().register(new SetWorthCommand(worths));
    }

}
