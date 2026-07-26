package top.likoslupus.cellulosesz.modules.sign;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.event.SignBreakEvent;
import top.likoslupus.cellulosesz.api.event.SignCreateEvent;
import top.likoslupus.cellulosesz.api.event.SignEditEvent;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.sign.SignMutationExecution;
import top.likoslupus.cellulosesz.api.sign.SignService;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.modules.sign.handler.*;
import top.likoslupus.cellulosesz.modules.sign.service.DefaultSignService;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "sign",
        name = "Sign",
        description = "Persistent validated interactive sign handlers.",
        phase = ModulePhase.FEATURE,
        requires = {
                "permission", "economy", "item", "teleport", "warp", "kit",
                "playerstate", "world", "text", "messaging"
        }
)
public final class SignModule implements CellulosesZModule {

    private @Nullable SignConfig config;
    private @Nullable DefaultSignService signs;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.sign",
                SignConfig.class,
                "modules/sign.yml",
                SignConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var permissions = context.services().require(PermissionService.class);
        var platform = context.services().require(PlatformService.class);
        var storage = context.services().require(StorageService.class);
        var items = context.services().require(ItemService.class);
        var economy = context.services().require(EconomyService.class);
        var warps = context.services().require(WarpService.class);
        var teleports = context.services().require(TeleportService.class);
        var kits = context.services().require(KitService.class);
        var playerStates = context.services().require(PlayerStateService.class);
        var worlds = context.services().require(WorldService.class);
        var texts = context.services().require(TextService.class);
        var mail = context.services().require(MailService.class);
        var randomTeleports = context.services().require(RandomTeleportService.class);
        var randomSettings = context.services().require(RandomTeleportSettingsService.class);

