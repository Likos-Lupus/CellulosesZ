package top.likoslupus.cellulosesz.common.command.service;

public interface PlayerCommandDispatchService {

    PlayerCommandDispatchResult dispatch(PlayerCommandDispatchRequest request);

    void beginTick();

}
