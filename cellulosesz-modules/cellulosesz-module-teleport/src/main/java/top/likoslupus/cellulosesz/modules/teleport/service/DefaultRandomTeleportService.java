package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class DefaultRandomTeleportService implements RandomTeleportService {

    private final PlatformService platform;
    private final int attempts;

    public DefaultRandomTeleportService(
            PlatformService platform,
            int attempts
    ) {
        this.platform = platform;
        this.attempts = Math.max(1, attempts);
    }

    @Override
    public Optional<CellLocation> randomLocation(
            String world,
            int minRadius,
            int maxRadius
    ) {
        var min = Math.max(0, minRadius);
        var max = Math.max(min + 1, maxRadius);
        var random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < attempts; attempt++) {
            var angle = random.nextDouble(0, Math.PI * 2.0D);
            var radius = random.nextInt(min, max + 1);
            var x = Math.cos(angle) * radius;
            var z = Math.sin(angle) * radius;
            var found = platform.highestLocation(world, x, z)
                    .flatMap(platform::safeLocation);
            if (found.isPresent()) return found;
        }

        return Optional.empty();
    }

}
