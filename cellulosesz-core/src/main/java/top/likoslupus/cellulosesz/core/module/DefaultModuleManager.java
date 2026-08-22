package top.likoslupus.cellulosesz.core.module;

import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigSnapshot;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.core.config.ModulesConfig;
import top.likoslupus.cellulosesz.core.runtime.CellulosesRuntime;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
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
    private final AtomicBoolean reloadPrepared = new AtomicBoolean();
    private final CellulosesRuntime runtime;
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
        this(
                scanner,
                dataDirectory,
                services,
                configs,
                events,
                scheduler,
                middlewares,
                logger,
                new CellulosesRuntime(logger)
        );
    }

    public DefaultModuleManager(
            ModuleScanner scanner,
            Path dataDirectory,
            ServiceRegistry services,
            ConfigRegistry configs,
            EventRegistry events,
            Scheduler scheduler,
            CommandMiddlewareRegistry middlewares,
            CellulosesZLogger logger,
            CellulosesRuntime runtime
    ) {
        this.scanner = requireNonNull(scanner, "scanner");
        this.dataDirectory = requireNonNull(dataDirectory, "dataDirectory");
        this.services = requireNonNull(services, "services");
        this.configs = requireNonNull(configs, "configs");
        this.events = requireNonNull(events, "events");
        this.scheduler = requireNonNull(scheduler, "scheduler");
        this.middlewares = requireNonNull(middlewares, "middlewares");
        this.logger = requireNonNull(logger, "logger");
        this.runtime = requireNonNull(runtime, "runtime");
    }

    private static <T> CompletableFuture<T> invoke(
            Supplier<? extends CompletionStage<T>> action
    ) {
        try {
            return requireNonNull(action.get(), "stage").toCompletableFuture();
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    public CompletableFuture<Void> loadAsync() {
        try {
            var scanned = scanner.scan();
            descriptors.clear();

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

            modulesConfigRegistration = configs.register(
                    "modules",
                    ModulesConfig.class,
                    "modules.yml",
                    () -> defaultModulesConfig(scanned),
                    "core"
            );
            modulesConfig = copyModulesConfig(configs.require("modules", ModulesConfig.class));
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return reconcileInitialModules()
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

    private static ModulesConfig copyModulesConfig(ModulesConfig source) {
        var copy = new ModulesConfig();
        copy.modules.putAll(source.modules);

        return copy;
    }

    private CompletableFuture<Void> reconcileInitialModules() {
        final ReconcilePlan plan;
        try {
            plan = planReconciliation(desiredEnabledIds(requireModulesConfig()));
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return unloadPlanned(plan.unloadOrder(), false)
                .thenCompose(_ -> loadPlanned(plan.loadOrder()));
    }

    private int activeCount() {
        return activeIds().size();
    }

    private ReconcilePlan planReconciliation(Set<String> desired) {
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
                .filter(id -> !currentEntries.get(id).optionalAvailability.equals(
                        optionalAvailability(descriptors.get(id), desired)
                ))
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
                .filter(descriptor -> toEnable.contains(descriptor.id()))
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

    private Set<String> desiredEnabledIds(ModulesConfig config) {
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

    private ModulesConfig requireModulesConfig() {
        return requireNonNull(modulesConfig, "ModulesConfig has not been initialized");
    }

    private CompletableFuture<Void> unloadPlanned(
            List<String> ids,
            boolean serverShutdown
    ) {
        var failures = new ArrayList<Throwable>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var id : ids) {
            chain = chain.thenCompose(_ -> unloadModuleAsync(id, serverShutdown)
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

    private synchronized Set<String> activeIds() {
        return loaded.entrySet().stream()
                .filter(entry ->
                        entry.getValue().state == ModuleState.ACTIVE
                )
                .map(Map.Entry::getKey)
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
        sorter.sort(desired.stream().map(descriptors::get).toList());
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
            if (entry == null || entry.state == ModuleState.UNLOADED) {
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
                    if (capturedHookFailure == null && closeFailure == null) {
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
        while (current instanceof CompletionException && current.getCause() != null) {
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
                    this::moduleEnabled,
                    runtime
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

    public synchronized boolean moduleEnabled(String moduleId) {
        var entry = loaded.get(moduleId);
        return entry != null && entry.state == ModuleState.ACTIVE;
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

        return chain.exceptionallyCompose(failure -> CompletableFuture.failedFuture(
                new ModuleLoadException(
                        "Module %s failed during initialize".formatted(descriptor.id()),
                        unwrap(failure)
                )
        ));
    }

    public CompletionStage<PreparedModuleReload> prepareReload(ConfigSnapshot candidateConfigs) {
        requireNonNull(candidateConfigs, "candidateConfigs");
        synchronized (this) {
            if (serverStopping) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Module manager is stopping"
                ));
            }
        }

        if (!reloadPrepared.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "A module reload is already prepared"
            ));
        }

        final ModulesConfig candidate;
        final ModulesConfig previous;
        final ReconcilePlan plan;
        final Set<String> previousActive;
        final Set<String> candidateEnabled;
        try {
            candidate = copyModulesConfig(candidateConfigs.require("modules", ModulesConfig.class));
            previous = copyModulesConfig(requireModulesConfig());
            candidateEnabled = desiredEnabledIds(candidate);
            plan = planReconciliation(candidateEnabled);
            previousActive = activeIds();
        } catch (RuntimeException failure) {
            reloadPrepared.set(false);
            return CompletableFuture.failedFuture(failure);
        }

        var execution = new ReloadExecution(
                previous,
                candidate,
                previousActive,
                candidateEnabled,
                plan
        );

        CompletableFuture<Void> prepareChain = CompletableFuture.completedFuture(null);
        for (var id : plan.unchanged()) {
            prepareChain = prepareChain.thenCompose(_ -> prepareUnchanged(
                    execution,
                    id,
                    candidateConfigs
            ));
        }

        return prepareChain
                .thenApply(_ -> PreparedReloads.of(
                        () -> commitReload(execution)
                                .whenComplete((_, failure) -> {
                                    if (failure == null) {
                                        reloadPrepared.set(false);
                                    }
                                }),
                        () -> rollbackReload(execution)
                                .whenComplete((_, _) -> reloadPrepared.set(false))
                ))
                .exceptionallyCompose(failure -> rollbackPrepared(execution.prepared)
                        .handle((_, rollbackFailure) -> {
                            reloadPrepared.set(false);
                            var original = unwrap(failure);
                            if (rollbackFailure != null) {
                                original.addSuppressed(unwrap(rollbackFailure));
                            }
                            throw new CompletionException(original);
                        })
                );
    }

    private CompletableFuture<Void> prepareUnchanged(
            ReloadExecution execution,
            String id,
            ConfigSnapshot candidateConfigs
    ) {
        final LoadedModule entry;
        synchronized (this) {
            entry = loaded.get(id);
        }

        if (entry == null || entry.state != ModuleState.ACTIVE) {
            return CompletableFuture.failedFuture(new ModuleLoadException(
                    "Module became unavailable while reload was being prepared: " + id
            ));
        }

        try {
            return requireNonNull(
                    entry.module.prepareReload(new DefaultModuleReloadContext(
                            entry.context,
                            candidateConfigs,
                            execution.candidateEnabled::contains
                    )),
                    "module prepare stage"
            )
                    .thenAccept(prepared -> execution.prepared.add(new PreparedEntry(
                            id,
                            requireNonNull(prepared, "prepared module reload")
                    )))
                    .toCompletableFuture();
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(new ModuleLoadException(
                    "Module %s failed during reload prepare".formatted(id),
                    failure
            ));
        }
    }

    private CompletableFuture<Void> commitReload(ReloadExecution execution) {
        CompletableFuture<Void> chain = onServerThread(() -> {
            synchronized (this) {
                modulesConfig = copyModulesConfig(execution.candidateConfig);
            }
            return CompletableFuture.completedFuture(null);
        });

        for (var id : execution.plan.unloadOrder()) {
            chain = chain.thenCompose(_ -> onServerThread(() -> unloadModuleAsync(
                            id,
                            false
                    ))
                            .whenComplete((_, _) -> {
                                if (!moduleEnabled(id)) {
                                    execution.unloaded.add(id);
                                }
                            })
            );
        }

        for (var descriptor : execution.plan.loadOrder()) {
            chain = chain.thenCompose(_ -> onServerThread(() -> loadModuleAsync(descriptor))
                    .thenRun(() -> execution.loaded.add(descriptor.id()))
            );
        }

        for (var prepared : execution.prepared) {
            chain = chain.thenCompose(_ -> onServerThread(() -> {
                execution.commitAttempted.add(prepared.id());
                return prepared.transaction().commit();
            }));
        }

        return chain;
    }

    private CompletableFuture<Void> rollbackReload(ReloadExecution execution) {
        synchronized (this) {
            modulesConfig = copyModulesConfig(execution.previousConfig);
        }

        var failures = new ArrayList<Throwable>();
        return rollbackPrepared(execution.prepared)
                .handle((_, failure) -> {
                    if (failure != null) {
                        failures.add(unwrap(failure));
                    }
                    return (Void) null;
                })
                .thenCompose(_ -> rollbackLoadedModules(execution, failures))
                .thenCompose(_ -> restorePreviousModules(execution, failures))
                .thenCompose(_ -> aggregateFailures(
                        "Module reload rollback failed",
                        failures
                ));
    }

    private CompletableFuture<Void> rollbackPrepared(List<PreparedEntry> prepared) {
        var failures = new ArrayList<Throwable>();

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int index = prepared.size() - 1; index >= 0; index--) {
            var transaction = prepared.get(index).transaction();
            chain = chain.thenCompose(_ -> onServerThread(transaction::rollback)
                    .handle((_, failure) -> {
                        if (failure != null) {
                            failures.add(unwrap(failure));
                        }
                        return (Void) null;
                    })
            );
        }

        return chain.thenCompose(_ -> aggregateFailures(
                "Prepared module rollback failed",
                failures
        ));
    }

    private CompletableFuture<Void> rollbackLoadedModules(
            ReloadExecution execution,
            List<Throwable> failures
    ) {
        var ids = new ArrayList<>(execution.loaded);
        Collections.reverse(ids);

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var id : ids) {
            chain = chain.thenCompose(_ ->
                    onServerThread(() -> unloadModuleAsync(id, false))
                            .handle((_, failure) -> {
                                if (failure != null) {
                                    failures.add(unwrap(failure));
                                }
                                return (Void) null;
                            })
            );
        }
        return chain;
    }

    private CompletableFuture<Void> restorePreviousModules(
            ReloadExecution execution,
            List<Throwable> failures
    ) {
        var active = activeIds();
        var missing = new LinkedHashSet<>(execution.previousActive);

        missing.removeAll(active);
        var order = sorter.sort(
                missing.stream()
                        .map(descriptors::get)
                        .toList()
        );

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var descriptor : order) {
            chain = chain.thenCompose(_ ->
                    onServerThread(() -> loadModuleAsync(descriptor))
                            .handle((_, failure) -> {
                                if (failure != null) {
                                    failures.add(unwrap(failure));
                                }
                                return (Void) null;
                            })
            );
        }

        return chain;
    }

    private <T> CompletableFuture<T> onServerThread(
            Supplier<? extends CompletionStage<T>> action
    ) {
        var executor = services.optional(ServerThreadExecutor.class);
        if (executor.isEmpty()) {
            return invoke(action);
        }

        return executor.orElseThrow()
                .submit(() -> requireNonNull(action.get(), "server-thread stage"))
                .thenCompose(CompletionStage::toCompletableFuture);
    }

    public void onServerStarting() {
        serverStarting = true;
        activeInDependencyOrder().forEach(entry -> runPhase(
                entry.descriptor,
                "server-starting",
                () -> entry.module.onServerStarting(entry.context)
        ));
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
        activeInDependencyOrder().forEach(entry -> runPhase(
                entry.descriptor,
                "server-started",
                () -> entry.module.onServerStarted(entry.context)
        ));
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

    private record PreparedEntry(
            String id,
            PreparedModuleReload transaction
    ) {

    }

    private static final class ReloadExecution {

        private final ModulesConfig previousConfig;
        private final ModulesConfig candidateConfig;
        private final Set<String> previousActive;
        private final Set<String> candidateEnabled;
        private final ReconcilePlan plan;
        private final List<PreparedEntry> prepared = new ArrayList<>();
        private final List<String> unloaded = new ArrayList<>();
        private final List<String> loaded = new ArrayList<>();
        private final List<String> commitAttempted = new ArrayList<>();

        private ReloadExecution(
                ModulesConfig previousConfig,
                ModulesConfig candidateConfig,
                Set<String> previousActive,
                Set<String> candidateEnabled,
                ReconcilePlan plan
        ) {
            this.previousConfig = previousConfig;
            this.candidateConfig = candidateConfig;
            this.previousActive = Set.copyOf(previousActive);
            this.candidateEnabled = Set.copyOf(candidateEnabled);
            this.plan = plan;
        }

    }

}
