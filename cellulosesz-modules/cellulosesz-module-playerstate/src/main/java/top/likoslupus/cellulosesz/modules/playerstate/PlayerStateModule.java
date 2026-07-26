package top.likoslupus.cellulosesz.modules.playerstate;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.*;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.playerstate.command.*;
import top.likoslupus.cellulosesz.modules.playerstate.config.PlayerStateConfig;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultPlayerStateService;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultVanishService;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "playerstate",
        name = "PlayerState",
        description = "Persistent player state, AFK automation, player lookup, and per-player world settings.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "permission", "command"}
)
public final class PlayerStateModule implements CellulosesZModule {

    private @Nullable PlayerStateConfig config;
    private @Nullable PlayerStateService states;
    private @Nullable VanishService vanish;
    private @Nullable NearCommand nearCommand;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.playerstate",
                PlayerStateConfig.class,
                "modules/playerstate.yml",
                PlayerStateConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var permissions = context.services().require(PermissionService.class);
        var displayNames = context.services().require(DisplayNameService.class);

        states = new DefaultPlayerStateService(platform, users, displayNames);
        vanish = new DefaultVanishService(platform, users, permissions, displayNames);

        context.services().register(PlayerStateService.class, states);
        context.services().register(DefaultPlayerStateService.class, (DefaultPlayerStateService) states);
        context.services().register(VanishService.class, vanish);
        context.services().register(DefaultVanishService.class, (DefaultVanishService) vanish);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        context.events().listen(
                PlayerJoinEvent.class,
                event -> restoreJoinedState(context, event.player(), 0)
        );
        context.events().listen(
                PlayerDisconnectEvent.class,
                event -> {
                    context.services().require(PlatformService.class)
                            .setVanishedState(event.player(), false);
                    context.services().require(DefaultPlayerStateService.class)
                            .forgetActivity(event.player().uuid());
                }
        );
        context.events().listen(
                PlayerMoveEvent.class,
                event -> {
                    if (event.changedBlock()) {
                        activity(context, event.player());
                    }
                });
        context.events().listen(
                PlayerChatEvent.class,
                event -> activity(context, event.player())
        );
        context.events().listen(
                PlayerCommandPreprocessEvent.class,
                event -> activity(context, event.player())
        );
        context.events().listen(
                PlayerAttackEvent.class,
                event -> activity(context, event.player())
        );
        context.events().listen(
                PlayerPickupEvent.class,
                event -> activity(context, event.player())
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
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var displayNames = context.services().require(DisplayNameService.class);
        var currentConfig = requireNonNull(config, "PlayerStateConfig has not been initialized");
        var currentStates = requireNonNull(states, "PlayerStateService has not been initialized");
        var currentVanish = requireNonNull(vanish, "VanishService has not been initialized");

        context.commands().register(new FlyCommand(platform, users, currentStates));
        context.commands().register(new GodCommand(platform, users, currentStates));
        context.commands().register(new HealCommand(platform, users, currentStates));
        context.commands().register(new FeedCommand(platform, users, currentStates));
        context.commands().register(new AfkCommand(platform, users, currentStates));
        context.commands().register(new NickCommand(platform, users, currentStates));
        context.commands().register(new VanishCommand(
                platform,
                users,
                currentStates,
                currentVanish,
                context.services().require(MessageRenderer.class),
                context.services().require(LocaleResolver.class)
        ));
        context.commands().register(new SeenCommand(platform, users, currentVanish));
        context.commands().register(new WhoisCommand(platform, users, currentVanish));
        context.commands().register(new PlaytimeCommand(platform, users));
        nearCommand = new NearCommand(platform, currentVanish, displayNames, currentConfig);
        context.commands().register(nearCommand);
        context.commands().register(new GameModeCommand(platform));
        context.commands().register(new SpeedCommand(platform));
        context.commands().register(new PTimeCommand(platform, users));
        context.commands().register(new PWeatherCommand(platform, users));
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        context.scheduler()
                .syncRepeating(() -> checkAutomaticAfk(context), 20L, 20L);
        context.scheduler()
                .syncRepeating(() -> maintainPersonalWorldState(context), 20L, 20L);
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.playerstate", PlayerStateConfig.class);
        if (nearCommand != null) {
            nearCommand.configure(requireNonNull(config, "PlayerStateConfig has not been initialized"));
        }
    }

