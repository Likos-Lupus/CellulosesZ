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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public final class CellulosesZBootstrap {

    private final Path configDirectory;
    private final String version;
    private final CellulosesZLogger logger;
    private final DefaultServiceRegistry services = new DefaultServiceRegistry();
    private final JacksonConfigRegistry configs;
    private final SimpleEventRegistry events = new SimpleEventRegistry();
    private final DefaultScheduler scheduler;
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
    private final AtomicBoolean reloadRunning = new AtomicBoolean();
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
        this.scheduler = new DefaultScheduler(logger);
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
        awaitLifecycle("module initialization", modules.loadAsync());
        initialized = true;
    }

    private void awaitLifecycle(String operation, CompletableFuture<Void> future) {
        try {
            future.orTimeout(60, TimeUnit.SECONDS).join();
        } catch (RuntimeException failure) {
            logger.error("CellulosesZ " + operation + " failed.", failure);
            throw failure;
        }
    }

    public void permissionBackend(PermissionBackend backend) {
        permissions.backend(backend);
    }

    public void onServerStarting(Object server) {
        logger.info("CellulosesZ server starting.");
        requireModules().onServerStarting();
    }

    private DefaultModuleManager requireModules() {
        return requireNonNull(modules, "CellulosesZ is not initialized");
    }

    public void onServerStarted(Object server) {
        logger.info("CellulosesZ server started.");
        requireModules().onServerStarted();
    }

    public void onServerStopping(Object server) {
        logger.info("CellulosesZ server stopping.");
        awaitLifecycle("module shutdown", requireModules().onServerStoppingAsync());
        awaitLifecycle("storage shutdown", storage.closeAsync());
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

    public CompletableFuture<Void> reload() {
        if (!reloadRunning.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A reload is already in progress"));
        }

        var platform = services.require(PlatformService.class);
        return scheduler.async(() -> {
                    var preparedConfigs = configs.prepareReload();
                    var candidateCore = preparedConfigs.require("core", CoreConfig.class);
                    var preparedMessages = messages.prepareReload(
                            candidateCore.locale.defaultLocale,
                            candidateCore.locale.fallback,
                            candidateCore.locale.primaryColor,
                            candidateCore.locale.secondaryColor,
                            candidateCore.locale.legacyColors
                    );
                    return new ReloadPlan(preparedConfigs, preparedMessages, candidateCore);
                }).thenCompose(plan -> platform.runOnServerThreadAsync(() -> applyReload(plan)))
                .whenComplete((ignored, _) -> reloadRunning.set(false));
    }

    private synchronized void applyReload(ReloadPlan plan) {
        var previousConfigs = configs.snapshot();
        var previousMessages = messages.snapshot();
        var previousCore = coreConfig();

        try {
            configs.commit(plan.configs());
            coreConfig = plan.core();
            messages.commit(
                    plan.messages(),
                    coreConfig.locale.defaultLocale,
                    coreConfig.locale.fallback,
                    coreConfig.locale.primaryColor,
                    coreConfig.locale.secondaryColor,
                    coreConfig.locale.legacyColors
            );
            applyCoreRuntimeConfiguration(coreConfig);
            requireModules().onReload();
            services.optional(CommandTreeService.class).ifPresent(CommandTreeService::refresh);
            logger.info("CellulosesZ reloaded.");
        } catch (RuntimeException failure) {
            configs.restore(previousConfigs);
            messages.restore(previousMessages);
            coreConfig = previousCore;
            try {
                applyCoreRuntimeConfiguration(previousCore);
                requireModules().onReload();
                services.optional(CommandTreeService.class).ifPresent(CommandTreeService::refresh);
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    public CoreConfig coreConfig() {
        return requireNonNull(coreConfig, "CellulosesZ is not initialized");
    }

    private void applyCoreRuntimeConfiguration(CoreConfig config) {
        requireLocaleResolver().configure(config.locale.defaultLocale, config.locale.useClientLocale);
        commandCosts.configure(config.commands.costs);
        aliasRegistry.configure(config.commands.aliases);
    }

    private DefaultLocaleResolver requireLocaleResolver() {
        return requireNonNull(localeResolver, "CellulosesZ is not initialized");
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

    private record ReloadPlan(
            JacksonConfigRegistry.ReloadSnapshot configs,
            DefaultMessageService.PreparedMessages messages,
            CoreConfig core
    ) {

    }

}
