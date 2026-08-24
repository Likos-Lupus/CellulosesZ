package top.likoslupus.cellulosesz.modules.playerstate.service;


import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.playerstate.*;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class DefaultPlayerStateService implements PlayerStateService {

    private final PlayerStatePlatformService platform;
    private final ServerThreadExecutor serverThread;
    private final PlayerDirectory players;
    private final UserService users;
    private final DisplayNameService displayNames;
    private final ConcurrentHashMap<UUID, Long> lastActivityNanos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastActivityMillis = new ConcurrentHashMap<>();

    public DefaultPlayerStateService(
            PlayerStatePlatformService platform,
            ServerThreadExecutor serverThread,
            PlayerDirectory players,
            UserService users,
            DisplayNameService displayNames
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.players = requireNonNull(players, "players");
        this.users = requireNonNull(users, "users");
        this.displayNames = requireNonNull(displayNames, "displayNames");
    }

    @Override
    public CompletableFuture<PlayerStateResult> setFlying(CellPlayer player, boolean enabled) {
        return changePersistentBoolean(player, enabled, true);
    }

    @Override
    public CompletableFuture<PlayerStateResult> setGod(CellPlayer player, boolean enabled) {
        return changePersistentBoolean(player, enabled, false);
    }

    @Override
    public PlayerStateResult heal(CellPlayer player) {
        var result = platform.heal(player);
        return result.successful()
                ?
                PlayerStateResult.success(
                        "service.playerstate.heal-success",
                        MessageArguments.builder()
                                .add(displayNames.plainDisplayName(player))
                                .build()
                )
                : PlayerStateResult.failure(
                        "service.playerstate.heal-failed",
                        MessageArguments.builder()
                                .add(displayNames.plainDisplayName(player))
                                .build()
                );
    }

    @Override
    public PlayerStateResult feed(CellPlayer player) {
        var result = platform.feed(player);
        return result.successful()
                ?
                PlayerStateResult.success(
                        "service.playerstate.feed-success",
                        MessageArguments.builder()
                                .add(displayNames.plainDisplayName(player))
                                .build()
                )
                : PlayerStateResult.failure(
                        "service.playerstate.feed-failed",
                        MessageArguments.builder()
                                .add(displayNames.plainDisplayName(player))
                                .build()
                );
    }

    @Override
    public CompletableFuture<PlayerStateResult> setAfk(
            UUID uuid,
            String name,
            boolean afk
    ) {
        var now = System.currentTimeMillis();
        return users
                .updateVoid(
                        uuid,
                        user -> user
                                .withState(user.state().withAfk(afk))
                                .withTimestamps(user.timestamps().withLastActivityAt(now))
                )
                .thenApply(_ -> {
                    activity(uuid, now);
                    return PlayerStateResult.success(
                            afk
                                    ? "service.playerstate.afk-enabled"
                                    : "service.playerstate.afk-disabled",
                            MessageArguments.builder().add(name).build()
                    );
                })
                .exceptionally(_ -> PlayerStateResult.failure("service.user.persistence-failed"));
    }

    @Override
    public boolean afk(UUID uuid) {
        var user = users.cached(uuid);
        return user != null && user.state().afk();
    }

    @Override
    public void activity(UUID uuid, long timestamp) {
        lastActivityNanos.put(uuid, System.nanoTime());
        lastActivityMillis.merge(uuid, timestamp, Math::max);
    }

    @Override
    public long lastActivity(UUID uuid) {
        var cachedUser = users.cached(uuid);
        return lastActivityMillis
                .getOrDefault(
                        uuid,
                        cachedUser == null
                                ? 0L
                                : cachedUser.timestamps().lastActivityAt()
                );
    }

    @Override
    public long idleMillis(UUID uuid) {
        var started = lastActivityNanos.get(uuid);
        return started == null
                ? -1L
                : Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }

    @Override
    public CompletableFuture<PersonalWorldState> loadPersonalWorldState(UUID uuid) {
        return users
                .load(uuid)
                .thenApply(user -> personalWorldState(
                        user.state().personalTime(),
                        user.state().personalWeather()
                ));
    }

    @Override
    public PersonalWorldState cachedPersonalWorldState(UUID uuid) {
        var user = users.cached(uuid);
        return user == null
                ? null
                : personalWorldState(
                        user.state().personalTime(),
                        user.state().personalWeather()
                );
    }

    @Override
    public CompletableFuture<PlayerStateResult> setPersonalTime(
            CellPlayer player,
            PersonalTimeSetting setting
    ) {
        requireNonNull(setting, "setting");

        return loadPersonalWorldState(player.uuid())
                .thenCompose(previousState -> serverThread
                        .submit(() -> platform.setPersonalTime(player, setting))
                        .thenCompose(applied -> {
                            if (!applied.successful()) {
                                return CompletableFuture.completedFuture(PlayerStateResult.failure(
                                        "commands.playerstate.ptime-failed"
                                ));
                            }

                            return users
                                    .updateVoid(
                                            player.uuid(),
                                            user -> user.withState(
                                                    user.state()
                                                            .withPersonalTime(persistedTime(setting))
                                            )
                                    )
                                    .thenApply(_ -> timeSuccess(setting))
                                    .exceptionallyCompose(_ -> serverThread
                                            .submit(() -> platform.setPersonalTime(
                                                    player,
                                                    previousState.time()
                                            ))
                                            .thenApply(rollback ->
                                                    rollback.successful()
                                                            ?
                                                            PlayerStateResult.failure(
                                                                    "service.user.persistence-failed"
                                                            )
                                                            : PlayerStateResult.failure(
                                                                    "commands.playerstate.ptime-rollback-failed"
                                                            )
                                            )
                                    );
                        })
                )
                .exceptionally(_ -> PlayerStateResult.failure("service.user.persistence-failed"));
    }

    @Override
    public CompletableFuture<PlayerStateResult> setPersonalWeather(
            CellPlayer player,
            PersonalWeatherSetting setting
    ) {
        requireNonNull(setting, "setting");

        return loadPersonalWorldState(player.uuid())
                .thenCompose(previousState -> serverThread
                        .submit(() -> platform.setPersonalWeather(player, setting))
                        .thenCompose(applied -> {
                            if (!applied.successful()) {
                                return CompletableFuture.completedFuture(PlayerStateResult.failure(
                                        "commands.playerstate.pweather-failed"
                                ));
                            }

                            return users
                                    .updateVoid(
                                            player.uuid(),
                                            user -> user.withState(
                                                    user.state()
                                                            .withPersonalWeather(persistedWeather(
                                                                    setting
                                                            ))
                                            )
                                    )
                                    .thenApply(_ -> weatherSuccess(setting))
                                    .exceptionallyCompose(_ -> serverThread
                                            .submit(() -> platform.setPersonalWeather(
                                                    player,
                                                    previousState.weather()
                                            ))
                                            .thenApply(rollback ->
                                                    rollback.successful()
                                                            ?
                                                            PlayerStateResult.failure(
                                                                    "service.user.persistence-failed"
                                                            )
                                                            : PlayerStateResult.failure(
                                                                    "commands.playerstate.pweather-rollback-failed"
                                                            )
                                            )
                                    );
                        })
                )
                .exceptionally(_ -> PlayerStateResult.failure("service.user.persistence-failed"));
    }

    private static @Nullable String persistedWeather(PersonalWeatherSetting setting) {
        return setting == PersonalWeatherSetting.RESET
                ? null
                : setting.name().toLowerCase(Locale.ROOT);
    }

    private static PlayerStateResult weatherSuccess(
            PersonalWeatherSetting setting
    ) {
        return setting == PersonalWeatherSetting.RESET
                ?
                PlayerStateResult.success(
                        "commands.playerstate.pweather-reset",
                        MessageArguments.empty()
                )
                : PlayerStateResult.success(
                        "commands.playerstate.pweather-set",
                        MessageArguments.empty()
                );
    }

    @Override
    public CompletableFuture<PlayerStateResult> setNick(
            UUID uuid,
            String name,
            @Nullable String nickname
    ) {
        var online = players.onlinePlayer(uuid);
        var normalized = nickname == null || nickname.isBlank()
                ? null
                : nickname;

        if (online != null && normalized != null) {
            var sanitized = displayNames.sanitizeNickname(online, normalized);
            if (!displayNames.validNickname(online, sanitized)) {
                return CompletableFuture.completedFuture(PlayerStateResult.failure(
                        "player.nick-invalid"));
            }

            normalized = sanitized;
        }

        final String stored = normalized;
        return users
                .updateVoid(
                        uuid,
                        user -> user.withState(user.state().withNickname(stored))
                )
                .thenCompose(_ -> serverThread
                        .submit(() -> {
                            if (online != null) {
                                displayNames.refresh(online);
                            }
                            return stored == null
                                    ? PlayerStateResult.success("player.nick-cleared")
                                    : PlayerStateResult.success(
                                            "player.nick-set",
                                            MessageArguments.builder().add(stored).build()
                                    );
                        })
                )
                .exceptionally(_ -> PlayerStateResult.failure("service.user.persistence-failed"));
    }

    @Override
    public String nick(UUID uuid) {
        var user = users.cached(uuid);
        return user == null
                ? null
                : user.state().nickname();
    }

    private static @Nullable Long persistedTime(PersonalTimeSetting setting) {
        return switch (setting) {
            case PersonalTimeSetting.Fixed fixed -> Math.floorMod(fixed.ticks(), 24_000L);
            case PersonalTimeSetting.Relative relative -> Math.floorMod(relative.offset(), 24_000L);
            case PersonalTimeSetting.Reset _ -> null;
        };
    }

    private static PlayerStateResult timeSuccess(PersonalTimeSetting setting) {
        if (setting instanceof PersonalTimeSetting.Reset) {
            return PlayerStateResult.success(
                    "commands.playerstate.ptime-reset",
                    MessageArguments.empty()
            );
        }

        return PlayerStateResult.success(
                "commands.playerstate.ptime-set",
                MessageArguments.empty()
        );
    }

    private static PersonalWorldState personalWorldState(
            @Nullable Long rawTime,
            @Nullable String rawWeather
    ) {
        var time = rawTime == null
                ? PersonalTimeSetting.reset()
                : new PersonalTimeSetting.Fixed(rawTime);
        return new PersonalWorldState(time, parseWeather(rawWeather));
    }

    private static PersonalWeatherSetting parseWeather(@Nullable String raw) {
        if (raw == null) {
            return PersonalWeatherSetting.RESET;
        }

        try {
            return PersonalWeatherSetting.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            return PersonalWeatherSetting.RESET;
        }
    }

    private CompletableFuture<PlayerStateResult> changePersistentBoolean(
            CellPlayer player,
            boolean enabled,
            boolean flying
    ) {
        return serverThread
                .submit(() ->
                        flying
                                ? platform.flying(player)
                                : platform.invulnerable(player)
                )
                .thenCompose(current -> {
                    var currentValue = current.value();
                    if (!current.successful()
                            || currentValue == null
                    ) {
                        return CompletableFuture.completedFuture(PlayerStateResult.failure(
                                flying
                                        ? "service.playerstate.fly-failed"
                                        : "service.playerstate.god-failed"
                        ));
                    }

                    var previous = (boolean) currentValue;
                    return serverThread
                            .submit(() ->
                                    flying
                                            ? platform.setFlying(player, enabled)
                                            : platform.setInvulnerable(player, enabled)
                            )
                            .thenCompose(applied -> {
                                if (!applied.successful()) {
                                    return CompletableFuture.completedFuture(PlayerStateResult.failure(
                                            flying
                                                    ? "service.playerstate.fly-failed"
                                                    : "service.playerstate.god-failed"
                                    ));
                                }

                                return users
                                        .updateVoid(
                                                player.uuid(),
                                                user -> user.withState(
                                                        flying
                                                                ? user.state().withFlying(enabled)
                                                                : user.state().withGod(enabled)
                                                )
                                        ).thenApply(_ -> PlayerStateResult.success(
                                                flying
                                                        ?
                                                        enabled
                                                                ? "service.playerstate.fly-enabled"
                                                                : "service.playerstate.fly-disabled"

                                                        : enabled
                                                                ? "service.playerstate.god-enabled"
                                                                : "service.playerstate.god-disabled",
                                                MessageArguments.builder()
                                                        .add(
                                                                displayNames.plainDisplayName(player)
                                                        )
                                                        .build()
                                        ))
                                        .exceptionallyCompose(_ -> serverThread
                                                .submit(() ->
                                                        flying
                                                                ?
                                                                platform.setFlying(
                                                                        player,
                                                                        previous
                                                                )
                                                                : platform.setInvulnerable(
                                                                        player,
                                                                        previous
                                                                )
                                                )
                                                .thenApply(rollback ->
                                                        rollback.successful()
                                                                ?
                                                                PlayerStateResult.failure(
                                                                        "service.user.persistence-failed"
                                                                )
                                                                : PlayerStateResult.failure(
                                                                        "service.user.rollback-failed"
                                                                )
                                                )
                                        );
                            });
                });
    }

    public void forgetActivity(UUID uuid) {
        lastActivityNanos.remove(uuid);
        lastActivityMillis.remove(uuid);
    }

}
