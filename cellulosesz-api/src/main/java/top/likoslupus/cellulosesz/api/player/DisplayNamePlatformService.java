package top.likoslupus.cellulosesz.api.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;

public interface DisplayNamePlatformService {

    PlatformResult<Void> setDisplayName(CellPlayer player, RichText displayName);

    PlatformResult<Void> refreshPlayerInfo(CellPlayer player);

}
