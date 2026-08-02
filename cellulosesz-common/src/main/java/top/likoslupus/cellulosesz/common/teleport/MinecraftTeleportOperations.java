package top.likoslupus.cellulosesz.common.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOperations;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public final class MinecraftTeleportOperations implements TeleportOperations {

    private final MinecraftServerHandle server;

    public MinecraftTeleportOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<Void> move(CellPlayer player, CellLocation destination) {
        var running = running();

        if (running.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.NOT_READY,
                    "server-not-running"
            );
        }

        if (!running.orElseThrow().isSameThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "teleport-must-run-on-server-thread"
            );
        }

        if (!finite(destination)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "non-finite-destination"
            );
        }

        var level = level(destination.world());
        if (level.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WORLD_NOT_FOUND,
                    "world-not-loaded"
            );
        }

        try {
            var moved = MinecraftPlayers.requireOnline(server, player).teleportTo(
                    level.orElseThrow(),
                    destination.x(), destination.y(), destination.z(),
                    Set.of(),
                    destination.yaw(), destination.pitch(),
                    true
            );
            return moved
                    ? PlatformResult.success()
                    : PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "teleport-rejected"
                    );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    @Override
    public PlatformResult<CellLocation> safeLocation(CellLocation requested) {
        if (!finite(requested)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "non-finite-destination"
            );
        }

        var level = level(requested.world());
        if (level.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WORLD_NOT_FOUND,
                    "world-not-loaded"
            );
        }

        var base = BlockPos.containing(requested.x(), requested.y(), requested.z());
        for (var offset = 0; offset <= 8; offset++) {
            var up = base.above(offset);
            if (safe(level.orElseThrow(), up)) {
                return PlatformResult.success(at(requested, up));
            }

            if (offset > 0) {
                var down = base.below(offset);
                if (safe(level.orElseThrow(), down)) {
                    return PlatformResult.success(at(requested, down));
                }
            }
        }

        return highestSafeLocation(requested);
    }

    @Override
    public PlatformResult<CellLocation> highestSafeLocation(CellLocation column) {
        if (!finite(column)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "non-finite-column"
            );
        }

        var level = level(column.world());
        if (level.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WORLD_NOT_FOUND,
                    "world-not-loaded"
            );
        }

        var top = level.orElseThrow().getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(column.x(), 0.0D, column.z())
        );

        for (var offset = 0; offset <= 8; offset++) {
            var candidate = top.above(offset);
            if (safe(level.orElseThrow(), candidate)) {
                return PlatformResult.success(at(column, candidate));
            }
        }

        return PlatformResult.failure(
                PlatformOperationStatus.UNSAFE_DESTINATION,
                "no-highest-safe-location"
        );
    }

    @Override
    public PlatformResult<CellLocation> lowestSafeLocation(CellLocation column) {
        if (!finite(column)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "non-finite-column"
            );
        }

        var level = level(column.world());
        if (level.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WORLD_NOT_FOUND,
                    "world-not-loaded"
            );
        }

        var current = level.orElseThrow();
        var min = current.getMinY();
        var max = current.getMaxY();
        var x = (int) Math.floor(column.x());
        var z = (int) Math.floor(column.z());
        for (var y = min + 1; y < max - 1; y++) {
            var candidate = new BlockPos(x, y, z);
            if (safe(current, candidate)) {
                return PlatformResult.success(at(column, candidate));
            }
        }

        return PlatformResult.failure(
                PlatformOperationStatus.UNSAFE_DESTINATION,
                "no-lowest-safe-location"
        );
    }

    @Override
    public PlatformResult<CellLocation> targetLocation(CellPlayer player, int maximumDistance) {
        if (maximumDistance <= 0) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "invalid-distance"
            );
        }

        try {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            var hit = nativePlayer.pick(maximumDistance, 0.0F, false).getLocation();
            return PlatformResult.success(new CellLocation(
                    nativePlayer.level().dimension().identifier().toString(),
                    hit.x, hit.y, hit.z,
                    nativePlayer.getYRot(),
                    nativePlayer.getXRot()
            ));
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        return !level.getBlockState(feet.below()).isAir()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir();
    }

    private static CellLocation at(CellLocation requested, BlockPos feet) {
        return new CellLocation(
                requested.world(),
                feet.getX() + 0.5D,
                feet.getY(),
                feet.getZ() + 0.5D,
                requested.yaw(),
                requested.pitch()
        );
    }

    private Optional<MinecraftServer> running() {
        return server.current();
    }

    private static boolean finite(CellLocation location) {
        return !location.world().isBlank()
                && Double.isFinite(location.x())
                && Double.isFinite(location.y())
                && Double.isFinite(location.z())
                && Float.isFinite(location.yaw())
                && Float.isFinite(location.pitch());
    }

    private Optional<ServerLevel> level(String world) {
        var current = running();
        if (current.isEmpty() || world.isBlank()) {
            return Optional.empty();
        }

        var normalized = normalize(world);
        return StreamSupport.stream(current.orElseThrow().getAllLevels().spliterator(), false)
                .filter(candidate ->
                        normalize(candidate.dimension().identifier().toString()).equals(normalized)
                )
                .findFirst();
    }

    private static String normalize(String world) {
        var value = world.trim().toLowerCase(Locale.ROOT);
        return value.contains(":")
                ? value
                : "minecraft:" + value;
    }

}
