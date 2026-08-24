package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

public interface PlayerTargetingService {

    PlatformResult<CellLocation> targetLocation(CellPlayer player, int maximumDistance);

}
