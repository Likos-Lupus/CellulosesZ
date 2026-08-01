package top.likoslupus.cellulosesz.modules.sign;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.SignBreakEvent;
import top.likoslupus.cellulosesz.api.event.SignCreateEvent;
import top.likoslupus.cellulosesz.api.event.SignEditEvent;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.sign.*;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.sign.command.EditSignCommand;
import top.likoslupus.cellulosesz.modules.sign.handler.*;
import top.likoslupus.cellulosesz.modules.sign.service.DefaultSignService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "sign",
        name = "Sign",
        description = "Persistent validated interactive sign handlers.",
        phase = ModulePhase.FEATURE,
        requires = {
                "permission",
                "economy",
                "item",
                "teleport",
                "warp",
                "kit",
                "playerstate",
                "world",
                "text",
                "messaging",
                "command"
        }
)
public final class SignModule implements CellulosesZModule {

    private @Nullable SignConfig config;
    private @Nullable DefaultSignService signs;
    private @Nullable EditSignCommand editSign;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.sign",
                SignConfig.class,
                "modules/sign.yml",
                SignConfig::new
        );
        requireNonNull(config, "SignConfig has not been initialized").validate();
    }

    @Override
    @SuppressWarnings("resource")
    public void registerServices(ModuleContext context) {
        var permissions = context.services().require(PermissionService.class);
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
                context,
                requireNonNull(signs, "SignService has not been initialized"),
                economy,
                items,
                playerStates,
                worlds,
                texts,
                mail,
                randomTeleports,
                randomSettings,
                teleports,
                warps,
                kits,
                permissions
        );
        context.services().register(SignService.class, signs);
        context.services().register(DefaultSignService.class, signs);
    }

    private void registerHandlers(
            ModuleContext context,
            DefaultSignService service,
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
            PermissionService permissions
    ) {
        var inventory = context.services().require(InventoryPlatformService.class);
        var itemPlatform = context.services().require(ItemPlatformService.class);
        var workstations = context.services().require(WorkstationPlatformService.class);
        var playerStatePlatform = context.services().require(PlayerStatePlatformService.class);
        var entities = context.services().require(EntityPlatformService.class);
        var worldDirectory = context.services().require(WorldDirectory.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);

        service.register(new WarpSignHandler(
                warps,
                teleports,
                permissions
        ));
        service.register(new BuySignHandler(
                items,
                economy,
                inventory,
                serverThread
        ));
        service.register(new SellSignHandler(
                items,
                economy,
                inventory,
                serverThread,
                context.logger()
        ));
        service.register(new KitSignHandler(kits, permissions));
        service.register(new BalanceSignHandler(economy));
        service.register(new FreeSignHandler(items, inventory));
        service.register(new TradeSignHandler(items, inventory));
        service.register(new EnchantSignHandler(itemPlatform));
        service.register(new RepairSignHandler(itemPlatform));
        service.register(new GameModeSignHandler(playerStatePlatform));
        service.register(new HealSignHandler(playerStates));
        service.register(new InfoSignHandler(texts));
        service.register(new MailSignHandler(mail));
        service.register(new RandomTeleportSignHandler(
                worldDirectory,
                randomTeleports,
                randomSettings,
                teleports
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Anvil",
                WorkstationKind.ANVIL
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Cartography",
                WorkstationKind.CARTOGRAPHY
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Disposal",
                WorkstationKind.DISPOSAL
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Grindstone",
                WorkstationKind.GRINDSTONE
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Loom",
                WorkstationKind.LOOM
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Smithing",
                WorkstationKind.SMITHING
        ));
        service.register(new WorkstationSignHandler(
                workstations,
                "Workbench",
                WorkstationKind.WORKBENCH
        ));
        service.register(new SpawnMobSignHandler(entities));
        service.register(new TimeSignHandler(worldDirectory, worlds));
        service.register(new WeatherSignHandler(worldDirectory, worlds));
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var service = requireNonNull(signs, "SignService has not been initialized");
        var platform = context.services().require(SignPlatformService.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);
        var audience = context.services().require(PlayerAudienceService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);

        context.events().listen(
                PlayerDisconnectEvent.class,
                event -> {
                    var command = editSign;
                    if (command != null) {
                        command.clearClipboard(event.player().uuid());
                    }
                }
        );
        context.events().listen(
                SignCreateEvent.class,
                event -> {
                    var execution = service.create(
                            event.player(),
                            event.location(),
                            event.front(),
                            event.lines()
                    );
                    if (!execution.handled()) {
                        return;
                    }

                    event.cancel();
                    var expected = List.of("", "", "", "");
                    var replacement = service.formattedLines(event.lines());

                    completeMutation(
                            execution,
                            serverThread,
                            audience,
                            renderer,
                            locales,
                            event.player(),
                            () -> platform.compareAndReplace(new SignWriteRequest(
                                    event.player(),
                                    event.location(),
                                    event.front(),
                                    expected,
                                    replacement,
                                    false
                            ))
                    );
                }
        );
        context.events().listen(
                SignEditEvent.class,
                event -> {
                    var execution = service.edit(
                            event.player(),
                            event.location(),
                            event.front(),
                            event.previousLines(),
                            event.lines()
                    );
                    if (!execution.handled()) {
                        return;
                    }

                    event.cancel();
                    var replacement = service.formattedLines(event.lines());

                    completeMutation(
                            execution,
                            serverThread,
                            audience,
                            renderer,
                            locales,
                            event.player(),
                            () -> platform.compareAndReplace(new SignWriteRequest(
                                    event.player(),
                                    event.location(),
                                    event.front(),
                                    event.previousLines(),
                                    replacement,
                                    false
                            ))
                    );
                }
        );
        context.events().listen(
                SignBreakEvent.class,
                event -> {
                    var execution = service.breakSign(
                            event.player(),
                            event.location(),
                            event.frontLines(),
                            event.backLines()
                    );
                    if (!execution.handled()) {
                        return;
                    }

                    event.cancel();
                    completeMutation(
                            execution,
                            serverThread,
                            audience,
                            renderer,
                            locales,
                            event.player(),
                            () -> platform.compareAndBreak(new SignBreakRequest(
                                    event.player(),
                                    event.location(),
                                    event.frontLines(),
                                    event.backLines()
                            ))
                    );
                }
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        editSign = new EditSignCommand(
                context.services().require(SignPlatformService.class),
                requireNonNull(signs, "SignService has not been initialized"),
                context.services().require(ServerThreadExecutor.class),
                requireNonNull(config, "SignConfig has not been initialized")
        );
        context.track(context
                .services()
                .require(CommandRegistry.class)
                .register("editsign", editSign)
        );

        var catalog = context.services().require(PermissionCatalog.class);
        catalog.register("cellulosesz.command.editsign.waxed", "Edit waxed signs");
        catalog.register("cellulosesz.command.editsign.color", "Use legacy colors on edited signs");
        catalog.register("cellulosesz.command.editsign.format", "Use formatted sign text");
        catalog.register("cellulosesz.command.editsign.rgb", "Use RGB colors on edited signs");
    }

    @Override
    public void onReload(ModuleContext context) {
        var candidate = context.configs().require("module.sign", SignConfig.class);
        candidate.validate();
        requireNonNull(signs, "SignService has not been initialized").configure(candidate);
        config = candidate;
    }

    private void completeMutation(
            SignMutationExecution execution,
            ServerThreadExecutor serverThread,
            PlayerAudienceService audience,
            MessageRenderer renderer,
            LocaleResolver locales,
            CellPlayer player,
            Supplier<PlatformResult<?>> platformAction
    ) {
        execution
                .preparation()
                .whenComplete((commit, preparationFailure) -> serverThread
                        .execute(() -> {
                            if (preparationFailure != null) {
                                send(
                                        audience,
                                        renderer,
                                        locales,
                                        player,
                                        failure(preparationFailure)
                                );
                                return;
                            }

                            if (!commit.platformActionRequired()) {
                                commit
                                        .complete(true)
                                        .whenComplete((result, completionFailure) -> serverThread
                                                .execute(
                                                        () ->
                                                                send(
                                                                        audience,
                                                                        renderer,
                                                                        locales,
                                                                        player,
                                                                        completionFailure == null
                                                                                ? result
                                                                                : failure(
                                                                                        completionFailure
                                                                                )
                                                                )
                                                ));
                                return;
                            }

                            PlatformResult<?> applied;
                            try {
                                applied = platformAction.get();
                            } catch (RuntimeException exception) {
                                applied = PlatformResult.failure(
                                        PlatformOperationStatus.INTERNAL_ERROR,
                                        exception.getClass().getSimpleName()
                                );
                            }
                            commit
                                    .complete(applied.successful())
                                    .whenComplete((result, completionFailure) ->
                                            serverThread.execute(() -> send(
                                                    audience,
                                                    renderer,
                                                    locales,
                                                    player,
                                                    completionFailure == null
                                                            ? result
                                                            : failure(completionFailure)
                                            ))
                                    );
                        }));
    }

    private void send(
            PlayerAudienceService audience,
            MessageRenderer renderer,
            LocaleResolver locales,
            CellPlayer player,
            SignUseResult result
    ) {
        result.optionalMessage()
                .ifPresent(message -> audience
                        .send(
                                player,
                                renderer.render(
                                        locales.locale(player),
                                        message.key(),
                                        message.placeholders()
                                )
                        )
                );
    }

    private SignUseResult failure(Throwable throwable) {
        var cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        var message = cause.getMessage();
        return SignUseResult.failure(
                "service.sign.execution-failed",
                Map.of(
                        "reason",
                        message == null || message.isBlank()
                                ? cause.getClass().getSimpleName()
                                : message
                )
        );
    }

}
