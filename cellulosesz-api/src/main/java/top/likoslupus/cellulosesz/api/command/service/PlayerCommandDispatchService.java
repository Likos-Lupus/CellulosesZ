package top.likoslupus.cellulosesz.api.command.service;

public interface PlayerCommandDispatchService {

    PlayerCommandDispatchResult dispatch(PlayerCommandDispatchRequest request);

    void beginTick();

}
