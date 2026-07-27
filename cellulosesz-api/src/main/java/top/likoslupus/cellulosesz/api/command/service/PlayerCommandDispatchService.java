package top.likoslupus.cellulosesz.api.command.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

public interface PlayerCommandDispatchService {

    PlayerCommandDispatchResult dispatch(
            CellPlayer player,
            String command,
            CommandDispatchOrigin origin
    );

    void beginTick();

}
