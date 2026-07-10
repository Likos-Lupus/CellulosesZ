package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.EntityRemoveService;

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
        if (origin.isEmpty()) return AdminResult.failure("/remove 需要玩家作为半径中心。");
        var removed = platform.removeEntities(
                selector,
                origin.get(),
                radius
        );
        return removed >= 0
                ? AdminResult.success("已移除 " + removed + " 个实体。")
                : AdminResult.failure("实体移除失败。");
    }

}
