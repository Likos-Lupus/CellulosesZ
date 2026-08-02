package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.modules.teleport.TeleportRuntimeSettings;

import java.util.random.RandomGenerator;

import static java.util.Objects.requireNonNull;

public final class DefaultRandomTeleportService implements RandomTeleportService {

    private final TeleportOperations operations;
    private final WorldDirectory worlds;
    private final RandomGenerator random;
    private final TeleportRuntimeSettings runtimeSettings;

    public DefaultRandomTeleportService(
            TeleportOperations operations,
            WorldDirectory worlds,
            RandomGenerator random,
            TeleportRuntimeSettings runtimeSettings
    ) {
        this.operations = requireNonNull(operations, "operations");
        this.worlds = requireNonNull(worlds, "worlds");
        this.random = requireNonNull(random, "random");
        this.runtimeSettings = requireNonNull(runtimeSettings, "runtimeSettings");
    }

    @Override
    public RandomTeleportResult randomLocation(
            String world,
            RandomTeleportSettings settings
    ) {
        requireNonNull(settings, "settings");

        var resolution = worlds.resolve(world);
        if (resolution.worldId().isEmpty()) {
            return RandomTeleportResult.failure(RandomTeleportStatus.WORLD_NOT_FOUND);
        }

        var worldId = resolution.worldId().orElseThrow();
        var minSquared = (double) settings.minRadius() * settings.minRadius();
        var maxSquared = (double) settings.maxRadius() * settings.maxRadius();

        for (var attempt = 0; attempt < runtimeSettings.randomTeleportAttempts(); attempt++) {
            var angle = random.nextDouble(0.0D, Math.PI * 2.0D);
            var radius = Math.sqrt(random.nextDouble(minSquared, maxSquared));
            var requested = new CellLocation(
                    worldId,
                    settings.centerX() + Math.cos(angle) * radius,
                    0.0D,
                    settings.centerZ() + Math.sin(angle) * radius,
                    0.0F,
                    0.0F
            );
            var found = operations.highestSafeLocation(requested);

            if (found.successful() && found.value().isPresent()) {
                return RandomTeleportResult.success(found.value().orElseThrow());
            }
        }

        return RandomTeleportResult.failure(RandomTeleportStatus.NO_SAFE_LOCATION);
    }

}
