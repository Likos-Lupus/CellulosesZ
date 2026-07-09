package top.likoslupus.cellulosesz.modules.user;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.NameCacheService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.modules.user.service.DefaultNameCacheService;
import top.likoslupus.cellulosesz.modules.user.service.JsonUserService;

@CellulosesModule(
        id = "user",
        name = "User",
        description = "User cache and profile foundation.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class UserModule implements CellulosesZModule {

    private UserConfig config;
    private JsonUserService users;

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

        context.services().register(NameCacheService.class, nameCache);
        context.services().register(UserService.class, users);
        context.services().register(JsonUserService.class, users);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        context.events().listen(CellulosesZBootstrap.PlayerJoinEvent.class, event ->
                users.loadFromPlayer(event.player())
                        .thenCompose(user -> users.save(user.uuid))
                        .exceptionally(exception -> {
                            context.logger().error("Failed to load user data for joining player", exception);
                            return null;
                        })
        );
        context.events().listen(CellulosesZBootstrap.PlayerDisconnectEvent.class, event -> {
            users.markQuit(event.player());
            if (config.saveOnQuit) {
                users.saveAll();
            }
        });
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        context.scheduler().syncRepeating(
                () -> users.saveAll(),
                config.autosaveIntervalSeconds * 20L,
                config.autosaveIntervalSeconds * 20L
        );
    }

    @Override
    public void onServerStopping(ModuleContext context) {
        users.saveAll().join();
    }

}
