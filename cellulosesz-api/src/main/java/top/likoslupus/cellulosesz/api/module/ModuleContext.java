package top.likoslupus.cellulosesz.api.module;

import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.nio.file.Path;

public interface ModuleContext {

    String moduleId();

    Path dataDirectory();

    ModuleScope scope();

    ServiceRegistry services();

    ConfigRegistry configs();

    EventRegistry events();

    Scheduler scheduler();

    CommandMiddlewareRegistry middlewares();

    CellulosesZLogger logger();

    boolean moduleEnabled(String moduleId);

}