        signs = new DefaultSignService(
                requireNonNull(config, "SignConfig has not been initialized"),
                permissions,
                storage,
                context.dataDirectory().resolve("signs.json")
        );
        registerHandlers(
                signs, platform, economy, items, playerStates, worlds, texts, mail,
                randomTeleports, randomSettings, teleports, warps, kits, permissions, context.logger()
        );
        context.services().register(SignService.class, signs);
        context.services().register(DefaultSignService.class, signs);
    }

    private void registerHandlers(
            DefaultSignService service,
            PlatformService platform,
            EconomyService economy,
            ItemService items,
            PlayerStateService playerStates,
            WorldService worlds,
            TextService texts,
            MailService mail,
            RandomTeleportService randomTeleports,
            RandomTeleportSettingsService randomSettings,
            TeleportService teleports,
            WarpService warps,
            KitService kits,
            PermissionService permissions,
            top.likoslupus.cellulosesz.api.logging.CellulosesZLogger logger
    ) {
        service.register(new WarpSignHandler(warps, teleports, permissions));
        service.register(new BuySignHandler(items, economy, platform));
        service.register(new SellSignHandler(items, economy, platform, logger));
        service.register(new KitSignHandler(kits, permissions));
        service.register(new BalanceSignHandler(economy));
        service.register(new FreeSignHandler(items, platform));
        service.register(new TradeSignHandler(items, platform));
        service.register(new EnchantSignHandler(platform));
        service.register(new RepairSignHandler(platform));
        service.register(new GameModeSignHandler(platform));
        service.register(new HealSignHandler(playerStates));
        service.register(new InfoSignHandler(texts));
        service.register(new MailSignHandler(mail));
        service.register(new RandomTeleportSignHandler(platform, randomTeleports, randomSettings, teleports));
        service.register(new WorkstationSignHandler(platform, "Anvil", "anvil"));
        service.register(new WorkstationSignHandler(platform, "Cartography", "cartography"));
        service.register(new WorkstationSignHandler(platform, "Disposal", "disposal"));
        service.register(new WorkstationSignHandler(platform, "Grindstone", "grindstone"));
        service.register(new WorkstationSignHandler(platform, "Loom", "loom"));
        service.register(new WorkstationSignHandler(platform, "Smithing", "smithing"));
        service.register(new WorkstationSignHandler(platform, "Workbench", "workbench"));
        service.register(new SpawnMobSignHandler(platform));
        service.register(new TimeSignHandler(platform, worlds));
        service.register(new WeatherSignHandler(platform, worlds));
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var service = requireNonNull(signs, "SignService has not been initialized");
        var platform = context.services().require(PlatformService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);

        context.events().listen(SignCreateEvent.class, event -> {
            var execution = service.create(event.player(), event.location(), event.front(), event.lines());
            if (!execution.handled()) return;
            event.cancel();
            var expected = List.of("", "", "", "");
            var replacement = service.formattedLines(event.lines());
            completeMutation(
                    execution,
                    platform,
                    renderer,
                    locales,
                    event.player(),
                    () -> platform.replaceSignText(
                            event.player(), event.location(), event.front(), expected, replacement)
            );
        });
        context.events().listen(SignEditEvent.class, event -> {
            var execution = service.edit(
                    event.player(), event.location(), event.front(), event.previousLines(), event.lines()
            );
            if (!execution.handled()) return;
            event.cancel();
            var replacement = service.formattedLines(event.lines());
            completeMutation(
                    execution,
                    platform,
                    renderer,
                    locales,
                    event.player(),
                    () -> platform.replaceSignText(
                            event.player(), event.location(), event.front(), event.previousLines(), replacement)
            );
        });
        context.events().listen(SignBreakEvent.class, event -> {
            var execution = service.breakSign(
                    event.player(), event.location(), event.frontLines(), event.backLines()
            );
            if (!execution.handled()) return;
            event.cancel();
            completeMutation(
                    execution,
                    platform,
                    renderer,
                    locales,
                    event.player(),
                    () -> platform.breakSignBlock(
                            event.player(), event.location(), event.frontLines(), event.backLines())
            );
        });
    }

    @Override
    public void onReload(ModuleContext context) {
        var candidate = context.configs().require("module.sign", SignConfig.class);
        requireNonNull(signs, "SignService has not been initialized").configure(candidate);
        config = candidate;
    }

    private void completeMutation(
            SignMutationExecution execution,
            PlatformService platform,
            MessageRenderer renderer,
            LocaleResolver locales,
            CellPlayer player,
            BooleanSupplier platformAction
    ) {
        execution.preparation().whenComplete((commit, preparationFailure) ->
                platform.runOnServerThread(() -> {
                    if (preparationFailure != null) {
                        send(platform, renderer, locales, player, failure(preparationFailure));
                        return;
                    }
                    if (!commit.platformActionRequired()) {
                        commit.complete(true).whenComplete((result, completionFailure) ->
                                platform.runOnServerThread(() -> send(
                                        platform, renderer, locales, player,
                                        completionFailure == null ? result : failure(completionFailure)
                                ))
                        );
                        return;
                    }

                    boolean applied;
                    try {
                        applied = platformAction.getAsBoolean();
                    } catch (RuntimeException exception) {
                        applied = false;
                    }
                    commit.complete(applied).whenComplete((result, completionFailure) ->
                            platform.runOnServerThread(() -> send(
                                    platform, renderer, locales, player,
                                    completionFailure == null ? result : failure(completionFailure)
                            ))
                    );
                })
        );
    }

    private void send(
            PlatformService platform,
            MessageRenderer renderer,
            LocaleResolver locales,
            CellPlayer player,
            SignUseResult result
    ) {
        result.optionalMessage().ifPresent(message -> platform.sendMessage(
                player,
                renderer.render(locales.locale(player), message.key(), message.placeholders())
        ));
    }

    private SignUseResult failure(Throwable throwable) {
        var cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        var message = cause.getMessage();
        return SignUseResult.failure(
                "service.sign.execution-failed",
                java.util.Map.of(
                        "reason",
                        message == null || message.isBlank() ? cause.getClass().getSimpleName() : message
                )
        );
    }

}
