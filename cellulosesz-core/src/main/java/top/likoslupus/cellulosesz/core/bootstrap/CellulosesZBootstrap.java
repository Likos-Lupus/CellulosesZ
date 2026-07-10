package top.likoslupus.cellulosesz.core.bootstrap;

import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.LoadedModuleInfo;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.runtime.RuntimeService;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.core.command.DefaultCommandRegistry;
import top.likoslupus.cellulosesz.core.config.CoreConfig;
import top.likoslupus.cellulosesz.core.config.JacksonConfigRegistry;
import top.likoslupus.cellulosesz.core.event.SimpleEventRegistry;
import top.likoslupus.cellulosesz.core.i18n.DefaultMessageService;
import top.likoslupus.cellulosesz.core.module.ClassGraphModuleScanner;
import top.likoslupus.cellulosesz.core.module.DefaultModuleManager;
import top.likoslupus.cellulosesz.core.permission.DefaultPermissionService;
import top.likoslupus.cellulosesz.core.permission.PermissionBackend;
import top.likoslupus.cellulosesz.core.runtime.DefaultRuntimeService;
import top.likoslupus.cellulosesz.core.scheduler.DefaultScheduler;
import top.likoslupus.cellulosesz.core.service.DefaultServiceRegistry;
import top.likoslupus.cellulosesz.core.storage.JacksonStorageService;

import java.nio.file.Path;
import java.util.List;

public final class CellulosesZBootstrap {

    private final Path configDirectory;
    private final String version;
    private final CellulosesZLogger logger;
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();
    private final JacksonConfigRegistry configs;
    private final SimpleEventRegistry events = new SimpleEventRegistry();
    private final DefaultScheduler scheduler = new DefaultScheduler();
    private final DefaultCommandRegistry commands = new DefaultCommandRegistry();
    private final DefaultPermissionService permissions = new DefaultPermissionService();
    private final JacksonStorageService storage;
    private final DefaultMessageService messages;
    private DefaultModuleManager modules;
    private CoreConfig coreConfig;
    private boolean initialized;

    public CellulosesZBootstrap(
            Path configDirectory,
            String version,
            CellulosesZLogger logger
    ) {
        this.configDirectory = configDirectory;
        this.version = version;
        this.logger = logger;
        this.configs = new JacksonConfigRegistry(configDirectory, logger);
        this.storage = new JacksonStorageService(
                configDirectory.resolve("data"),
                scheduler::async,
                logger
        );
        this.messages = new DefaultMessageService(configDirectory.resolve("messages"), logger);
    }

    public <T> void registerService(Class<T> type, T instance) {
        services.register(type, instance);
    }

    public synchronized void initialize() {
        if (initialized) return;

        coreConfig = configs.register(
                "core",
                CoreConfig.class,
                "cellulosesz.yml",
                CoreConfig::new
        );
        messages.locales(coreConfig.locale.defaultLocale, coreConfig.locale.fallback);
        messages.reload();

        services.register(ServiceRegistry.class, services);
        services.register(ConfigRegistry.class, configs);
        services.register(EventRegistry.class, events);
        services.register(Scheduler.class, scheduler);
        services.register(CommandRegistry.class, commands);
        services.register(CommandMiddlewareRegistry.class, commands);
        services.register(DefaultCommandRegistry.class, commands);
        services.register(PermissionService.class, permissions);
        services.register(DefaultPermissionService.class, permissions);
        services.register(StorageService.class, storage);
        services.register(MessageService.class, messages);
        services.register(RuntimeService.class, new DefaultRuntimeService(this));

        modules = new DefaultModuleManager(
                new ClassGraphModuleScanner(),
                configDirectory.resolve("data"),
                services,
                configs,
                events,
                commands,
                scheduler,
                logger
        );
        modules.load();
        initialized = true;
    }

    public void permissionBackend(PermissionBackend backend) {
        permissions.backend(backend);
    }

    public void onServerStarting(Object server) {
        logger.info("CellulosesZ server starting.");
        modules.onServerStarting();
    }

    public void onServerStarted(Object server) {
        logger.info("CellulosesZ server started.");
        modules.onServerStarted();
    }

    public void onServerStopping(Object server) {
        logger.info("CellulosesZ server stopping.");
        modules.onServerStopping();
        scheduler.close();
    }

    public void onPlayerJoin(Object player) {
        events.fire(new PlayerJoinEvent(player));
    }

    public void onPlayerDisconnect(Object player) {
        events.fire(new PlayerDisconnectEvent(player));
    }

    public void tick() {
        scheduler.tick();
    }

    public synchronized void reload() {
        configs.reload();
        coreConfig = configs.require("core", CoreConfig.class);
        messages.locales(coreConfig.locale.defaultLocale, coreConfig.locale.fallback);
        messages.reload();
        modules.onReload();
        logger.info("CellulosesZ reloaded.");
    }

    public String version() {
        return version;
    }

    public List<LoadedModuleInfo> modules() {
        return modules.modules();
    }

    public CommandRegistry commandRegistry() {
        return commands;
    }

    public ConfigRegistry configRegistry() {
        return configs;
    }

    public PermissionService permissionService() {
        return permissions;
    }

    public MessageService messageService() {
        return messages;
    }

    public CoreConfig coreConfig() {
        return coreConfig;
    }

    public record PlayerJoinEvent(
            Object player
    ) {

    }

    public record PlayerDisconnectEvent(
            Object player
    ) {

    }

}
