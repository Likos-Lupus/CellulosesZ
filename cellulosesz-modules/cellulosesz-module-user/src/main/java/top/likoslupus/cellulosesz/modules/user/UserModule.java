package top.likoslupus.cellulosesz.modules.user;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.player.DisplayNamePlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.user.service.DefaultDisplayNameService;
import top.likoslupus.cellulosesz.modules.user.service.DefaultNameCacheService;
import top.likoslupus.cellulosesz.modules.user.service.DefaultPlayerResolver;
import top.likoslupus.cellulosesz.modules.user.service.JsonUserService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "user",
        name = "User",
        description = "User cache and profile foundation.",
        phase = ModulePhase.FEATURE,
        requires = {"command", "permission"}
)
@SuppressWarnings("resource")
public final class UserModule implements CellulosesZModule {

    private volatile @Nullable UserConfig config;
    private @Nullable JsonUserService users;
    private @Nullable DefaultDisplayNameService displayNames;
    private @Nullable TaskHandle autosave;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.user",
                UserConfig.class,
                "modules/user.yml",
                UserConfig::new
        );
        config = context.configs().require("module.user", UserConfig.class);
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

        var current = validate(requireNonNull(config, "UserConfig has not been initialized"));
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
                current
        );
        var resolver = new DefaultPlayerResolver(
                players,
                users,
                nameCache,
                permissions,
                displayNames
        );

        context.services().register(NameCacheService.class, nameCache);
        context.services().register(UserService.class, users);
        context.services().register(JsonUserService.class, users);
        context.services().register(PlayerResolver.class, resolver);
        context.services().register(DefaultPlayerResolver.class, resolver);
        context.services().register(DisplayNameService.class, displayNames);
        context.services().register(DefaultDisplayNameService.class, displayNames);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var userService = requireNonNull(
                users,
                "JsonUserService has not been initialized"
        );
        var displayNameService = requireNonNull(
                displayNames,
                "DisplayNameService has not been initialized"
        );

        context.events().listen(
                PlayerJoinEvent.class,
                event -> {
                    var current = requireNonNull(
                            config,
                            "UserConfig has not been initialized"
                    );
                    userService
                            .loadFromPlayer(event.player(), current.updateNameCacheOnJoin)
                            .thenApply(user -> {
                                displayNameService.refresh(event.player());
                                return user;
                            })
                            .whenComplete((_, exception) -> {
                                if (exception != null) {
                                    context.logger().error(
                                            "Failed to load user data for joining player",
                                            exception
                                    );
                                }
                            });
                }
        );
        context.events().listen(
                PlayerDisconnectEvent.class,
                event -> {
                    var current = requireNonNull(
                            config,
                            "UserConfig has not been initialized"
                    );
                    if (!current.saveOnQuit) {
                        return;
                    }
                    userService.markQuit(event.player()).whenComplete((_, failure) -> {
                        if (failure != null) {
                            context.logger().error("Failed to persist quitting user", failure);
                        }
                    });
                }
        );
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        scheduleAutosave(context);
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previous = requireNonNull(config, "UserConfig has not been initialized");
        var candidate = validate(reload.configs().require("module.user", UserConfig.class));
        var displayNameService = requireNonNull(
                displayNames,
                "DisplayNameService has not been initialized"
        );
        displayNameService.validateConfiguration(candidate);

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    displayNameService.configure(candidate);
                    config = candidate;
                    scheduleAutosave(context);
                    displayNameService.refreshAll();
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    displayNameService.configure(previous);
                    config = previous;
                    scheduleAutosave(context);
                    displayNameService.refreshAll();
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    private void scheduleAutosave(ModuleContext context) {
        var userService = requireNonNull(users, "JsonUserService has not been initialized");
        var current = requireNonNull(config, "UserConfig has not been initialized");
        if (autosave != null) {
            autosave.close();
        }

        var period = Math.multiplyExact(current.autosaveIntervalSeconds, 20L);
        autosave = context.scope().own(context.scheduler().syncRepeating(
                () -> userService.saveAll().whenComplete((_, failure) -> {
                    if (failure != null) {
                        context.logger().error("Failed to autosave users", failure);
                    }
                }),
                period,
                period,
                context.scope().owner()
        ));
    }

    private UserConfig validate(UserConfig candidate) {
        requireNonNull(candidate, "config");
        if (candidate.autosaveIntervalSeconds <= 0) {
            throw new IllegalArgumentException("User autosave interval must be positive");
        }
        //noinspection ResultOfMethodCallIgnored
        Math.multiplyExact(candidate.autosaveIntervalSeconds, 20L);
        return candidate;
    }

}
