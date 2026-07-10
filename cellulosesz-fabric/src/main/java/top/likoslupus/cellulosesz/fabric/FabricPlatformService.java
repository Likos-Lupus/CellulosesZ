package top.likoslupus.cellulosesz.fabric;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

public final class FabricPlatformService implements PlatformService {

    private @Nullable MinecraftServer server;

    public void server(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public Optional<CellPlayer> player(CommandInvocation invocation) {
        if (invocation.nativeSource() instanceof CommandSourceStack source) {
            return player(source.getEntity());
        }
        return Optional.empty();
    }

    @Override
    public Optional<CellPlayer> player(Object nativeHandle) {
        if (nativeHandle instanceof ServerPlayer player) {
            return Optional.of(wrap(player));
        }
        return Optional.empty();
    }

    @Override
    public Optional<CellPlayer> onlinePlayer(String name) {
        if (server == null || name.isBlank()) return Optional.empty();

        var exact = server.getPlayerList().getPlayerByName(name);
        if (exact != null) return Optional.of(wrap(exact));

        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.getGameProfile().name().equalsIgnoreCase(name))
                .findFirst()
                .map(this::wrap);
    }

    @Override
    public List<CellPlayer> onlinePlayers() {
        if (server == null) return List.of();
        return server.getPlayerList().getPlayers().stream()
                .map(this::wrap)
                .toList();
    }

    @Override
    public List<String> worlds() {
        if (server == null) return List.of();
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .map(level -> level.dimension().identifier().toString())
                .toList();
    }

    @Override
    public String defaultWorld() {
        if (server == null) return "minecraft:overworld";
        return Level.OVERWORLD.identifier().toString();
    }

    @Override
    public CellLocation location(CellPlayer player) {
        var nativePlayer = requireNative(player);
        return new CellLocation(
                nativePlayer.level().dimension().identifier().toString(),
                nativePlayer.getX(),
                nativePlayer.getY(),
                nativePlayer.getZ(),
                nativePlayer.getYRot(),
                nativePlayer.getXRot()
        );
    }

    @Override
    public CompletableFuture<Boolean> teleport(CellPlayer player, CellLocation location) {
        return CompletableFuture.completedFuture(teleportNow(player, location));
    }

    @Override
    public Optional<CellLocation> safeLocation(CellLocation location) {
        var level = level(location.world);
        if (level.isEmpty()) return Optional.empty();

        var base = BlockPos.containing(location.x, location.y, location.z);
        for (int dy = 0; dy <= 8; dy++) {
            var up = base.above(dy);
            if (safe(level.get(), up)) {
                return Optional.of(new CellLocation(
                        location.world,
                        up.getX() + 0.5D,
                        up.getY(),
                        up.getZ() + 0.5D,
                        location.yaw,
                        location.pitch
                ));
            }
        }
        for (int dy = 1; dy <= 8; dy++) {
            var down = base.below(dy);
            if (safe(level.get(), down)) {
                return Optional.of(new CellLocation(
                        location.world,
                        down.getX() + 0.5D,
                        down.getY(),
                        down.getZ() + 0.5D,
                        location.yaw,
                        location.pitch
                ));
            }
        }

        return highestLocation(location.world, location.x, location.z)
                .map(found -> new CellLocation(
                        found.world,
                        found.x,
                        found.y,
                        found.z,
                        location.yaw,
                        location.pitch
                ));
    }

    @Override
    public Optional<CellLocation> highestLocation(
            String world,
            double x,
            double z
    ) {
        return level(world).map(level -> {
            var top = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(x, 0.0D, z)
            );
            return new CellLocation(
                    world,
                    top.getX() + 0.5D,
                    top.getY(),
                    top.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
        });
    }

    @Override
    public Optional<CellLocation> targetLocation(CellPlayer player, int maxDistance) {
        var nativePlayer = requireNative(player);
        var block = nativePlayer.pick(maxDistance, 0.0F, false).getLocation();
        return Optional.of(new CellLocation(
                nativePlayer.level().dimension().identifier().toString(),
                block.x,
                block.y,
                block.z,
                nativePlayer.getYRot(),
                nativePlayer.getXRot()
        ));
    }

    private boolean safe(ServerLevel level, BlockPos feet) {
        var below = feet.below();
        var head = feet.above();
        return !level.getBlockState(below).isAir()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(head).isAir();
    }

    private boolean teleportNow(CellPlayer player, CellLocation location) {
        var nativePlayer = requireNative(player);
        var targetLevel = level(location.world).orElse(null);
        if (targetLevel == null) return false;

        return nativePlayer.teleportTo(
                targetLevel,
                location.x,
                location.y,
                location.z,
                Set.of(),
                location.yaw,
                location.pitch,
                true
        );
    }

    private Optional<ServerLevel> level(String world) {
        if (server == null || world.isBlank()) return Optional.empty();

        var normalized = normalizeWorldName(world);
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .filter(level -> normalizeWorldName(level.dimension().identifier().toString()).equals(normalized))
                .findFirst();
    }

    private String normalizeWorldName(String world) {
        var value = world.trim().toLowerCase();
        if (value.indexOf(':') < 0) {
            return "minecraft:" + value;
        }
        return value;
    }

    private ServerPlayer requireNative(CellPlayer player) {
        if (player.nativeHandle() instanceof ServerPlayer nativePlayer) {
            return nativePlayer;
        }
        throw new IllegalArgumentException("CellPlayer does not wrap a ServerPlayer: " + player);
    }

    private CellPlayer wrap(ServerPlayer player) {
        return new CellPlayer(
                player.getUUID(),
                player.getGameProfile().name(),
                player
        );
    }

}
