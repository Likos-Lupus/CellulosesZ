package top.likoslupus.cellulosesz.api.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;

public interface PlayerConnectionService {

    PlatformResult<Void> disconnect(CellPlayer player, RichText reason);

}
