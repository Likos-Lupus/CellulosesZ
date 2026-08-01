package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.core.config.ModulesConfig;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class DefaultModuleManager {

    private final ModuleScanner scanner;
    private final Path dataDirectory;
    private final ServiceRegistry services;
    private final ConfigRegistry configs;
    private final EventRegistry events;
    private final Scheduler scheduler;
    private final CommandMiddlewareRegistry middlewares;
    private final CellulosesZLogger logger;
    private final ModuleDependencySorter sorter = new ModuleDependencySorter();
    private final Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
    private final Map<String, LoadedModule> loaded = new LinkedHashMap<>();
    private @Nullable ModulesConfig modulesConfig;
    private @Nullable Registration modulesConfigRegistration;
    private boolean serverStarting;
    private boolean serverStarted;
    private boolean serverStopping;

    public DefaultModuleManager(
            ModuleScanner scanner,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            Scheduler scheduler,
            CommandMiddlewareRegistry middlewares,
            CellulosesZLogger logger
    ) {
        this.scanner = requireNonNull(scanner, "scanner");
        this.dataDirectory = requireNonNull(dataDirectory, "dataDirectory");
        this.services = requireNonNull(services, "services");
        this.configs = requireNonNull(configs, "configs");
        this.events = requireNonNull(events, "events");
        this.scheduler = requireNonNull(scheduler, "scheduler");
        this.middlewares = requireNonNull(middlewares, "middlewares");
        this.logger = requireNonNull(logger, "logger");
    }

    public CompletableFuture<Void> loadAsync() {
        try {
            var scanned = scanner.scan();
            descriptors.clear();
            scanned.forEach(descriptor -> {
                var previous = descriptors.putIfAbsent(
                        descriptor.id(),
                        descriptor
                );

                if (previous != null) {
                    throw new ModuleLoadException("Duplicate module id: %s (%s, %s)".formatted(
                            descriptor.id(),
                            previous.moduleClass().getName(),
                            descriptor.moduleClass().getName()
                    ));
                }
            });

            modulesConfigRegistration = configs.register(
                    "modules",
                    ModulesConfig.class,
                    "modules.yml",
                    () -> defaultModulesConfig(scanned),
                    "core"
            );
            modulesConfig = configs.require(
                    "modules",
                    ModulesConfig.class
            );
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return reconcileModulesAsync(false)
                .thenRun(() -> logger.info(
                        "Loaded %d CellulosesZ module(s).".formatted(activeCount())
                ));
    }

    private ModulesConfig defaultModulesConfig(List<ModuleDescriptor> scanned) {
        var config = new ModulesConfig();
        scanned.forEach(descriptor -> config.modules.put(
                descriptor.id(),
                descriptor.enabledByDefault()
        ));
        return config;
    }

    private CompletableFuture<Void> reconcileModulesAsync(boolean reloadUnchanged) {
        final ReconcilePlan plan;
        try {
            plan = planReconciliation();
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return unloadPlanned(plan.unloadOrder(), false)
                .thenCompose(_ -> loadPlanned(plan.loadOrder()))
                .thenRun(() -> {
                    if (reloadUnchanged) {
                        plan.unchanged().forEach(this::reloadActiveModule);
                    }
                });
    }

    private int activeCount() {
        synchronized (this) {
            return (int) loaded.values().stream()
                    .filter(entry -> entry.state == ModuleState.ACTIVE)
                    .count();
        }
    }

    private ReconcilePlan planReconciliation() {
        var desired = desiredEnabledIds();
        validateDesiredGraph(desired);

        Set<String> current;
        Map<String, LoadedModule> currentEntries;
        synchronized (this) {
            current = new LinkedHashSet<>();
            currentEntries = new LinkedHashMap<>();

            loaded.forEach((id, value) -> {
                if (value.state == ModuleState.ACTIVE) {
                    current.add(id);
                    currentEntries.put(id, value);
                }
            });
        }

        var restart = current.stream()
                .filter(desired::contains)
                .filter(id -> !currentEntries.get(id)
                        .optionalAvailability.equals(
                                optionalAvailability(descriptors.get(id), desired)
                        )
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
        expandRequiredDependents(restart, current, desired);

        var toDisable = new LinkedHashSet<>(current);
        toDisable.removeAll(desired);
        toDisable.addAll(restart);

        var toEnable = new LinkedHashSet<>(desired);
        toEnable.removeAll(current);
        toEnable.addAll(restart);

        var unchanged = new LinkedHashSet<>(current);
        unchanged.retainAll(desired);
        unchanged.removeAll(restart);

        var currentSorted = sorter.sort(
                current.stream()
                        .map(descriptors::get)
                        .toList()
        );
        var unloadOrder = IntStream.iterate(
                        currentSorted.size() - 1,
                        index -> index >= 0,
                        index -> index - 1
                )
                .mapToObj(index -> currentSorted.get(index).id())
                .filter(toDisable::contains)
                .collect(Collectors.toCollection(ArrayList::new));

        var desiredSorted = sorter.sort(
                desired.stream()
                        .map(descriptors::get)
                        .toList()
        );
        var loadOrder = desiredSorted.stream()
                .filter(descriptor ->
                        toEnable.contains(descriptor.id())
                )
                .toList();
        var unchangedOrder = desiredSorted.stream()
                .map(ModuleDescriptor::id)
                .filter(unchanged::contains)
                .toList();

        return new ReconcilePlan(
                List.copyOf(unloadOrder),
                loadOrder,
                unchangedOrder
        );
    }

    private CompletableFuture<Void> unloadPlanned(
            List<String> ids,
            boolean serverShutdown
    ) {
        var failures = new ArrayList<Throwable>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var id : ids) {
            chain = chain
                    .thenCompose(_ -> unloadModuleAsync(id, serverShutdown)
                            .handle((_, failure) -> {
                                if (failure != null) {
                                    failures.add(unwrap(failure));
                                }
                                return (Void) null;
                            })
                    );
        }
        return chain.thenCompose(_ -> aggregateFailures("Module unload failed", failures));
    }

    private CompletableFuture<Void> loadPlanned(List<ModuleDescriptor> order) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var descriptor : order) {
            chain = chain.thenCompose(_ -> loadModuleAsync(descriptor));
        }
        return chain;
    }

    private void reloadActiveModule(String id) {
        LoadedModule entry;
        synchronized (this) {
            entry = loaded.get(id);
        }

        if (entry == null || entry.state != ModuleState.ACTIVE) {
            return;
        }

        runPhase(
                entry.descriptor,
                "reload",
                () -> entry.module.onReload(entry.context)
        );
    }

    private Set<String> desiredEnabledIds() {
        var config = requireModulesConfig();

        config.modules.keySet().stream()
                .filter(id -> !descriptors.containsKey(id))
                .forEach(id -> logger.warn("Ignoring unknown module id in modules.yml: " + id));

        return descriptors.values().stream()
                .filter(descriptor -> config.modules.getOrDefault(
                        descriptor.id(),
                        descriptor.enabledByDefault()
                ))
                .map(ModuleDescriptor::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateDesiredGraph(Set<String> desired) {
        desired.forEach(id -> {
            var descriptor = descriptors.get(id);
            descriptor.requires().forEach(required -> {
                if (!desired.contains(required)) {
                    throw new ModuleLoadException(
                            "Module %s requires enabled module %s".formatted(id, required)
                    );
                }
            });
        });

        sorter.sort(
                desired.stream()
                        .map(descriptors::get)
                        .toList()
        );
    }

    private Map<String, Boolean> optionalAvailability(
            ModuleDescriptor descriptor,
            Set<String> desired
    ) {
        var availability = new LinkedHashMap<String, Boolean>();
        descriptor.optional().forEach(optional ->
                availability.put(optional, desired.contains(optional))
        );
        return Map.copyOf(availability);
    }

    private void expandRequiredDependents(
            Set<String> restart,
            Set<String> current,
            Set<String> desired
    ) {
        var changed = true;
        while (changed) {
            changed = false;
            for (var descriptor : descriptors.values()) {
                if (!current.contains(descriptor.id()) || !desired.contains(descriptor.id())) {
                    continue;
                }
                if (descriptor.requires().stream().anyMatch(restart::contains)
                        && restart.add(descriptor.id())
                ) {
                    changed = true;
                }
            }
        }
    }

    private CompletableFuture<Void> unloadModuleAsync(String id, boolean serverShutdown) {
        final LoadedModule entry;
        synchronized (this) {
            entry = loaded.get(id);
            if (entry == null
                    || entry.state == ModuleState.UNLOADED
            ) {
                return CompletableFuture.completedFuture(null);
            }

            entry.state = ModuleState.STOPPING;
        }

        Throwable hookFailure = null;
        try {
            if (serverShutdown) {
                entry.module.onServerStopping(entry.context);
            } else {
                entry.module.onUnload(entry.context);
            }
        } catch (RuntimeException failure) {
            hookFailure = new ModuleLoadException(
                    "Module %s failed during %s".formatted(
                            id,
                            serverShutdown
                                    ? "server-stopping"
                                    : "unload"
                    ),
                    failure
            );
        }

        var capturedHookFailure = hookFailure;
        return entry.context.scope().closeAsync()
                .handle((_, closeFailure) -> {
                    synchronized (this) {
                        loaded.remove(id, entry);
                        entry.state = ModuleState.UNLOADED;
                    }

                    logger.info("Unloaded module: " + id);
                    if (capturedHookFailure == null
                            && closeFailure == null
                    ) {
                        return (Void) null;
                    }

                    var aggregate = new IllegalStateException("Failed to unload module " + id);
                    if (capturedHookFailure != null) {
                        aggregate.addSuppressed(capturedHookFailure);
                    }

                    if (closeFailure != null) {
                        aggregate.addSuppressed(unwrap(closeFailure));
                    }

                    throw new CompletionException(aggregate);
                });
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    private static CompletableFuture<Void> aggregateFailures(
            String message,
            List<Throwable> failures
    ) {
        if (failures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var aggregate = new IllegalStateException(message);
        failures.forEach(aggregate::addSuppressed);
        return CompletableFuture.failedFuture(aggregate);
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
                    scheduler,
                    middlewares,
                    logger,
                    this::moduleEnabled
            );
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException failure
        ) {
            return CompletableFuture.failedFuture(new ModuleLoadException(
                    "Module %s failed during instantiate".formatted(descriptor.id()),
                    failure
            ));
        }

        var entry = new LoadedModule(
                descriptor,
                module,
                context,
                ModuleState.LOADING,
                Map.of()
        );
        synchronized (this) {
            if (loaded.putIfAbsent(descriptor.id(), entry) != null) {
                return CompletableFuture.failedFuture(new ModuleLoadException(
                        "Module is already loaded or loading: " + descriptor.id()
                ));
            }
        }

        try {
            runPhase(
                    descriptor,
                    "construct",
                    () -> module.construct(context)
            );
            runPhase(
                    descriptor,
                    "register-configs",
                    () -> module.registerConfigs(context)
            );
            runPhase(
                    descriptor,
                    "register-services",
                    () -> module.registerServices(context)
            );
        } catch (RuntimeException failure) {
            return failLoad(entry, failure);
        }

        return initialize(context, descriptor)
                .thenRun(() -> {
                    runPhase(
                            descriptor,
                            "register-events",
                            () -> module.registerEvents(context)
                    );
                    runPhase(
                            descriptor,
                            "register-commands",
                            () -> module.registerCommands(context)
                    );

                    if (serverStarting || serverStarted) {
                        runPhase(
                                descriptor,
                                "server-starting",
                                () -> module.onServerStarting(context)
                        );
                    }

                    if (serverStarted) {
                        runPhase(
                                descriptor,
                                "server-started",
                                () -> module.onServerStarted(context)
                        );
                    }

                    synchronized (this) {
                        entry.optionalAvailability = descriptor.optional().stream()
                                .collect(
                                        LinkedHashMap::new,
                                        (map, optional) -> map.put(
                                                optional,
                                                moduleEnabled(optional)
                                        ),
                                        LinkedHashMap::putAll
                                );
                        entry.optionalAvailability = Map.copyOf(entry.optionalAvailability);
                        entry.state = ModuleState.ACTIVE;
                    }

                    logger.info("Loaded module: " + descriptor.id());
                })
                .exceptionallyCompose(failure -> failLoad(entry, unwrap(failure)));
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

    private ModulesConfig requireModulesConfig() {
        return requireNonNull(modulesConfig, "ModulesConfig has not been initialized");
    }

    public synchronized boolean moduleEnabled(String moduleId) {
        var entry = loaded.get(moduleId);
        return entry != null
                && entry.state == ModuleState.ACTIVE;
    }

    private CompletableFuture<Void> failLoad(LoadedModule entry, Throwable failure) {
        synchronized (this) {
            entry.state = ModuleState.STOPPING;
        }

        return entry.context.scope().closeAsync()
                .handle((_, closeFailure) -> {
                    synchronized (this) {
                        loaded.remove(entry.descriptor.id(), entry);
                        entry.state = ModuleState.UNLOADED;
                    }

                    var loadFailure = failure instanceof ModuleLoadException
                            ? failure
                            : new ModuleLoadException(
                                    "Module %s failed during load".formatted(entry.descriptor.id()),
                                    failure
                            );
                    if (closeFailure != null) {
                        loadFailure.addSuppressed(unwrap(closeFailure));
                    }

                    throw new CompletionException(loadFailure);
                });
    }

    private CompletableFuture<Void> initialize(
            DefaultModuleContext context,
            ModuleDescriptor descriptor
    ) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var initializable : context.initializables()) {
            chain = chain.thenCompose(_ -> {
                try {
                    return requireNonNull(initializable.initialize(), "initialize future");
                } catch (RuntimeException failure) {
                    return CompletableFuture.failedFuture(failure);
                }
            });
        }

        return chain
                .exceptionallyCompose(failure -> CompletableFuture.failedFuture(
                        new ModuleLoadException(
                                "Module %s failed during initialize".formatted(descriptor.id()),
                                unwrap(failure)
                        )
                ));
    }

    public CompletableFuture<Void> onReloadAsync() {
        try {
            modulesConfig = configs.require("modules", ModulesConfig.class);
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return reconcileModulesAsync(true);
    }

    public void onServerStarting() {
        serverStarting = true;
        activeInDependencyOrder().forEach(entry ->
                runPhase(
                        entry.descriptor,
                        "server-starting",
                        () -> entry.module.onServerStarting(entry.context)
                )
        );
    }

    private List<LoadedModule> activeInDependencyOrder() {
        Map<String, LoadedModule> snapshot;
        synchronized (this) {
            snapshot = new LinkedHashMap<>();
            loaded.forEach((id, entry) -> {
                if (entry.state == ModuleState.ACTIVE) {
                    snapshot.put(id, entry);
                }
            });
        }

        return sorter.sort(
                        snapshot.values().stream()
                                .map(entry -> entry.descriptor)
                                .toList()
                ).stream()
                .map(descriptor -> snapshot.get(descriptor.id()))
                .toList();
    }

    public void onServerStarted() {
        serverStarted = true;
        activeInDependencyOrder().forEach(entry ->
                runPhase(
                        entry.descriptor,
                        "server-started",
                        () -> entry.module.onServerStarted(entry.context)
                )
        );
    }

    public CompletableFuture<Void> onServerStoppingAsync() {
        serverStopping = true;
        var order = activeInDependencyOrder().stream()
                .map(entry -> entry.descriptor.id())
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(order);
        return unloadPlanned(order, true);
    }

    public synchronized List<LoadedModuleInfo> modules() {
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

    private enum ModuleState {

        LOADING,
        ACTIVE,
        STOPPING,
        UNLOADED

    }

    private static final class LoadedModule {

        private final ModuleDescriptor descriptor;
        private final CellulosesZModule module;
        private final DefaultModuleContext context;
        private ModuleState state;
        private Map<String, Boolean> optionalAvailability;

        private LoadedModule(
                ModuleDescriptor descriptor,
                CellulosesZModule module,
                DefaultModuleContext context,
                ModuleState state,
                Map<String, Boolean> optionalAvailability
        ) {
            this.descriptor = descriptor;
            this.module = module;
            this.context = context;
            this.state = state;
            this.optionalAvailability = optionalAvailability;
        }

    }

    private record ReconcilePlan(
            List<String> unloadOrder,
            List<ModuleDescriptor> loadOrder,
            List<String> unchanged
    ) {

    }

}
