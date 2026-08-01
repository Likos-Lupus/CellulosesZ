package top.likoslupus.cellulosesz.modules.user;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.player.DisplayNamePlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
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

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

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

    @SuppressWarnings("resource")
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

        requireNonNull(config, "UserConfig has not been initialized");

        var displayNamePlatform = context.services().require(DisplayNamePlatformService.class);
        var players = context.services().require(PlayerDirectory.class);
        var permissions = context.services().require(PermissionService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);
        displayNames = new DefaultDisplayNameService(
                displayNamePlatform,
                players,
                users,
                permissions,
                renderer,
                locales,
                config
        );
        var resolver = new DefaultPlayerResolver(
                players,
                users,
                nameCache,
                permissions,
                displayNames
        );

        context.services().register(
                NameCacheService.class,
                nameCache
        );
        context.services().register(
                UserService.class,
                users
        );
        context.services().register(
                JsonUserService.class,
                users
        );
        context.services().register(
                PlayerResolver.class,
                resolver
        );
        context.services().register(
                DefaultPlayerResolver.class,
                resolver
        );
        context.services().register(
                DisplayNameService.class,
                displayNames
        );
        context.services().register(
                DefaultDisplayNameService.class,
                (DefaultDisplayNameService) displayNames
        );
    }

    @Override
    public void registerEvents(ModuleContext context) {
        requireNonNull(users, "JsonUserService has not been initialized");
        requireNonNull(displayNames, "DisplayNameService has not been initialized");
        requireNonNull(config, "UserConfig has not been initialized");

        context.events().listen(
                PlayerJoinEvent.class,
                event -> users
                        .loadFromPlayer(event.player())
                        .thenApply(user -> {
                            displayNames.refresh(event.player());
                            return user;
                        })
                        .whenComplete((_, exception) -> {
                            if (exception != null) {
                                context.logger()
                                        .error(
                                                "Failed to load user data for joining player",
                                                exception
                                        );
                            }
                        })
        );
        context.events().listen(
                PlayerDisconnectEvent.class,
                event -> {
                    var quit = users.markQuit(event.player());
                    if (config.saveOnQuit) {
                        quit.thenCompose(_ -> users.saveAll());
                    }
                }
        );
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        requireNonNull(users, "JsonUserService has not been initialized");
        requireNonNull(config, "UserConfig has not been initialized");

        context.scheduler().syncRepeating(
                () -> users.saveAll(),
                config.autosaveIntervalSeconds * 20L,
                config.autosaveIntervalSeconds * 20L
        );
    }

    @Override
    public void onReload(ModuleContext context) {
        requireNonNull(displayNames, "DisplayNameService has not been initialized");
        displayNames.refreshAll();
    }

    @Override
    public void onServerStopping(ModuleContext context) {
        requireNonNull(users, "JsonUserService has not been initialized");
    }

}
