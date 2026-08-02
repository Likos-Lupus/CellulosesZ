package top.likoslupus.cellulosesz.common.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public final class MinecraftWorlds {

    private MinecraftWorlds() {
    }

    public static Optional<ServerLevel> findLoaded(
            MinecraftServer server,
            String worldId
    ) {
        requireNonNull(server, "server");
        if (worldId.isBlank()) {
            return Optional.empty();
        }

        var normalized = normalize(worldId);
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .filter(level ->
                        normalize(level.dimension().identifier().toString()).equals(normalized)
                )
                .findFirst();
    }

    private static String normalize(String worldId) {
        var normalized = requireNonNull(worldId, "worldId").strip().toLowerCase(Locale.ROOT);
        return normalized.contains(":")
                ? normalized
                : "minecraft:" + normalized;
    }

}
