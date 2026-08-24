package top.likoslupus.cellulosesz.common.item;

import top.likoslupus.cellulosesz.api.item.WorkstationKind;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface WorkstationPlatformService {

    PlatformResult<Void> open(CellPlayer player, WorkstationKind kind);

}
