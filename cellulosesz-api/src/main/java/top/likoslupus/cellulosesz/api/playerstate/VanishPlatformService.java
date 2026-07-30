package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface VanishPlatformService {

    PlatformResult<Void> setVanishedState(CellPlayer player, boolean vanished);

    PlatformResult<Void> setVisible(
            CellPlayer viewer,
            CellPlayer target,
            boolean visible
    );

}
