package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface WorkstationPlatformService {

    PlatformResult<Void> open(CellPlayer player, WorkstationKind kind);

}
