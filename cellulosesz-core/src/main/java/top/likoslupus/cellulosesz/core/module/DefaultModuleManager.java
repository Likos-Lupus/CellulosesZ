package top.likoslupus.cellulosesz.core.module;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.core.config.ModulesConfig;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.Objects.requireNonNull;

public final class DefaultModuleManager {

    private final ModuleScanner scanner;
    private final Path dataDirectory;
    private final ServiceRegistry services;
    private final ConfigRegistry configs;
    private final EventRegistry events;
    private final CommandRegistry commands;
    private final Scheduler scheduler;
    private final CellulosesZLogger logger;
    private final ModuleDependencySorter sorter = new ModuleDependencySorter();
    private final Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, CellulosesZModule> loadedModules = new LinkedHashMap<>();
    private final Map<String, DefaultModuleContext> contexts = new LinkedHashMap<>();
    private @Nullable ModulesConfig modulesConfig;

    public DefaultModuleManager(
            ModuleScanner scanner,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            CommandRegistry commands,
            Scheduler scheduler,
            CellulosesZLogger logger
    ) {
        this.scanner = scanner;
        this.dataDirectory = dataDirectory;
        this.services = services;
        this.configs = configs;
        this.events = events;
        this.commands = commands;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public CompletableFuture<Void> loadAsync() {
        final List<ModuleDescriptor> sorted;
        try {
            var scanned = scanner.scan();
            var defaultModulesConfig = defaultModulesConfig(scanned);
            modulesConfig = configs.register(
                    "modules",
                    ModulesConfig.class,
                    "modules.yml",
                    () -> defaultModulesConfig
            );

            scanned.forEach(descriptor -> {
                var previous = descriptors.putIfAbsent(descriptor.id(), descriptor);
                if (previous != null) {
                    throw new ModuleLoadException("Duplicate module id: %s (%s, %s)".formatted(
                            descriptor.id(),
                            previous.moduleClass().getName(),
                            descriptor.moduleClass().getName()
                    ));
                }
            });

            var currentModules = requireModulesConfig();
            var enabled = scanned.stream()
                    .filter(descriptor -> currentModules.modules.getOrDefault(
                            descriptor.id(),
                            descriptor.enabledByDefault()
                    ))
                    .toList();
            sorted = sorter.sort(enabled);
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var descriptor : sorted) {
            chain = chain.thenCompose(_ -> loadModuleAsync(descriptor));
        }
        return chain.thenRun(() -> logger.info("Loaded %d CellulosesZ module(s).".formatted(loadedModules.size())));
    }

    private ModulesConfig defaultModulesConfig(List<ModuleDescriptor> scanned) {
        var config = new ModulesConfig();
        scanned.forEach(descriptor -> config.modules.put(
                descriptor.id(),
                descriptor.enabledByDefault()
        ));
        return config;
    }

    private ModulesConfig requireModulesConfig() {
        return requireNonNull(modulesConfig, "ModulesConfig has not been initialized");
    }

    private CompletableFuture<Void> loadModuleAsync(ModuleDescriptor descriptor) {
        final CellulosesZModule module;
        final DefaultModuleContext context;
        try {
            module = descriptor.moduleClass().getDeclaredConstructor().newInstance();
            context = new DefaultModuleContext(
                    descriptor.id(),
                    dataDirectory.resolve(descriptor.id()),
                    services,
                    configs,
                    events,
                    commands,
                    scheduler,
                    logger,
                    this::moduleEnabled
            );
            runPhase(descriptor, "construct", () -> module.construct(context));
            runPhase(descriptor, "register-configs", () -> module.registerConfigs(context));
            runPhase(descriptor, "register-services", () -> module.registerServices(context));
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException failure
        ) {
            return CompletableFuture.failedFuture(new ModuleLoadException(
                    "Module %s failed during instantiate".formatted(descriptor.id()), failure
            ));
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return initialize(context, descriptor)
                .thenRun(() -> {
                    runPhase(descriptor, "register-events", () -> module.registerEvents(context));
                    runPhase(descriptor, "register-commands", () -> module.registerCommands(context));
                    loadedModules.put(descriptor.id(), module);
                    contexts.put(descriptor.id(), context);
                    logger.info("Loaded module: " + descriptor.id());
                })
                .whenComplete((_, failure) -> {
                    if (failure != null && !loadedModules.containsKey(descriptor.id())) {
                        rollbackServices(context, descriptor.id());
                    }
                });
    }

    public boolean moduleEnabled(String moduleId) {
        var descriptor = descriptors.get(moduleId);
        if (descriptor == null || !loadedModules.containsKey(moduleId)) return false;
        return requireModulesConfig().modules.getOrDefault(moduleId, descriptor.enabledByDefault());
    }

    private void runPhase(
            ModuleDescriptor descriptor,
            String phase,
            Runnable operation
    ) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            throw new ModuleLoadException(
                    "Module %s failed during %s".formatted(descriptor.id(), phase),
                    failure
            );
        }
    }

