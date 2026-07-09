package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.nio.file.Path;
import java.util.function.Predicate;

public final class DefaultModuleContext implements ModuleContext {

    private final String moduleId;
    private final Path dataDirectory;
    private final ServiceRegistry services;
    private final ConfigRegistry configs;
    private final EventRegistry events;
    private final CommandRegistry commands;
    private final Scheduler scheduler;
    private final CellulosesZLogger logger;
    private final Predicate<String> enabledPredicate;

    public DefaultModuleContext(
            String moduleId,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            CommandRegistry commands,
            Scheduler scheduler,
            CellulosesZLogger logger,
            Predicate<String> enabledPredicate
    ) {
        this.moduleId = moduleId;
        this.dataDirectory = dataDirectory;
        this.services = services;
        this.configs = configs;
        this.events = events;
        this.commands = commands;
        this.scheduler = scheduler;
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
    public CommandRegistry commands() {
        return commands;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public CellulosesZLogger logger() {
        return logger;
    }

    @Override
    public boolean moduleEnabled(String moduleId) {
        return enabledPredicate.test(moduleId);
    }

}
