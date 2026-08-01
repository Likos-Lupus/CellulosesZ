package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class DefaultModuleContext implements ModuleContext {

    private final String moduleId;
    private final Path dataDirectory;
    private final ModuleScopedServiceRegistry services;
    private final ConfigRegistry configs;
    private final EventRegistry events;
    private final Scheduler scheduler;
    private final CellulosesZLogger logger;
    private final Predicate<String> enabledPredicate;
    private final List<Registration> trackedRegistrations = new ArrayList<>();

    public DefaultModuleContext(
            String moduleId,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            Scheduler scheduler,
            CellulosesZLogger logger,
            Predicate<String> enabledPredicate
    ) {
        this.moduleId = moduleId;
        this.dataDirectory = dataDirectory;
        this.services = new ModuleScopedServiceRegistry(moduleId, services);
        this.configs = configs;
        this.events = events;
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

    @Override
    public synchronized void track(Registration registration) {
        trackedRegistrations.add(requireNonNull(registration, "registration"));
    }

    List<AsyncInitializable> initializables() {
        return services.initializables();
    }

    List<AsyncCloseable> closeablesInReverseOrder() {
        return services.closeablesInReverseOrder();
    }

    synchronized List<Registration> registrationsInReverseOrder() {
        var result = new ArrayList<>(services.registrationsInReverseOrder());
        var tracked = new ArrayList<>(trackedRegistrations);

        Collections.reverse(tracked);
        result.addAll(0, tracked);
        return List.copyOf(result);
    }

}
