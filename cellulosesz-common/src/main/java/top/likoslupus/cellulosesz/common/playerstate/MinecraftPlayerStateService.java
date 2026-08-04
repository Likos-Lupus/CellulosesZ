package top.likoslupus.cellulosesz.common.playerstate;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.level.GameType;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.MovementSpeedType;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.playerstate.*;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayerUnavailableException;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Minecraft-only player state adapter shared by every loader.
 */
public final class MinecraftPlayerStateService implements PlayerStatePlatformService {

    private static final int MAX_TOTAL_EXPERIENCE = Integer.MAX_VALUE;
    private final MinecraftServerHandle server;

    public MinecraftPlayerStateService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<Integer> seaLevel(CellPlayer player) {
        return onServerThread(() ->
                PlatformResult.success(MinecraftPlayers.requireOnline(server, player)
                        .level()
                        .getSeaLevel())
        );
    }

    @Override
    public PlatformResult<ExperienceSnapshot> experience(CellPlayer player) {
        return onServerThread(() ->
                PlatformResult.success(snapshot(MinecraftPlayers.requireOnline(server, player)))
        );
    }

    @Override
    public PlatformResult<ExperienceSnapshot> mutateExperience(
            CellPlayer player,
            ExperienceRequest request
    ) {
        requireNonNull(request, "request");

        return onServerThread(() -> {
            var targetPlayer = MinecraftPlayers.requireOnline(server, player);
            var current = (long) Math.max(0, targetPlayer.totalExperience);

            final long target;
            try {
                if (request.unit() == ExperienceUnit.LEVELS
                        && request.action() != ExperienceAction.RESET
                ) {
                    var currentLevel = Math.max(0, targetPlayer.experienceLevel);
                    var requestedLevels = request.amount();
                    var targetLevel = switch (request.action()) {
                        case SET -> requestedLevels;
                        case GIVE -> Math.addExact(currentLevel, requestedLevels);
                        case TAKE -> Math.subtractExact(currentLevel, requestedLevels);
                        case RESET -> 0;
                    };

                    if (targetLevel < 0 || targetLevel > ExperienceMath.maximumLevel()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.STATE_NOT_ALLOWED,
                                "Experience level is outside the supported range"
                        );
                    }

                    target = request.action() == ExperienceAction.SET
                            ? ExperienceMath.totalForLevel(targetLevel)
                            : totalAtLevelWithProgress(
                                    targetLevel,
                                    targetPlayer.experienceProgress
                            );
                } else {
                    target = switch (request.action()) {
                        case RESET -> 0L;
                        case SET -> request.amount();
                        case GIVE -> Math.addExact(current, request.amount());
                        case TAKE -> Math.subtractExact(current, request.amount());
                    };
                }
            } catch (ArithmeticException | IllegalArgumentException _) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Experience arithmetic overflow or range error"
                );
            }

