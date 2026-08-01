package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.world.EntityRemovalPlatformService;
import top.likoslupus.cellulosesz.api.world.EntityRemovalRequest;
import top.likoslupus.cellulosesz.api.world.EntityRemoveSelector;
import top.likoslupus.cellulosesz.api.world.EntityRemoveService;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class DefaultEntityRemoveService implements EntityRemoveService {

    private final EntityRemovalPlatformService platform;

    public DefaultEntityRemoveService(EntityRemovalPlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public AdminResult remove(
            String selector,
            Optional<CellPlayer> origin,
            int radius
    ) {
        return remove(EntityRemoveSelector.entity(selector), origin, radius);
    }

    public AdminResult remove(
            EntityRemoveSelector selector,
            Optional<CellPlayer> origin,
            int radius
    ) {
        if (origin.isEmpty()) {
            return AdminResult.failure("service.world.remove-player-required");
        }

        var result = platform.remove(new EntityRemovalRequest(
                selector,
                origin,
                radius
        ));

        if (!result.successful() || result.value().isEmpty()) {
            return AdminResult.failure("service.world.remove-failed");
        }

        var value = result.value().orElseThrow();
        return value.failed() == 0
                ?
                AdminResult.success(
                        "service.world.remove-success",
                        Map.of("count", value.removed())
                )
                : AdminResult.partial(
                        "service.world.remove-partial",
                        Map.of(
                                "removed", value.removed(),
                                "failed", value.failed()
                        )
                );
    }

}
