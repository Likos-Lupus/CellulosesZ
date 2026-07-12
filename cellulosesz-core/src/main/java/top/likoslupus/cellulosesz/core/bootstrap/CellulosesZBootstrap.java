package top.likoslupus.cellulosesz.core.bootstrap;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.command.service.*;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.LoadedModuleInfo;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.runtime.RuntimeService;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.core.command.DefaultCommandRegistry;
import top.likoslupus.cellulosesz.core.command.service.*;
import top.likoslupus.cellulosesz.core.config.CoreConfig;
import top.likoslupus.cellulosesz.core.config.JacksonConfigRegistry;
import top.likoslupus.cellulosesz.core.event.SimpleEventRegistry;
import top.likoslupus.cellulosesz.core.i18n.DefaultLocaleResolver;
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
import java.util.Objects;

public final class CellulosesZBootstrap {

    private final Path configDirectory;
    private final String version;
    private final CellulosesZLogger logger;
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();
    private final JacksonConfigRegistry configs;
    private final SimpleEventRegistry events = new SimpleEventRegistry();
    private final DefaultScheduler scheduler = new DefaultScheduler();
    private final DefaultPermissionCatalog permissionCatalog = new DefaultPermissionCatalog();
    private final DefaultCommandAliasRegistry aliasRegistry = new DefaultCommandAliasRegistry();
    private final DefaultCommandSuggestionRegistry suggestionRegistry = new DefaultCommandSuggestionRegistry();
    private final DefaultCommandRegistry commands = new DefaultCommandRegistry(permissionCatalog, aliasRegistry);
    private final DefaultPermissionService permissions = new DefaultPermissionService();
    private final JacksonStorageService storage;
    private final DefaultMessageService messages;
    private final DefaultCooldownService cooldowns = new DefaultCooldownService(services);
    private final DefaultConfirmationService confirmations = new DefaultConfirmationService();
    private final DefaultCommandCostService commandCosts = new DefaultCommandCostService(services);
    private @Nullable DefaultLocaleResolver localeResolver;
    private @Nullable DefaultModuleManager modules;
    private @Nullable CoreConfig coreConfig;
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
        messages.theme(coreConfig.locale.primaryColor, coreConfig.locale.secondaryColor, coreConfig.locale.legacyColors);
        messages.reload();

        var platform = services.require(PlatformService.class);
        localeResolver = new DefaultLocaleResolver(platform, coreConfig.locale.defaultLocale, coreConfig.locale.useClientLocale);
        commandCosts.configure(coreConfig.commands.costs);
        aliasRegistry.configure(coreConfig.commands.aliases);

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
        services.register(MessageRenderer.class, messages);
        services.register(LocaleResolver.class, localeResolver);
        services.register(PermissionCatalog.class, permissionCatalog);
        services.register(CommandAliasRegistry.class, aliasRegistry);
        services.register(CommandSuggestionRegistry.class, suggestionRegistry);
        services.register(CooldownService.class, cooldowns);
        services.register(ConfirmationService.class, confirmations);
        services.register(CommandCostService.class, commandCosts);
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
        requireModules().onServerStarting();
    }

    private DefaultModuleManager requireModules() {
        return Objects.requireNonNull(modules, "CellulosesZ is not initialized");
    }

    public void onServerStarted(Object server) {
        logger.info("CellulosesZ server started.");
        requireModules().onServerStarted();
    }

    public void onServerStopping(Object server) {
        logger.info("CellulosesZ server stopping.");
        requireModules().onServerStopping();
        scheduler.close();
    }

    public void onPlayerJoin(Object player) {
        services.require(PlatformService.class)
                .player(player)
                .ifPresent(wrapped -> events.fire(new PlayerJoinEvent(wrapped)));
    }

    public void onPlayerDisconnect(Object player) {
        services.require(PlatformService.class)
                .player(player)
                .ifPresent(wrapped -> events.fire(new PlayerDisconnectEvent(wrapped)));
    }

    public void tick() {
        scheduler.tick();
    }

    public synchronized void reload() {
        configs.reload();
        coreConfig = configs.require("core", CoreConfig.class);
        messages.locales(coreConfig.locale.defaultLocale, coreConfig.locale.fallback);
        messages.theme(coreConfig.locale.primaryColor, coreConfig.locale.secondaryColor, coreConfig.locale.legacyColors);
        messages.reload();
        requireLocaleResolver().configure(coreConfig.locale.defaultLocale, coreConfig.locale.useClientLocale);
        commandCosts.configure(coreConfig.commands.costs);
        aliasRegistry.configure(coreConfig.commands.aliases);
        requireModules().onReload();
        services.optional(CommandTreeService.class).ifPresent(CommandTreeService::refresh);
        logger.info("CellulosesZ reloaded.");
    }

    private DefaultLocaleResolver requireLocaleResolver() {
        return Objects.requireNonNull(localeResolver, "CellulosesZ is not initialized");
    }

    public String version() {
        return version;
    }

    public List<LoadedModuleInfo> modules() {
        return requireModules().modules();
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

    public ServiceRegistry serviceRegistry() {
        return services;
    }

    public EventRegistry eventRegistry() {
        return events;
    }

    public MessageService messageService() {
        return messages;
    }

    public CellulosesZLogger logger() {
        return logger;
    }

    public CoreConfig coreConfig() {
        return Objects.requireNonNull(coreConfig, "CellulosesZ is not initialized");
    }

}
