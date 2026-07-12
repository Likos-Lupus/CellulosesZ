package top.likoslupus.cellulosesz.modules.user;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.user.service.DefaultDisplayNameService;
import top.likoslupus.cellulosesz.modules.user.service.DefaultNameCacheService;
import top.likoslupus.cellulosesz.modules.user.service.DefaultPlayerResolver;
import top.likoslupus.cellulosesz.modules.user.service.JsonUserService;

import java.util.Objects;

@CellulosesModule(
        id = "user",
        name = "User",
        description = "User cache and profile foundation.",
        phase = ModulePhase.FEATURE,
        requires = {"command", "permission"}
)
public final class UserModule implements CellulosesZModule {

    private @Nullable UserConfig config;
    private @Nullable JsonUserService users;
    private @Nullable DisplayNameService displayNames;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.user",
                UserConfig.class,
                "modules/user.yml",
                UserConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent();
        var nameCache = new DefaultNameCacheService(
                storage,
                root.resolve("runtime/name-cache.json")
        );
        users = new JsonUserService(
                storage,
                nameCache,
                root.resolve("users"),
                context.logger()
        );

        Objects.requireNonNull(config, "UserConfig has not been initialized");

        var platform = context.services().require(PlatformService.class);
        var permissions = context.services().require(PermissionService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);
        displayNames = new DefaultDisplayNameService(platform, users, permissions, renderer, locales, config);
        var resolver = new DefaultPlayerResolver(platform, users, nameCache, permissions, displayNames);

        context.services().register(NameCacheService.class, nameCache);
        context.services().register(UserService.class, users);
        context.services().register(JsonUserService.class, users);
        context.services().register(PlayerResolver.class, resolver);
        context.services().register(DefaultPlayerResolver.class, resolver);
        context.services().register(DisplayNameService.class, displayNames);
        context.services().register(DefaultDisplayNameService.class, (DefaultDisplayNameService) displayNames);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        Objects.requireNonNull(users, "JsonUserService has not been initialized");
        Objects.requireNonNull(displayNames, "DisplayNameService has not been initialized");
        Objects.requireNonNull(config, "UserConfig has not been initialized");

        context.events().listen(PlayerJoinEvent.class, event ->
                users.loadFromPlayer(event.player().nativeHandle())
                        .thenApply(user -> {
                            displayNames.refresh(event.player());
                            return user;
                        })
                        .thenCompose(user -> users.save(user.uuid))
                        .whenComplete((_, exception) -> {
                            if (exception != null) {
                                context.logger().error("Failed to load user data for joining player", exception);
                            }
                        })
        );
        context.events().listen(PlayerDisconnectEvent.class, event -> {
            users.markQuit(event.player().nativeHandle());
            if (config.saveOnQuit) {
                users.saveAll();
            }
        });
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        Objects.requireNonNull(users, "JsonUserService has not been initialized");
        Objects.requireNonNull(config, "UserConfig has not been initialized");

        context.scheduler().syncRepeating(
                () -> users.saveAll(),
                config.autosaveIntervalSeconds * 20L,
                config.autosaveIntervalSeconds * 20L
        );
    }

    @Override
    public void onReload(ModuleContext context) {
        Objects.requireNonNull(displayNames, "DisplayNameService has not been initialized");
        displayNames.refreshAll();
    }

    @Override
    public void onServerStopping(ModuleContext context) {
        Objects.requireNonNull(users, "JsonUserService has not been initialized");
        users.saveAll().join();
    }

}
