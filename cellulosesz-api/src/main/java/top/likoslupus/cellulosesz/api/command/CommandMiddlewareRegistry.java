package top.likoslupus.cellulosesz.api.command;

import java.util.List;

public interface CommandMiddlewareRegistry {

    void addMiddleware(CommandMiddleware middleware);

    List<CommandMiddleware> middlewares();

}
