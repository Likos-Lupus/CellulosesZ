package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.core.runtime.CellulosesRuntime;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public final class DefaultModuleContext implements ModuleContext {

    private final String moduleId;
    private final Path dataDirectory;
    private final DefaultModuleScope scope;
    private final ModuleScopedServiceRegistry services;
    private final ConfigRegistry configs;
    private final EventRegistry events;
    private final Scheduler scheduler;
    private final CommandMiddlewareRegistry middlewares;
    private final CellulosesZLogger logger;
    private final Predicate<String> enabledPredicate;

    public DefaultModuleContext(
            String moduleId,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            Scheduler scheduler,
            CommandMiddlewareRegistry middlewares,
            CellulosesZLogger logger,
            Predicate<String> enabledPredicate,
            CellulosesRuntime runtime
    ) {
        this.moduleId = moduleId;
        this.dataDirectory = dataDirectory;
        this.scope = runtime.createModuleScope(moduleId);
        this.services = new ModuleScopedServiceRegistry(moduleId, services, scope);
        this.configs = new ModuleScopedConfigRegistry(moduleId, configs, scope);
        this.events = new ModuleScopedEventRegistry(moduleId, events, scope);
        this.scheduler = new ModuleScopedScheduler(moduleId, scheduler, scope);
        this.middlewares = new ModuleScopedCommandMiddlewareRegistry(moduleId, middlewares, scope);
        this.logger = logger;
        this.enabledPredicate = enabledPredicate;
    }

    public DefaultModuleContext(
            String moduleId,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            Scheduler scheduler,
            CommandMiddlewareRegistry middlewares,
            CellulosesZLogger logger,
            Predicate<String> enabledPredicate,
            DefaultModuleScope scope
    ) {
        this.moduleId = moduleId;
        this.dataDirectory = dataDirectory;
        this.scope = scope;
        this.services = new ModuleScopedServiceRegistry(moduleId, services, scope);
        this.configs = new ModuleScopedConfigRegistry(moduleId, configs, scope);
        this.events = new ModuleScopedEventRegistry(moduleId, events, scope);
        this.scheduler = new ModuleScopedScheduler(moduleId, scheduler, scope);
        this.middlewares = new ModuleScopedCommandMiddlewareRegistry(moduleId, middlewares, scope);
        this.logger = logger;
        this.enabledPredicate = enabledPredicate;
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public DefaultModuleScope scope() {
        return scope;
    }

    @Override
    public ServiceRegistry services() {
        return services;
    }

    @Override
    public ConfigRegistry configs() {
        return configs;
    }

    @Override
    public EventRegistry events() {
        return events;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public CommandMiddlewareRegistry middlewares() {
        return middlewares;
    }

    @Override
    public CellulosesZLogger logger() {
        return logger;
    }

    @Override
    public boolean moduleEnabled(String moduleId) {
        return enabledPredicate.test(moduleId);
    }

    List<AsyncInitializable> initializables() {
        return services.initializables();
    }

}
