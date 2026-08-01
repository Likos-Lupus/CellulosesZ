package top.likoslupus.cellulosesz.api.command;

import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.List;

public interface CommandMiddlewareRegistry {

    default Registration addMiddleware(CommandMiddleware middleware) {
        return addMiddleware(middleware, "global");
    }

    Registration addMiddleware(CommandMiddleware middleware, String owner);

    List<CommandMiddleware> middlewares();

}
