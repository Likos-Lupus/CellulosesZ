package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.EntityRemoveService;

import java.util.Map;
import java.util.Optional;

public final class DefaultEntityRemoveService implements EntityRemoveService {

    private final PlatformService platform;

    public DefaultEntityRemoveService(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public AdminResult remove(
            String selector,
            Optional<CellPlayer> origin,
            int radius
    ) {
        if (origin.isEmpty()) return AdminResult.failure("service.world.remove-player-required");

        var removed = platform.removeEntities(selector, origin.get(), radius);
        return removed >= 0 ? AdminResult.success(
                "service.world.remove-success",
                Map.of("count", removed)
        ) : AdminResult.failure("service.world.remove-failed");
    }

}
