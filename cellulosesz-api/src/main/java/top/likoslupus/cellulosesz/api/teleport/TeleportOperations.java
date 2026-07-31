package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface TeleportOperations {

    PlatformResult<Void> move(CellPlayer player, CellLocation destination);

    PlatformResult<CellLocation> safeLocation(CellLocation requested);

    PlatformResult<CellLocation> highestSafeLocation(CellLocation column);

    PlatformResult<CellLocation> lowestSafeLocation(CellLocation column);

    PlatformResult<CellLocation> targetLocation(CellPlayer player, int maximumDistance);

}
