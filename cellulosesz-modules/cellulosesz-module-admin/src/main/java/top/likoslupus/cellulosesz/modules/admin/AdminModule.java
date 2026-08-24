package top.likoslupus.cellulosesz.modules.admin;

import top.likoslupus.cellulosesz.api.event.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.player.*;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.common.admin.BanPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.common.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.common.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.common.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.core.module.*;
import top.likoslupus.cellulosesz.core.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.core.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.application.*;
import top.likoslupus.cellulosesz.modules.admin.command.*;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.domain.JailState;
import top.likoslupus.cellulosesz.modules.admin.service.*;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class AdminModule implements CellulosesZModule {

    private @Nullable AdminConfig config;
    private @Nullable AdminRuntimeSettings runtimeSettings;
    private @Nullable TempBanService tempBans;
    private @Nullable MuteService mutes;
    private @Nullable JailService jails;
    private @Nullable AddressBookService addresses;
    private @Nullable MuteCommandMiddleware mutePolicy;
    private @Nullable JailEnforcementService enforcement;
    private @Nullable TaskHandle maintenance;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.admin",
                AdminConfig.class,
                "modules/admin.yml",
                AdminConfig::new
        );
        config = context.configs()
                .require("module.admin", AdminConfig.class)
                .validatedCopy();
        runtimeSettings = new AdminRuntimeSettings(config);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(
                config,
                "AdminConfig has not been initialized"
        );
        var settings = requireNonNull(
                runtimeSettings,
                "AdminRuntimeSettings has not been initialized"
        );
        var clock = Clock.systemUTC();
        var storage = context.services().require(StorageService.class);
        var players = context.services().require(PlayerDirectory.class);
        var connections = context.services().require(PlayerConnectionService.class);
        var audience = context.services().require(PlayerAudienceService.class);
        var permissions = context.services().require(PermissionService.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);
        var networks = context.services().require(PlayerNetworkService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var resolver = context.services().require(PlayerResolver.class);
        var locations = context.services().require(PlayerLocationPlatformService.class);
        var teleports = context.services().require(TeleportService.class);
        var root = context.dataDirectory().getParent().resolve("admin");

        var bans = new DefaultBanService(
                context.services().require(BanPlatformService.class),
                players,
                connections,
                audience,
                permissions,
                clock
        );

        addresses = new JsonAddressBookService(storage, root.resolve("addresses.json"));
        mutes = new JsonMuteService(storage, root.resolve("mutes.json"), clock);
        tempBans = new JsonTempBanService(
                storage,
                root.resolve("temp-bans.json"),
                players,
                connections,
                audience,
                networks,
                renderer,
                serverThread,
                clock,
                settings
        );
        jails = new JsonJailService(
                storage,
                root.resolve("jails.json"),
                players,
                locations,
                teleports,
                serverThread,
                clock,
                settings
        );
        enforcement = new JailEnforcementService(jails, locations, teleports, settings);

        var banCommands = new DefaultBanCommandService(
                bans,
                tempBans,
                resolver,
                players,
                networks,
                addresses,
                serverThread,
                settings
        );
        var moderation = new DefaultModerationCommandService(
                bans,
                mutes,
                players,
                resolver,
                permissions,
                serverThread,
                clock,
                settings
        );
        var jailCommands = new DefaultJailCommandService(
                jails,
                players,
                resolver,
                locations,
                serverThread,
                clock,
                settings
        );
        var controls = new DefaultPlayerControlCommandService(
                players,
                context.services().require(PlayerStatePlatformService.class),
                permissions,
                context.services().require(PlayerCommandDispatchService.class),
                settings
        );

        context.services().register(BanService.class, bans);
        context.services().register(TempBanService.class, tempBans);
        context.services().register(AddressBookService.class, addresses);
        context.services().register(MuteService.class, mutes);
        context.services().register(JailService.class, jails);
        context.services().register(BanCommandService.class, banCommands);
        context.services().register(ModerationCommandService.class, moderation);
        context.services().register(JailCommandService.class, jailCommands);
        context.services().register(PlayerControlCommandService.class, controls);
        context.services().register(JailEnforcementService.class, enforcement);

        mutePolicy = new MuteCommandMiddleware(mutes, current);
        context.middlewares().addMiddleware(mutePolicy);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var temporary = requireNonNull(
                tempBans,
                "TempBanService has not been initialized"
        );
        var mute = requireNonNull(
                mutes,
                "MuteService has not been initialized"
        );
        var jailService = requireNonNull(
                jails,
                "JailService has not been initialized"
        );
        var addressBook = requireNonNull(
                addresses,
                "AddressBookService has not been initialized"
        );
        var jailEnforcement = requireNonNull(
                enforcement,
                "JailEnforcementService has not been initialized"
        );
        var networks = context.services().require(PlayerNetworkService.class);
        var connections = context.services().require(PlayerConnectionService.class);
        var audience = context.services().require(PlayerAudienceService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var permissions = context.services().require(PermissionService.class);

        context.events().listen(
                PlayerJoinEvent.class,
                event -> {
                    var player = event.player();
                    var address = networks.address(player);

                    if (address != null) {
                        addressBook.remember(
                                        player.uuid(),
                                        player.name(),
                                        address
                                )
                                .whenComplete((_, failure) -> {
                                    if (failure != null) {
                                        context.logger().error(
                                                "Failed to persist a player login address",
                                                failure
                                        );
                                    }
                                });
                    }

                    var active = temporary.active(player.uuid(), player.name());
                    if (active.isEmpty() && address != null) {
                        active = temporary.activeIp(address);
                    }

                    if (active.isPresent()) {
                        connections.disconnect(
                                player,
                                renderer.render(
                                        audience.locale(player),
                                        "service.admin.temp-ban-kick",
                                        MessageArguments.builder()
                                                .add(active.orElseThrow().reason())
                                                .build()
                                )
                        );
                        return;
                    }

                    var jailed = jailService.jailed(player.uuid());
                    if (jailed.filter(value ->
                                    value.state() == JailState.RELEASE_PENDING
                            )
                            .isPresent()
                    ) {
                        jailService
                                .completePendingRelease(player)
                                .whenComplete((_, failure) -> {
                                    if (failure != null) {
                                        context.logger().error(
                                                "Failed to complete pending jail release",
                                                failure
                                        );
                                    }
                                });
                    } else {
                        jailEnforcement
                                .enforce(player)
                                .whenComplete((_, failure) -> {
                                    if (failure != null) {
                                        context.logger().error(
                                                "Failed to enforce jail on join",
                                                failure
                                        );
                                    }
                                });
                    }
                }
        );

        context.events().listen(
                PlayerChatEvent.class,
                event -> {
                    if (permissions.has(event.player(), "cellulosesz.admin.mute.bypass")
                            || !mute.muted(event.player().uuid())
                    ) {
                        return;
                    }

                    event.cancel();
                    audience.send(
                            event.player(),
                            renderer.render(
                                    audience.locale(event.player()),
                                    "service.admin.muted-chat",
                                    MessageArguments.empty()
                            )
                    );
                }
        );

        context.events().listen(
                PlayerCommandPreprocessEvent.class,
                event -> {
                    if (permissions.has(event.player(), "cellulosesz.admin.mute.bypass")
                            || !mute.muted(event.player().uuid())
                    ) {
                        return;
                    }

                    var root = normalizeRoot(event.command());
                    var middleware = requireNonNull(
                            mutePolicy,
                            "MuteCommandMiddleware has not been initialized"
                    );

                    if (!middleware.blocked(root)) {
                        return;
                    }

                    event.cancel();
                    audience.send(
                            event.player(),
                            renderer.render(
                                    audience.locale(event.player()),
                                    "commands.admin.mute-command-middleware.error.muted-cannot-use-command",
                                    MessageArguments.empty()
                            )
                    );
                }
        );

        context.events().listen(
                PlayerMoveEvent.class,
                event -> jailEnforcement
                        .activeJail(event.player())
                        .ifPresent(jail -> {
                            if (!jailEnforcement.inside(jail.location(), event.to())) {
                                event.to(jail.location());
                                event.cancel();
                            }
                        })
        );

        context.events().listen(
                PlayerRespawnEvent.class,
                event -> jailEnforcement
                        .activeJail(event.player())
                        .ifPresent(jail -> event.location(jail.location()))
        );

        context.events().listen(
                PlayerWorldChangeEvent.class,
                event -> jailEnforcement
                        .enforce(event.player())
                        .whenComplete((_, failure) -> {
                            if (failure != null) {
                                context.logger().error(
                                        "Failed to enforce jail after world change",
                                        failure
                                );
                            }
                        })
        );

        context.events().listen(
                PlayerGameModeChangeEvent.class,
                event -> {
                    if (jailService
                            .jailed(event.player().uuid())
                            .filter(value -> value.state() == JailState.ACTIVE)
                            .isPresent()
                    ) {
                        event.cancel();
                    }
                }
        );

        context.events().listen(
                PlayerAttackEvent.class,
                event -> {
                    if (jailService
                            .jailed(event.player().uuid())
                            .filter(value -> value.state() == JailState.ACTIVE)
                            .isPresent()
                    ) {
                        event.cancel();
                    }
                }
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var players = context.services().require(PlayerDirectory.class);
        var current = requireNonNull(config, "AdminConfig has not been initialized");
        var maximum = maximumDuration(current.maximumPunishmentSeconds);
        var muteMaximum = maximumDuration(current.maximumMuteSeconds);
        var ban = context.services().require(BanCommandService.class);
        var moderation = context.services().require(ModerationCommandService.class);
        var jail = context.services().require(JailCommandService.class);
        var controls = context.services().require(PlayerControlCommandService.class);

        track(context, registry, "ban-command", new BanCommand(ban, players));
        track(context, registry, "banip-command", new BanIpCommand(ban, players));
        track(
                context,
                registry,
                "burn-command",
                new BurnCommand(controls, current.maximumBurnSeconds)
        );
        track(context, registry, "deljail-command", new DelJailCommand(jail));
        track(context, registry, "ext-command", new ExtCommand(controls, players));
        track(context, registry, "ice-command", new IceCommand(controls, players));
        track(context, registry, "jail-command", new JailCommand(jail, players, maximum));
        track(context, registry, "jailedplayers-command", new JailedPlayersCommand(jail));
        track(context, registry, "jails-command", new JailsCommand(jail));
        track(context, registry, "kick-command", new KickCommand(moderation, players));
        track(context, registry, "kickall-command", new KickAllCommand(moderation, players));
        track(context, registry, "kill-command", new KillCommand(controls));
        track(context, registry, "mute-command", new MuteCommand(moderation, players, muteMaximum));
        track(context, registry, "setjail-command", new SetJailCommand(jail, players));
        track(context, registry, "sudo-command", new SudoCommand(controls, players));
        track(context, registry, "suicide-command", new SuicideCommand(controls, players));
        track(context, registry, "tempban-command", new TempBanCommand(ban, players, maximum));
        track(context, registry, "tempbanip-command", new TempBanIpCommand(ban, players, maximum));
        track(context, registry, "unban-command", new UnbanCommand(ban, players));
        track(context, registry, "unbanip-command", new UnbanIpCommand(ban, players));

        registerCommandPermissions(context.services().require(PermissionCatalog.class));
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        schedule(context);
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previous = requireNonNull(config, "AdminConfig has not been initialized");
        var candidate = reload.configs()
                .require("module.admin", AdminConfig.class)
                .validatedCopy();

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    config = candidate;
                    requireNonNull(runtimeSettings, "AdminRuntimeSettings has not been initialized")
                            .configure(candidate);
                    requireNonNull(mutePolicy, "MuteCommandMiddleware has not been initialized")
                            .configure(candidate);
                    schedule(context);
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    config = previous;
                    requireNonNull(runtimeSettings, "AdminRuntimeSettings has not been initialized")
                            .configure(previous);
                    requireNonNull(mutePolicy, "MuteCommandMiddleware has not been initialized")
                            .configure(previous);
                    schedule(context);
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    private void schedule(ModuleContext context) {
        if (maintenance != null) {
            maintenance.close();
        }

        var seconds = requireNonNull(
                runtimeSettings,
                "AdminRuntimeSettings has not been initialized"
        ).jailedPlayerCheckSeconds();

        var period = Math.multiplyExact(seconds, 20L);

        maintenance = context.scope().own(context.scheduler().syncRepeating(
                () -> {
                    requireNonNull(tempBans, "TempBanService has not been initialized")
                            .purgeExpired()
                            .whenComplete((_, failure) -> {
                                if (failure != null) {
                                    context.logger().error(
                                            "Failed to purge expired temporary bans",
                                            failure
                                    );
                                }
                            });

                    requireNonNull(mutes, "MuteService has not been initialized")
                            .purgeExpired()
                            .whenComplete((_, failure) -> {
                                if (failure != null) {
                                    context.logger().error(
                                            "Failed to purge expired mutes",
                                            failure
                                    );
                                }
                            });

                    requireNonNull(jails, "JailService has not been initialized")
                            .purgeExpired()
                            .whenComplete((_, failure) -> {
                                if (failure != null) {
                                    context.logger().error(
                                            "Failed to purge expired jail records",
                                            failure
                                    );
                                }
                            });

                    var service = requireNonNull(
                            enforcement,
                            "JailEnforcementService has not been initialized"
                    );

                    context.services()
                            .require(PlayerDirectory.class)
                            .onlinePlayers()
                            .forEach(player -> service
                                    .enforce(player)
                                    .whenComplete((_, failure) -> {
                                        if (failure != null) {
                                            context.logger().error(
                                                    "Failed to enforce jail maintenance",
                                                    failure
                                            );
                                        }
                                    })
                            );
                },
                20L,
                period
        ));
    }

    private static Duration maximumDuration(long seconds) {
        return seconds < 0
                ? Duration.ofDays(365_000L)
                : Duration.ofSeconds(seconds);
    }

    private static void track(
            ModuleContext context,
            CommandRegistry registry,
            String id,
            CommandContributor contributor
    ) {
        context.scope().own(registry.register(id, contributor));
    }

    private static void registerCommandPermissions(PermissionCatalog catalog) {
        Map.ofEntries(
                Map.entry(
                        "cellulosesz.command.ext.others",
                        "Extinguish other players"
                ),
                Map.entry(
                        "cellulosesz.command.ice.others",
                        "Freeze other players"
                ),
                Map.entry(
                        "cellulosesz.command.kill.exempt",
                        "Exempt a player from administrative kill"
                ),
                Map.entry(
                        "cellulosesz.command.kill.force",
                        "Bypass kill exemptions and a prevented death"
                ),
                Map.entry(
                        "cellulosesz.command.sudo.exempt",
                        "Exempt a player from sudo"
                ),
                Map.entry(
                        "cellulosesz.admin.kickall.exempt",
                        "Exempt a player from kickall"
                )
        ).forEach(catalog::register);
    }

    private static String normalizeRoot(String command) {
        var raw = command.trim();

        while (raw.startsWith("/")) {
            raw = raw.substring(1);
        }

        var separator = raw.indexOf(' ');
        var root = separator < 0
                ? raw
                : raw.substring(0, separator);
        var namespace = root.indexOf(':');

        return (
                namespace >= 0
                        ? root.substring(namespace + 1)
                        : root
        ).toLowerCase(Locale.ROOT);
    }

}
