package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettings;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class DefaultRandomTeleportService implements RandomTeleportService {

    private final PlatformService platform;
    private final int attempts;

    public DefaultRandomTeleportService(PlatformService platform, int attempts) {
        this.platform = platform;
        this.attempts = Math.max(1, attempts);
    }

    @Override
    public Optional<CellLocation> randomLocation(String world, RandomTeleportSettings settings) {
        var random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < attempts; attempt++) {
            var angle = random.nextDouble(0, Math.PI * 2.0D);
            var radius = random.nextInt(settings.minRadius(), settings.maxRadius() + 1);
            var x = settings.centerX() + Math.cos(angle) * radius;
            var z = settings.centerZ() + Math.sin(angle) * radius;
            var found = platform.highestLocation(world, x, z).flatMap(platform::safeLocation);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

}
