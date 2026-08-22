package top.likoslupus.cellulosesz.modules.playerstate;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.event.*;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.*;
import top.likoslupus.cellulosesz.api.playerstate.*;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerAbilityCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerInformationCommandService;
import top.likoslupus.cellulosesz.modules.playerstate.application.PlayerStateCommandSettings;
import top.likoslupus.cellulosesz.modules.playerstate.command.*;
import top.likoslupus.cellulosesz.modules.playerstate.config.PlayerStateConfig;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultPlayerStateService;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultVanishService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class PlayerStateModule implements CellulosesZModule {

    private final Map<UUID, PersonalTimeSetting> lastTime = new ConcurrentHashMap<>();
    private final Map<UUID, PersonalWeatherSetting> lastWeather = new ConcurrentHashMap<>();

    private @Nullable PlayerStateConfig config;
    private @Nullable PlayerStateCommandSettings settings;
    private @Nullable DefaultPlayerStateService states;
    private @Nullable DefaultVanishService vanish;
    private @Nullable PlayerAbilityCommandService abilities;
    private @Nullable PlayerInformationCommandService information;
    private @Nullable NearCommand nearCommand;
    private @Nullable TaskHandle afkTask;
    private @Nullable TaskHandle personalWorldTask;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.playerstate",
                PlayerStateConfig.class,
                "modules/playerstate.yml",
                PlayerStateConfig::new
        );
        config = context.configs().require("module.playerstate", PlayerStateConfig.class);
        settings = PlayerStateCommandSettings.from(config);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var users = context.services().require(UserService.class);
        var permissions = context.services().require(PermissionService.class);
        var players = context.services().require(PlayerDirectory.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);
        var displayNames = context.services().require(DisplayNameService.class);
        var statePlatform = context.services().require(PlayerStatePlatformService.class);

        states = new DefaultPlayerStateService(
                statePlatform,
                serverThread,
                players,
                users,
                displayNames
        );
        vanish = new DefaultVanishService(
                context.services().require(VanishPlatformService.class),
                players,
                serverThread,
                users,
                permissions,
                displayNames
        );
        abilities = new PlayerAbilityCommandService(
                states,
                vanish,
                statePlatform,
                serverThread
        );
        information = new PlayerInformationCommandService(
                players,
                context.services().require(PlayerLocationPlatformService.class),
                statePlatform,
                context.services().require(PlayerResolver.class),
                users,
                vanish,
                displayNames,
                serverThread,
                requireNonNull(settings, "settings")
        );

        context.services().register(PlayerStateService.class, states);
        context.services().register(DefaultPlayerStateService.class, states);
        context.services().register(VanishService.class, vanish);
        context.services().register(DefaultVanishService.class, vanish);
        context.services().register(PlayerAbilityCommandService.class, abilities);
        context.services().register(PlayerInformationCommandService.class, information);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        context.events().listen(
                PlayerJoinEvent.class,
                event -> restoreJoinedState(context, event.player())
        );
        context.events().listen(
                PlayerDisconnectEvent.class,
                event -> disconnect(context, event.player())
        );
        context.events().listen(
                PlayerMoveEvent.class,
                event -> {
                    if (event.changedBlock()) {
                        activity(event.player());
                    }
                }
        );
        context.events().listen(
                PlayerChatEvent.class,
                event -> activity(event.player())
        );
        context.events().listen(
                PlayerCommandPreprocessEvent.class,
                event -> activity(event.player())
        );
        context.events().listen(
                PlayerAttackEvent.class,
                event -> activity(event.player())
        );
        context.events().listen(
                PlayerPickupEvent.class,
                event -> activity(event.player())
        );
        context.events().listen(
                PlayerWorldChangeEvent.class,
                event -> restorePersonalWorldState(context, event.player())
        );
        context.events().listen(
                PlayerRespawnEvent.class,
                event -> context.scheduler().syncLater(
                        () -> restorePersonalWorldState(context, event.player()),
                        1L
                )
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var players = context.services().require(PlayerDirectory.class);
        var names = context.services().require(NameCacheService.class);
        var abilityService = requireNonNull(abilities, "abilities");
        var informationService = requireNonNull(information, "information");
        var currentSettings = requireNonNull(settings, "settings");

        track(context, registry, "afk-command", new AfkCommand(abilityService, players));
        track(
                context,
                registry,
                "compass-command",
                new CompassCommand(informationService, players)
        );
        track(context, registry, "depth-command", new DepthCommand(informationService, players));
        track(context, registry, "exp-command", new ExpCommand(abilityService, players));
        track(context, registry, "feed-command", new FeedCommand(abilityService, players));
        track(context, registry, "fly-command", new FlyCommand(abilityService, players));
        track(context, registry, "gamemode-command", new GameModeCommand(abilityService, players));
        track(context, registry, "getpos-command", new GetPosCommand(informationService, players));
        track(context, registry, "god-command", new GodCommand(abilityService, players));
        track(context, registry, "heal-command", new HealCommand(abilityService, players));
        nearCommand = new NearCommand(informationService, players, currentSettings);
        track(context, registry, "near-command", nearCommand);
        track(context, registry, "nick-command", new NickCommand(abilityService, players));
        track(
                context,
                registry,
                "ping-command",
                new PingCommand(currentSettings.maximumPingLength())
        );
        track(
                context,
                registry,
                "playtime-command",
                new PlaytimeCommand(informationService, players, names)
        );
        track(context, registry, "ptime-command", new PTimeCommand(abilityService, players));
        track(context, registry, "pweather-command", new PWeatherCommand(abilityService, players));
        track(
                context,
                registry,
                "realname-command",
                new RealNameCommand(informationService, players)
        );
        track(context, registry, "rest-command", new RestCommand(abilityService, players));
        track(
                context,
                registry,
                "seen-command",
                new SeenCommand(informationService, players, names)
        );
        track(
                context,
                registry,
                "speed-command",
                new SpeedCommand(abilityService, players, currentSettings)
        );
        track(context, registry, "vanish-command", new VanishCommand(abilityService, players));
        track(
                context,
                registry,
                "whois-command",
                new WhoisCommand(informationService, players, names)
        );
        registerCommandPermissions(context.services().require(PermissionCatalog.class));
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
                        "cellulosesz.command.getpos.others",
                        "Inspect another visible player's position"
                ),
                Map.entry(
                        "cellulosesz.command.rest.others",
                        "Reset another player's rest statistic"
                ),
                Map.entry(
                        "cellulosesz.command.exp.others",
                        "Inspect another player's experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.set",
                        "Set personal experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.set.others",
                        "Set another player's experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.give",
                        "Give personal experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.give.others",
                        "Give another player experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.take",
                        "Take personal experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.take.others",
                        "Take another player's experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.reset",
                        "Reset personal experience"
                ),
                Map.entry(
                        "cellulosesz.command.exp.reset.others",
                        "Reset another player's experience"
                ),
                Map.entry(
                        "cellulosesz.playerstate.vanish.see",
                        "See vanished players"
                )
        ).forEach(catalog::register);
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        scheduleTasks(context);
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previousConfig = requireNonNull(config, "config");
        var previousSettings = requireNonNull(settings, "settings");
        var candidate = reload.configs().require("module.playerstate", PlayerStateConfig.class);
        var replacement = PlayerStateCommandSettings.from(candidate);

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    config = candidate;
                    settings = replacement;
                    requireNonNull(information, "information").configure(replacement);
                    if (nearCommand != null) {
                        nearCommand.configure(replacement);
                    }
                    lastTime.clear();
                    lastWeather.clear();
                    scheduleTasks(context);
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    config = previousConfig;
                    settings = previousSettings;
                    requireNonNull(information, "information").configure(previousSettings);
                    if (nearCommand != null) {
                        nearCommand.configure(previousSettings);
                    }
                    lastTime.clear();
                    lastWeather.clear();
                    scheduleTasks(context);
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    @Override
    public void onUnload(ModuleContext context) {
        clearRuntimeState();
    }

    @Override
    public void onServerStopping(ModuleContext context) {
        clearRuntimeState();
    }

    private void clearRuntimeState() {
        lastTime.clear();
        lastWeather.clear();
    }

    private void scheduleTasks(ModuleContext context) {
        cancelTasks();
        var current = requireNonNull(settings, "settings");
        afkTask = context.scope().own(context.scheduler().syncRepeating(
                () -> checkAutomaticAfk(context),
                20L,
                current.activityCheckTicks()
        ));

        if (current.persistPersonalTimeWeather()) {
            personalWorldTask = context.scope().own(context.scheduler().syncRepeating(
                    () -> maintainPersonalWorldState(context),
                    20L,
                    20L
            ));
        }
    }

    private void cancelTasks() {
        if (afkTask != null) {
            afkTask.close();
            afkTask = null;
        }

        if (personalWorldTask != null) {
            personalWorldTask.close();
            personalWorldTask = null;
        }
    }

    private void checkAutomaticAfk(ModuleContext context) {
        var current = requireNonNull(settings, "settings");
        if (current.autoAfkMillis() <= 0L) {
            return;
        }

        var stateService = requireNonNull(states, "states");
        var players = context.services().require(PlayerDirectory.class).onlinePlayers();
        var permissions = context.services().require(PermissionService.class);
        var connection = context.services().require(PlayerConnectionService.class);
        var audience = context.services().require(PlayerAudienceService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);

        players.forEach(player -> {
            var idle = stateService.idleMillis(player.uuid());
            if (idle < 0L) {
                stateService.activity(player.uuid(), System.currentTimeMillis());
                return;
            }

            if (!stateService.afk(player.uuid())
                    && idle >= current.autoAfkMillis()
            ) {
                stateService
                        .setAfk(
                                player.uuid(),
                                player.name(),
                                true
                        )
                        .whenComplete((result, failure) ->
                                serverThread.execute(() -> {
                                    var online = context.services()
                                            .require(PlayerDirectory.class)
                                            .onlinePlayer(player.uuid());
                                    if (failure != null) {
                                        context.logger().error(
                                                "Failed to persist automatic AFK state for "
                                                        + player.uuid(),
                                                failure
                                        );
                                    } else {
                                        online.ifPresent(target -> audience.send(
                                                target,
                                                renderer.render(
                                                        audience.locale(target),
                                                        result.message()
                                                )
                                        ));
                                    }
                                })
                        );
            }

            var kickAt = saturatedAdd(
                    current.autoAfkMillis(),
                    current.afkKickMillis()
            );
            if (current.afkKickMillis() > 0L
                    && stateService.afk(player.uuid())
                    && idle >= kickAt
                    && !permissions.has(player, "cellulosesz.playerstate.afk.kick.exempt")
            ) {
                connection.disconnect(
                        player,
                        renderer.render(
                                audience.locale(player),
                                "commands.playerstate.afk-kicked"
                        )
                );
            }
        });
    }

    private void maintainPersonalWorldState(ModuleContext context) {
        var stateService = requireNonNull(states, "states");
        context.services().require(PlayerDirectory.class).onlinePlayers()
                .forEach(player -> stateService
                        .cachedPersonalWorldState(player.uuid())
                        .ifPresent(state ->
                                applyPersonalWorldState(context, player, state)
                        )
                );
    }

    private static long saturatedAdd(long first, long second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException _) {
            return Long.MAX_VALUE;
        }
    }

    private void restoreJoinedState(ModuleContext context, CellPlayer joined) {
        context.services().require(UserService.class)
                .load(joined.uuid())
                .whenComplete((user, failure) ->
                        context.services().require(ServerThreadExecutor.class)
                                .execute(() -> {
                                    var online = context.services()
                                            .require(PlayerDirectory.class)
                                            .onlinePlayer(joined.uuid());

                                    if (failure != null) {
                                        context.logger().error(
                                                "Failed to restore player state for "
                                                        + joined.uuid(),
                                                failure
                                        );
                                        return;
                                    }

                                    if (online.isEmpty()) {
                                        return;
                                    }

                                    var player = online.orElseThrow();
                                    var statePlatform = context.services()
                                            .require(PlayerStatePlatformService.class);

                                    if (requireNonNull(settings, "settings").persistFlyGod()) {
                                        statePlatform.setFlying(player, user.state().flying());
                                        statePlatform.setInvulnerable(player, user.state().god());
                                    }

                                    if (requireNonNull(settings, "settings").persistVanish()
                                            && user.state().vanished()
                                    ) {
                                        requireNonNull(vanish, "vanish")
                                                .setVanished(player, true);
                                    }

                                    requireNonNull(states, "states")
                                            .cachedPersonalWorldState(player.uuid())
                                            .ifPresent(state ->
                                                    applyPersonalWorldState(context, player, state)
                                            );
                                    requireNonNull(vanish, "vanish")
                                            .synchronizeViewer(player);
                                    requireNonNull(states, "states").activity(
                                            player.uuid(),
                                            System.currentTimeMillis()
                                    );
                                }));
    }

    private void disconnect(ModuleContext context, CellPlayer player) {
        context.services()
                .require(VanishPlatformService.class)
                .setVanishedState(player, false);
        requireNonNull(states, "states").forgetActivity(player.uuid());

        lastTime.remove(player.uuid());
        lastWeather.remove(player.uuid());
    }

    private void activity(CellPlayer player) {
        var service = requireNonNull(states, "states");
        var wasAfk = service.afk(player.uuid());

        service.activity(
                player.uuid(),
                System.currentTimeMillis()
        );
        if (wasAfk) {
            service.setAfk(
                    player.uuid(),
                    player.name(),
                    false
            );
        }
    }

    private void restorePersonalWorldState(ModuleContext context, CellPlayer player) {
        lastTime.remove(player.uuid());
        lastWeather.remove(player.uuid());

        requireNonNull(states, "states")
                .cachedPersonalWorldState(player.uuid())
                .ifPresent(state -> applyPersonalWorldState(context, player, state));
    }

    private void applyPersonalWorldState(
            ModuleContext context,
            CellPlayer player,
            PersonalWorldState state
    ) {
        if (!requireNonNull(settings, "settings").persistPersonalTimeWeather()) {
            return;
        }

        var platform = context.services().require(PlayerStatePlatformService.class);
        if (!state.time().equals(lastTime.put(player.uuid(), state.time()))) {
            platform.setPersonalTime(player, state.time());
        }

        if (state.weather() != lastWeather.put(player.uuid(), state.weather())) {
            platform.setPersonalWeather(player, state.weather());
        }
    }

}