            if (target < 0L || target > MAX_TOTAL_EXPERIENCE) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Experience is outside the supported range"
                );
            }

            applyTotal(targetPlayer, (int) target);
            return PlatformResult.success(snapshot(targetPlayer));
        });
    }

    @Override
    public PlatformResult<Void> resetRest(CellPlayer player) {
        return onServerThread(() -> {
            MinecraftPlayers
                    .requireOnline(server, player)
                    .resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Boolean> flying(CellPlayer player) {
        return onServerThread(() ->
                PlatformResult.success(MinecraftPlayers
                        .requireOnline(server, player)
                        .getAbilities().flying)
        );
    }

    @Override
    public PlatformResult<BooleanStateChange> setFlying(CellPlayer player, boolean enabled) {
        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            var abilities = target.getAbilities();
            var previous = abilities.flying;

            abilities.mayfly = enabled || target.isCreative() || target.isSpectator();
            abilities.flying = enabled;

            target.onUpdateAbilities();
            return PlatformResult.success(new BooleanStateChange(previous, abilities.flying));
        });
    }

    @Override
    public PlatformResult<Boolean> invulnerable(CellPlayer player) {
        return onServerThread(() ->
                PlatformResult.success(
                        MinecraftPlayers
                                .requireOnline(server, player)
                                .getAbilities().invulnerable
                )
        );
    }

    @Override
    public PlatformResult<BooleanStateChange> setInvulnerable(CellPlayer player, boolean enabled) {
        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            var abilities = target.getAbilities();
            var previous = abilities.invulnerable;

            abilities.invulnerable = enabled;

            target.onUpdateAbilities();
            return PlatformResult.success(new BooleanStateChange(previous, abilities.invulnerable));
        });
    }

    @Override
    public PlatformResult<Void> heal(CellPlayer player) {
        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            target.setHealth(target.getMaxHealth());
            target.clearFire();
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Void> feed(CellPlayer player) {
        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            target.getFoodData().setFoodLevel(20);
            target.getFoodData().setSaturation(20.0F);
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<GameModeKind> gameMode(CellPlayer player) {
        return onServerThread(() ->
                PlatformResult.success(fromMinecraft(
                        MinecraftPlayers
                                .requireOnline(server, player)
                                .gameMode()
                ))
        );
    }

    @Override
    public PlatformResult<GameModeChange> setGameMode(CellPlayer player, GameModeKind mode) {
        requireNonNull(mode, "mode");
        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            var previous = fromMinecraft(target.gameMode());

            if (!target.setGameMode(toMinecraft(mode))) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Game mode change was rejected"
                );
            }

            return PlatformResult.success(new GameModeChange(
                    previous,
                    fromMinecraft(target.gameMode())
            ));
        });
    }

    @Override
    public PlatformResult<MovementSpeedChange> setMovementSpeed(
            CellPlayer player, MovementSpeedType type,
            double speed
    ) {
        requireNonNull(type, "type");
        if (!Double.isFinite(speed) || speed < 0.0001D || speed > 10.0D) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Movement speed must be finite and within range"
            );
        }

        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            var abilities = target.getAbilities();
            var previousNative = type == MovementSpeedType.FLY
                    ? abilities.getFlyingSpeed()
                    : abilities.getWalkingSpeed();
            var previous = denormalize(type, previousNative);
            var normalized = normalize(type, speed);

            if (type == MovementSpeedType.FLY) {
                abilities.setFlyingSpeed((float) normalized);
            } else {
                abilities.setWalkingSpeed((float) normalized);
            }

            target.onUpdateAbilities();
            return PlatformResult.success(new MovementSpeedChange(type, previous, speed));
        });
    }

    @Override
    public PlatformResult<PersonalTimeSetting> setPersonalTime(
            CellPlayer player,
            PersonalTimeSetting setting
    ) {
        requireNonNull(setting, "setting");

        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            var level = target.level();
            var currentTime = level.getDefaultClockTime();
            var applied = (PersonalTimeSetting) switch (setting) {
                case PersonalTimeSetting.Fixed fixed ->
                        new PersonalTimeSetting.Fixed(Math.floorMod(fixed.ticks(), 24_000L));
                case PersonalTimeSetting.Relative relative ->
                        new PersonalTimeSetting.Fixed(Math.floorMod(
                                currentTime + relative.offset(),
                                24_000L
                        ));
                case PersonalTimeSetting.Reset _ -> new PersonalTimeSetting.Reset();
            };

            if (applied instanceof PersonalTimeSetting.Fixed(long ticks)) {
                var clock = level.dimensionType().defaultClock();
                if (clock.isEmpty()) {
                    return PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Player world does not define a default clock"
                    );
                }
                target.connection.send(new ClientboundSetTimePacket(
                        level.getGameTime(),
                        Map.of(
                                clock.orElseThrow(),
                                new ClockNetworkState(ticks, 0.0F, 0.0F)
                        )
                ));
            } else {
                target.connection.send(
                        server.requireRunning()
                                .clockManager()
                                .createFullSyncPacket()
                );
            }

            return PlatformResult.success(applied);
        });
    }

    @Override
    public PlatformResult<PersonalWeatherSetting> setPersonalWeather(
            CellPlayer player,
            PersonalWeatherSetting setting
    ) {
        requireNonNull(setting, "setting");

        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            switch (setting) {
                case RESET -> restoreWorldWeather(target);

                case CLEAR -> {
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.STOP_RAINING,
                            0.0F
                    ));
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                            0.0F
                    ));
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                            0.0F
                    ));
                }

                case RAIN -> {
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.START_RAINING,
                            0.0F
                    ));
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                            1.0F
                    ));
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                            0.0F
                    ));
                }

                case THUNDER -> {
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.START_RAINING,
                            0.0F
                    ));
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                            1.0F
                    ));
                    target.connection.send(new ClientboundGameEventPacket(
                            ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                            1.0F
                    ));
                }
            }

            return PlatformResult.success(setting);
        });
    }

    @Override
    public PlatformResult<Integer> setFireTicks(CellPlayer player, int ticks) {
        if (ticks < 0) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Fire ticks must not be negative"
            );
        }

        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            target.setRemainingFireTicks(ticks);
            return PlatformResult.success(target.getRemainingFireTicks());
        });
    }

    @Override
    public PlatformResult<Void> extinguish(CellPlayer player) {
        return onServerThread(() -> {
            MinecraftPlayers.requireOnline(server, player).clearFire();
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Integer> freeze(CellPlayer player) {
        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            var ticks = target.getTicksRequiredToFreeze();

            target.setTicksFrozen(ticks);
            return PlatformResult.success(ticks);
        });
    }

    @Override
    public PlatformResult<Void> kill(CellPlayer player, KillKind kind, boolean force) {
        requireNonNull(kind, "kind");

        return onServerThread(() -> {
            var target = MinecraftPlayers.requireOnline(server, player);
            if (!target.isAlive()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Target is already dead"
                );
            }

            var damaged = target.hurtServer(
                    target.level(),
                    target.damageSources().genericKill(),
                    Float.MAX_VALUE
            );
            if (!damaged && force && target.isAlive()) {
                target.setHealth(0.0F);
            }

            return target.isAlive()
                    ?
                    PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Death was prevented"
                    )
                    : PlatformResult.success();
        });
    }

    private static void restoreWorldWeather(ServerPlayer target) {
        var level = target.level();
        if (level.isRaining()) {
            target.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.START_RAINING,
                    0.0F
            ));
            target.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    level.getRainLevel(1.0F)
            ));
            target.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                    level.getThunderLevel(1.0F)
            ));
        } else {
            target.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.STOP_RAINING,
                    0.0F
            ));
        }
    }

    private static double denormalize(MovementSpeedType type, double nativeSpeed) {
        var base = type == MovementSpeedType.FLY
                ? 0.1D
                : 0.2D;
        return nativeSpeed <= base
                ? nativeSpeed / base
                : 1.0D + ((nativeSpeed - base) / (1.0D - base)) * 9.0D;
    }

    private static double normalize(MovementSpeedType type, double speed) {
        var base = type == MovementSpeedType.FLY
                ? 0.1D
                : 0.2D;
        return speed < 1.0D
                ? base * speed
                : base + ((speed - 1.0D) / 9.0D) * (1.0D - base);
    }

    private static GameType toMinecraft(GameModeKind type) {
        return switch (type) {
            case SURVIVAL -> GameType.SURVIVAL;
            case CREATIVE -> GameType.CREATIVE;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SPECTATOR;
        };
    }

    private static GameModeKind fromMinecraft(GameType type) {
        return switch (type) {
            case SURVIVAL -> GameModeKind.SURVIVAL;
            case CREATIVE -> GameModeKind.CREATIVE;
            case ADVENTURE -> GameModeKind.ADVENTURE;
            case SPECTATOR -> GameModeKind.SPECTATOR;
        };
    }

    private static int totalAtLevelWithProgress(int level, float progress) {
        if (!Float.isFinite(progress)) {
            throw new IllegalArgumentException("progress must be finite");
        }

        var normalized = Math.clamp(progress, 0.0F, Math.nextDown(1.0F));
        return Math.addExact(
                ExperienceMath.totalForLevel(level),
                Math.round(normalized * ExperienceMath.pointsToNextLevel(level))
        );
    }

    private static void applyTotal(ServerPlayer player, int total) {
        var level = ExperienceMath.levelForTotal(total);
        var atLevel = ExperienceMath.totalForLevel(level);
        var needed = ExperienceMath.pointsToNextLevel(level);

        player.totalExperience = total;
        player.experienceLevel = level;
        player.experienceProgress = needed <= 0
                ? 0.0F
                : (float) (total - atLevel) / (float) needed;
    }

    private static ExperienceSnapshot snapshot(ServerPlayer player) {
        var total = Math.max(0, player.totalExperience);
        var level = Math.max(0, player.experienceLevel);
        var levelSize = Math.max(0, ExperienceMath.pointsToNextLevel(level));
        var earned = Math.max(0, total - ExperienceMath.totalForLevel(level));
        return new ExperienceSnapshot(
                total,
                level,
                player.experienceProgress,
                Math.max(0, levelSize - earned)
        );
    }

    private <T> PlatformResult<T> onServerThread(Supplier<PlatformResult<T>> operation) {
        var current = server.current();
        if (current.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.NOT_READY,
                    "Minecraft server is not active"
            );
        }
        if (!current.orElseThrow().isSameThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return operation.get();
        } catch (MinecraftPlayerUnavailableException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    failure.getMessage()
            );
        } catch (IllegalStateException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_STATE,
                    failure.getMessage() == null
                            ? failure.getClass().getSimpleName()
                            : failure.getMessage()
            );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

}