    private CompletableFuture<Void> initialize(DefaultModuleContext context, ModuleDescriptor descriptor) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var initializable : context.initializables()) {
            chain = chain.thenCompose(_ -> {
                try {
                    return initializable.initialize();
                } catch (RuntimeException failure) {
                    return CompletableFuture.failedFuture(failure);
                }
            });
        }
        return chain.exceptionallyCompose(failure -> CompletableFuture.failedFuture(
                new ModuleLoadException("Module %s failed during initialize".formatted(descriptor.id()), unwrap(failure))
        ));
    }

    private void rollbackServices(DefaultModuleContext context, String moduleId) {
        context.registrationsInReverseOrder().forEach(registration -> {
            try {
                registration.close();
            } catch (RuntimeException failure) {
                logger.error("Failed to roll back service registration for module " + moduleId, failure);
            }
        });
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    public void onServerStarting() {
        loadedModules.forEach((id, module) -> runLifecycle(
                id,
                "server-starting",
                () -> module.onServerStarting(contexts.get(id))
        ));
    }

    private void runLifecycle(String moduleId, String phase, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            throw new ModuleLoadException(
                    "Module %s failed during %s".formatted(moduleId, phase),
                    failure
            );
        }
    }

    public void onServerStarted() {
        loadedModules.forEach((id, module) -> runLifecycle(
                id,
                "server-started",
                () -> module.onServerStarted(contexts.get(id))
        ));
    }

    public void onReload() {
        modulesConfig = configs.require("modules", ModulesConfig.class);
        loadedModules.forEach((id, module) -> runLifecycle(
                id,
                "reload",
                () -> module.onReload(contexts.get(id))
        ));
    }

    public CompletableFuture<Void> onServerStoppingAsync() {
        var entries = new ArrayList<>(loadedModules.entrySet());
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int index = entries.size() - 1; index >= 0; index--) {
            var entry = entries.get(index);
            var id = entry.getKey();
            var module = entry.getValue();
            var context = contexts.get(id);
            chain = chain.handle((_, _) -> null)
                    .thenCompose(_ -> stopModule(id, module, context));
        }
        return chain;
    }

    private CompletableFuture<Void> stopModule(
            String id,
            CellulosesZModule module,
            DefaultModuleContext context
    ) {
        try {
            module.onServerStopping(context);
        } catch (RuntimeException failure) {
            logger.error("Module %s failed during server-stopping".formatted(id), failure);
        }
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var closeable : context.closeablesInReverseOrder()) {
            chain = chain.handle((_, _) -> null)
                    .thenCompose(_ -> {
                        try {
                            return closeable.closeAsync();
                        } catch (RuntimeException failure) {
                            return CompletableFuture.failedFuture(failure);
                        }
                    })
                    .exceptionally(failure -> {
                        logger.error("Async close failed for module " + id, unwrap(failure));
                        return (Void) null;
                    });
        }
        return chain.whenComplete((_, _) ->
                context.registrationsInReverseOrder().forEach(registration -> {
                    try {
                        registration.close();
                    } catch (RuntimeException closeFailure) {
                        logger.error("Service unregistration failed for module " + id, closeFailure);
                    }
                }));
    }

    public List<LoadedModuleInfo> modules() {
        return descriptors.values().stream()
                .map(descriptor -> new LoadedModuleInfo(
                        descriptor.id(),
                        descriptor.name(),
                        descriptor.description(),
                        descriptor.phase(),
                        moduleEnabled(descriptor.id()),
                        descriptor.moduleClass().getName()
                ))
                .toList();
    }

}
