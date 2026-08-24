package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.core.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.core.config.ConfigRegistry;
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.core.scheduler.Scheduler;

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
