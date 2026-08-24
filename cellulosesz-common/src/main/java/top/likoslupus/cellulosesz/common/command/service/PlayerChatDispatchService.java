package top.likoslupus.cellulosesz.common.command.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface PlayerChatDispatchService {

    PlatformResult<Void> dispatch(CellPlayer player, String message);

}
