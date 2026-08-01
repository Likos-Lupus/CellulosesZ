package top.likoslupus.cellulosesz.modules.item.application;

import top.likoslupus.cellulosesz.api.item.WorkstationKind;
import top.likoslupus.cellulosesz.api.item.WorkstationPlatformService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import static java.util.Objects.requireNonNull;

public final class WorkstationCommandService {

    private final WorkstationPlatformService platform;

    public WorkstationCommandService(WorkstationPlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    public PlatformResult<Void> open(CellPlayer player, WorkstationKind kind) {
        return platform.open(player, kind);
    }

}
