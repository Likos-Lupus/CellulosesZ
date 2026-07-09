package top.likoslupus.cellulosesz.core.module;

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
import java.util.stream.IntStream;

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
    private final Map<String, ModuleContext> contexts = new LinkedHashMap<>();

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

    public void load() {
        var scanned = scanner.scan();
        var defaultModulesConfig = defaultModulesConfig(scanned);
        var modulesConfig = configs.register(
                "modules",
                ModulesConfig.class,
                "modules.yml",
                () -> defaultModulesConfig
        );

        scanned.forEach(descriptor -> descriptors.put(descriptor.id(), descriptor));

        var enabled = scanned.stream()
                .filter(descriptor -> modulesConfig.modules.getOrDefault(
                        descriptor.id(),
                        descriptor.enabledByDefault()
                ))
                .toList();
        var sorted = sorter.sort(enabled);

        sorted.forEach(this::loadModule);
        logger.info("Loaded " + loadedModules.size() + " CellulosesZ module(s).");
    }

    private ModulesConfig defaultModulesConfig(List<ModuleDescriptor> scanned) {
        var config = new ModulesConfig();
        scanned.forEach(descriptor -> config.modules.put(
                descriptor.id(),
                descriptor.enabledByDefault()
        ));
        return config;
    }

    private void loadModule(ModuleDescriptor descriptor) {
        try {
            var module = descriptor.moduleClass().getDeclaredConstructor().newInstance();
            var context = new DefaultModuleContext(
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

            module.construct(context);
            module.registerConfigs(context);
            module.registerServices(context);
            module.registerEvents(context);
            module.registerCommands(context);

            loadedModules.put(descriptor.id(), module);
            contexts.put(descriptor.id(), context);

            logger.info("Loaded module: " + descriptor.id());
        } catch (
                InstantiationException |
                IllegalAccessException |
                InvocationTargetException |
                NoSuchMethodException exception
        ) {
            throw new ModuleLoadException("Failed to instantiate module " + descriptor.id(), exception);
        }
    }

    public boolean moduleEnabled(String moduleId) {
        return loadedModules.containsKey(moduleId);
    }

    public void onServerStarting() {
        loadedModules.forEach((id, module) -> module.onServerStarting(contexts.get(id)));
    }

    public void onServerStarted() {
        loadedModules.forEach((id, module) -> module.onServerStarted(contexts.get(id)));
    }

    public void onReload() {
        loadedModules.forEach((id, module) -> module.onReload(contexts.get(id)));
    }

    public void onServerStopping() {
        var entries = new ArrayList<>(loadedModules.entrySet());
        IntStream.iterate(entries.size() - 1, index -> index >= 0, index -> index - 1)
                .mapToObj(entries::get)
                .forEach(entry -> entry.getValue().onServerStopping(contexts.get(entry.getKey())));
    }

    public List<LoadedModuleInfo> modules() {
        return descriptors.values().stream()
                .map(descriptor -> new LoadedModuleInfo(
                        descriptor.id(),
                        descriptor.name(),
                        descriptor.description(),
                        descriptor.phase(),
                        loadedModules.containsKey(descriptor.id()),
                        descriptor.moduleClass().getName()
                ))
                .toList();
    }

}
