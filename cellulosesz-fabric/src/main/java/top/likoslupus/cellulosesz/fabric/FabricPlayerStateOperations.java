package top.likoslupus.cellulosesz.fabric;

import net.minecraft.stats.Stats;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.playerstate.*;

import static java.util.Objects.requireNonNull;

public final class FabricPlayerStateOperations implements PlayerStatePlatformService {

    private static final int MAX_TOTAL_EXPERIENCE = Integer.MAX_VALUE;
    private final FabricPlatformService platform;

    public FabricPlayerStateOperations(FabricPlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public PlatformResult<Integer> seaLevel(CellPlayer player) {
        return onServerThread(() -> PlatformResult.success(platform.nativePlayer(player).level().getSeaLevel()));
    }

    @Override
    public PlatformResult<ExperienceSnapshot> experience(CellPlayer player) {
        return onServerThread(() -> PlatformResult.success(snapshot(platform.nativePlayer(player))));
    }

    @Override
    public PlatformResult<ExperienceSnapshot> mutateExperience(CellPlayer player, ExperienceRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var nativePlayer = platform.nativePlayer(player);
            long current = Math.max(0, nativePlayer.totalExperience);
            final long target;
            try {
                if (request.unit() == ExperienceUnit.LEVELS && request.action() != ExperienceAction.RESET) {
                    var currentLevel = Math.max(0, nativePlayer.experienceLevel);
                    var requestedLevels = Math.toIntExact(request.amount());
                    var targetLevel = switch (request.action()) {
                        case SET -> requestedLevels;
                        case GIVE -> Math.addExact(currentLevel, requestedLevels);
                        case TAKE -> Math.subtractExact(currentLevel, requestedLevels);
                        case RESET -> 0;
                    };
                    if (targetLevel < 0 || targetLevel > ExperienceMath.maximumLevel()) {
                        return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Experience level would be outside the supported range");
                    }
                    target = request.action() == ExperienceAction.SET
                            ? ExperienceMath.totalForLevel(targetLevel)
                            : totalAtLevelWithProgress(targetLevel, nativePlayer.experienceProgress);
                } else {
                    var requestedPoints = request.amount();
                    target = switch (request.action()) {
                        case RESET -> 0L;
                        case SET -> requestedPoints;
                        case GIVE -> Math.addExact(current, requestedPoints);
                        case TAKE -> Math.subtractExact(current, requestedPoints);
                    };
                }
            } catch (ArithmeticException | IllegalArgumentException failure) {
                return PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "Experience arithmetic overflow or range error");
            }
            if (target < 0L || target > MAX_TOTAL_EXPERIENCE) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Experience would be outside the supported range");
            }

            applyTotal(nativePlayer, (int) target);
            return PlatformResult.success(snapshot(nativePlayer));
        });
    }

    @Override
    public PlatformResult<Void> resetRest(CellPlayer player) {
        return onServerThread(() -> {
            platform.nativePlayer(player).resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Integer> setFireTicks(CellPlayer player, int ticks) {
        if (ticks < 0)
            return PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "Fire ticks must not be negative");
        return onServerThread(() -> {
            var target = platform.nativePlayer(player);
            target.setRemainingFireTicks(ticks);
            return PlatformResult.success(target.getRemainingFireTicks());
        });
    }

    @Override
    public PlatformResult<Void> extinguish(CellPlayer player) {
        return onServerThread(() -> {
            platform.nativePlayer(player).clearFire();
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Integer> freeze(CellPlayer player) {
        return onServerThread(() -> {
            var target = platform.nativePlayer(player);
            var ticks = target.getTicksRequiredToFreeze();
            target.setTicksFrozen(ticks);
            return PlatformResult.success(ticks);
        });
    }

    @Override
    public PlatformResult<Void> kill(CellPlayer player, KillKind kind, boolean force) {
        requireNonNull(kind, "kind");
        return onServerThread(() -> {
            var target = platform.nativePlayer(player);
            if (!target.isAlive()) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Target is already dead");
            }
            var source = target.damageSources().genericKill();
            var damaged = target.hurtServer(target.level(), source, Float.MAX_VALUE);
            if (!damaged && force && target.isAlive()) {
                target.setHealth(0.0F);
            }
            return target.isAlive()
                    ? PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Death was prevented")
                    : PlatformResult.success();
        });
    }

    private static int totalAtLevelWithProgress(int level, float progress) {
        if (!Float.isFinite(progress)) throw new IllegalArgumentException("progress must be finite");
        var normalized = Math.clamp(progress, 0.0F, Math.nextDown(1.0F));
        var withinLevel = Math.round(normalized * ExperienceMath.pointsToNextLevel(level));
        return Math.addExact(ExperienceMath.totalForLevel(level), withinLevel);
    }

    private static void applyTotal(net.minecraft.server.level.ServerPlayer player, int total) {
        var level = ExperienceMath.levelForTotal(total);
        var atLevel = ExperienceMath.totalForLevel(level);
        var needed = ExperienceMath.pointsToNextLevel(level);
        player.totalExperience = total;
        player.experienceLevel = level;
        player.experienceProgress = needed <= 0 ? 0.0F : (float) (total - atLevel) / (float) needed;
    }

    private static ExperienceSnapshot snapshot(net.minecraft.server.level.ServerPlayer player) {
        var total = Math.max(0, player.totalExperience);
        var level = Math.max(0, player.experienceLevel);
        var levelSize = Math.max(0, ExperienceMath.pointsToNextLevel(level));
        var earnedInLevel = Math.max(0, total - ExperienceMath.totalForLevel(level));
        var remaining = Math.max(0, levelSize - earnedInLevel);
        return new ExperienceSnapshot(total, level, player.experienceProgress, remaining);
    }

    private <T> PlatformResult<T> onServerThread(java.util.function.Supplier<PlatformResult<T>> operation) {
        var server = platform.requireServer();
        if (!server.isSameThread()) {
            return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Operation requires the server thread");
        }
        try {
            return operation.get();
        } catch (RuntimeException failure) {
            return PlatformResult.failure(PlatformOperationStatus.INTERNAL_ERROR, failure.getClass().getSimpleName());
        }
    }

}
