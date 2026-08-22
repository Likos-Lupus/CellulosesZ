package top.likoslupus.cellulosesz.modules.economy;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.WorthService;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.economy.application.BalanceCommandService;
import top.likoslupus.cellulosesz.modules.economy.application.EconomyCommandSettings;
import top.likoslupus.cellulosesz.modules.economy.application.ItemValueCommandService;
import top.likoslupus.cellulosesz.modules.economy.application.PaymentCommandService;
import top.likoslupus.cellulosesz.modules.economy.command.*;
import top.likoslupus.cellulosesz.modules.economy.service.JsonEconomyService;
import top.likoslupus.cellulosesz.modules.economy.service.JsonWorthService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class EconomyModule implements CellulosesZModule {

    private final AtomicLong configVersion = new AtomicLong();
    private @Nullable EconomyConfig config;
    private volatile @Nullable EconomyCommandSettings settings;
    private @Nullable EconomyService economy;
    private @Nullable WorthService worths;
    private @Nullable BalanceCommandService balances;
    private @Nullable PaymentCommandService payments;
    private @Nullable ItemValueCommandService itemValues;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.economy",
                EconomyConfig.class,
                "modules/economy.yml",
                EconomyConfig::new
        );

        var registered = context.configs().require(
                "module.economy",
                EconomyConfig.class
        );

        settings = EconomyCommandSettings.from(
                registered,
                configVersion.incrementAndGet()
        );
        config = registered;
    }

    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent().resolve("economy");
        var currentConfig = requireNonNull(config, "EconomyConfig has not been initialized");

        economy = new JsonEconomyService(storage, currentConfig, root, context.logger());
        worths = new JsonWorthService(storage, root);
        var economyService = requireNonNull(economy, "economy");
        var worthService = requireNonNull(worths, "worths");
        var settingsSupplier = (Supplier<EconomyCommandSettings>) () -> requireNonNull(
                settings,
                "Economy command settings have not been initialized"
        );

        balances = new BalanceCommandService(
                economyService,
                context.services().require(PlayerResolver.class),
                context.services().require(NameCacheService.class),
                settingsSupplier
        );
        payments = new PaymentCommandService(
                economyService,
                context.services().require(UserService.class),
                context.services().require(PlayerResolver.class),
                context.services().require(ConfirmationService.class),
                context.services().require(PlayerAudienceService.class),
                context.services().require(MessageRenderer.class),
                settingsSupplier
        );
        itemValues = new ItemValueCommandService(
                context.services().require(InventoryPlatformService.class),
                context.services().require(ItemService.class),
                worthService,
                economyService,
                context.services().require(ServerThreadExecutor.class)
        );

        context.services().register(EconomyService.class, economyService);
        context.services().register(JsonEconomyService.class, (JsonEconomyService) economyService);
        context.services().register(WorthService.class, worthService);
        context.services().register(JsonWorthService.class, (JsonWorthService) worthService);
        context.services().register(BalanceCommandService.class, requireNonNull(balances));
        context.services().register(PaymentCommandService.class, requireNonNull(payments));
        context.services().register(ItemValueCommandService.class, requireNonNull(itemValues));
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var players = context.services().require(PlayerDirectory.class);
        var balanceService = requireNonNull(
                balances,
                "BalanceCommandService has not been initialized"
        );
        var paymentService = requireNonNull(
                payments,
                "PaymentCommandService has not been initialized"
        );
        var itemService = requireNonNull(
                itemValues,
                "ItemValueCommandService has not been initialized"
        );
        var settingsSupplier = (Supplier<EconomyCommandSettings>) () -> requireNonNull(
                settings,
                "Economy command settings have not been initialized"
        );

        track(
                context,
                registry,
                "balance-command",
                new BalanceCommand(balanceService, players)
        );
        track(
                context,
                registry,
                "balancetop-command",
                new BalanceTopCommand(balanceService, settingsSupplier)
        );
        track(
                context,
                registry,
                "eco-command",
                new EcoCommand(balanceService, settingsSupplier)
        );
        track(
                context,
                registry,
                "pay-command",
                new PayCommand(paymentService, players, settingsSupplier)
        );
        track(
                context,
                registry,
                "payconfirmtoggle-command",
                new PayConfirmToggleCommand(paymentService, players)
        );
        track(
                context,
                registry,
                "paytoggle-command",
                new PayToggleCommand(paymentService, players)
        );
        track(
                context,
                registry,
                "sell-command",
                new SellCommand(itemService, players)
        );
        track(
                context,
                registry,
                "setworth-command",
                new SetWorthCommand(itemService, settingsSupplier)
        );
        track(
                context,
                registry,
                "worth-command",
                new WorthCommand(itemService, players)
        );
    }

    private static void track(
            ModuleContext context,
            CommandRegistry registry,
            String id,
            CommandContributor contributor
    ) {
        context.scope().own(registry.register(id, contributor));
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var previousConfig = requireNonNull(
                config,
                "EconomyConfig has not been initialized"
        );
        var previousSettings = requireNonNull(
                settings,
                "Economy settings have not been initialized"
        );
        var previousVersion = configVersion.get();
        var candidate = reload.configs().require("module.economy", EconomyConfig.class);
        var candidateSettings = EconomyCommandSettings.from(candidate, previousVersion + 1L);
        var serviceReload = economy instanceof JsonEconomyService service
                ? service.prepareConfiguration(candidate)
                : PreparedReloads.noop();

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> serviceReload
                        .commit()
                        .thenRun(() -> {
                            config = candidate;
                            settings = candidateSettings;
                            configVersion.set(previousVersion + 1L);
                        }),
                () -> serviceReload
                        .rollback()
                        .thenRun(() -> {
                            config = previousConfig;
                            settings = previousSettings;
                            configVersion.set(previousVersion);
                        })
        ));
    }

}