    private void checkAutomaticAfk(ModuleContext context) {
        var currentConfig = requireNonNull(config, "PlayerStateConfig has not been initialized");
        if (currentConfig.autoAfkSeconds <= 0L) return;

        final long autoAfkMillis;
        final long kickMillis;
        try {
            autoAfkMillis = Math.multiplyExact(currentConfig.autoAfkSeconds, 1000L);
            kickMillis = currentConfig.afkKickSeconds <= 0L
                    ? Long.MAX_VALUE
                    : Math.addExact(autoAfkMillis, Math.multiplyExact(currentConfig.afkKickSeconds, 1000L));
        } catch (ArithmeticException failure) {
            context.logger().error("AFK time configuration overflows milliseconds", failure);
            return;
        }

        var platform = context.services().require(PlatformService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var currentStates = requireNonNull(states, "PlayerStateService has not been initialized");
        platform.onlinePlayers().forEach(player -> {
            var idle = currentStates.idleMillis(player.uuid());
            if (idle < 0L) {
                currentStates.activity(player.uuid(), System.currentTimeMillis());
                return;
            }

            if (!currentStates.afk(player.uuid()) && idle >= autoAfkMillis) {
                currentStates.setAfk(player.uuid(), player.name(), true).whenComplete((result, failure) -> {
                    if (failure != null) {
                        context.logger().error("Failed to persist automatic AFK state for " + player.uuid(), failure);
                        return;
                    }
                    platform.runOnServerThread(() -> platform.sendMessage(
                            player,
                            renderer.render(platform.locale(player), result.message().key(), result.message()
                                    .placeholders())
                    ));
                });
            }

            if (currentStates.afk(player.uuid())
                    && idle >= kickMillis
                    && !context.services().require(PermissionService.class)
                    .has(player.nativeHandle(), "cellulosesz.playerstate.afk.kick.exempt")) {
                platform.kick(player, renderer.render(platform.locale(player), "commands.playerstate.afk-kicked")
                        .plainText());
            }
        });
    }

    private void maintainPersonalWorldState(ModuleContext context) {
        var currentConfig = requireNonNull(config, "PlayerStateConfig has not been initialized");
        if (!currentConfig.persistPersonalTimeWeather) return;

        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        platform.onlinePlayers().forEach(player ->
                users.cached(player.uuid()).ifPresent(user -> {
                    if (user.state.personalTime != null) {
                        platform.setPersonalTime(player, user.state.personalTime);
                    }
                    if (user.state.personalWeather != null) {
                        platform.setPersonalWeather(player, user.state.personalWeather);
                    }
                })
        );
    }

    private void activity(ModuleContext context, CellPlayer player) {
        var currentStates = requireNonNull(states, "PlayerStateService has not been initialized");
        currentStates.activity(player.uuid(), System.currentTimeMillis());
        if (!currentStates.afk(player.uuid())) return;

        currentStates.setAfk(player.uuid(), player.name(), false).whenComplete((result, failure) -> {
            if (failure != null) {
                context.logger().error("Failed to persist AFK reset for " + player.uuid(), failure);
                return;
            }
            var platform = context.services().require(PlatformService.class);
            platform.runOnServerThread(() -> platform.sendMessage(
                    player,
                    context.services().require(MessageRenderer.class).render(
                            platform.locale(player), result.message().key(), result.message().placeholders()
                    )
            ));
        });
    }

    private void restoreJoinedState(
            ModuleContext context,
            CellPlayer player,
            int attempt
    ) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        if (platform.onlinePlayers().stream()
                .noneMatch(online -> online.uuid().equals(player.uuid()))
        ) return;

        var loaded = users.cached(player.uuid());
        if (loaded.isEmpty()) {
            if (attempt < 100) {
                context.scheduler().syncLater(
                        () -> restoreJoinedState(context, player, attempt + 1),
                        1L
                );
            } else {
                context.logger().warn("Timed out waiting for player data before restoring state: " + player.name());
            }
            return;
        }

        var currentConfig = requireNonNull(config, "PlayerStateConfig has not been initialized");
        var currentStates = requireNonNull(states, "PlayerStateService has not been initialized");
        var currentVanish = requireNonNull(vanish, "VanishService has not been initialized");
        var user = loaded.get();

        context.services().require(DisplayNameService.class).refresh(player);
        currentStates.activity(player.uuid(), System.currentTimeMillis());

        if (currentConfig.persistFlyGod) {
            if (user.state.flying) currentStates.setFlying(player, true);
            if (user.state.god) currentStates.setGod(player, true);
        }

        if ((!currentConfig.persistAfk || !user.state.afk) && user.state.afk) {
            currentStates.setAfk(player.uuid(), player.name(), false);
        }

        if (user.state.vanished && currentConfig.persistVanish) {
            currentVanish.setVanished(player, true);
        } else {
            if (user.state.vanished) {
                user.state.vanished = false;
                users.markDirty(player.uuid());
                users.save(player.uuid());
            }
            platform.setVanishedState(player, false);
        }

        restorePersonalWorldState(context, player);
        currentVanish.synchronizeViewer(player);
    }

    private void restorePersonalWorldState(ModuleContext context, CellPlayer player) {
        var currentConfig = requireNonNull(config, "PlayerStateConfig has not been initialized");
        if (!currentConfig.persistPersonalTimeWeather) return;

        context.services().require(UserService.class).cached(player.uuid()).ifPresent(user -> {
            var platform = context.services().require(PlatformService.class);
            platform.setPersonalTime(player, user.state.personalTime);
            platform.setPersonalWeather(player, user.state.personalWeather);
        });
    }

}
