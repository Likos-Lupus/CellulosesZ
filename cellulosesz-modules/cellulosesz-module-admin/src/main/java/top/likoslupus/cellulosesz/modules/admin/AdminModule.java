package top.likoslupus.cellulosesz.modules.admin;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.modules.admin.command.*;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.*;

@CellulosesModule(
        id = "admin",
        name = "Admin",
        description = "Administration, punishments, mute, and jail services.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class AdminModule implements CellulosesZModule {

    private @Nullable AdminConfig config;
    private @Nullable BanService bans;
    private @Nullable TempBanService tempBans;
    private @Nullable MuteService mutes;
    private @Nullable JailService jails;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.admin",
                AdminConfig.class,
                "modules/admin.yml",
                AdminConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var storage = context.services().require(StorageService.class);
        var users = context.services().require(UserService.class);
        var root = context.dataDirectory().getParent().resolve("admin");

        bans = new DefaultBanService(platform);
        tempBans = new JsonTempBanService(storage, root.resolve("temp-bans.json"), platform, users);
        mutes = new JsonMuteService(storage, root.resolve("mutes.json"), users);
        jails = new JsonJailService(storage, root.resolve("jails.json"), platform);

        context.services().register(BanService.class, bans);
        context.services().register(TempBanService.class, tempBans);
        context.services().register(MuteService.class, mutes);
        context.services().register(JailService.class, jails);

        context.services().require(CommandMiddlewareRegistry.class)
                .addMiddleware(new MuteCommandMiddleware(platform, mutes));
    }

    @Override
    public void registerEvents(ModuleContext context) {
        context.events().listen(CellulosesZBootstrap.PlayerJoinEvent.class, event -> {
            var platform = context.services().require(PlatformService.class);
            platform.player(event.player())
                    .ifPresent(player -> {
                        tempBans.active(player.uuid(), player.name())
                                .ifPresent(record ->
                                        platform.kick(player, "临时封禁: " + record.reason)
                                );
                        jails.jailed(player.uuid())
                                .flatMap(record -> jails.jail(record.jail))
                                .ifPresent(jail ->
                                        platform.teleport(player, jail.location)
                                );
                    });
        });
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        context.commands().register(new BanCommand(platform, users, bans));
        context.commands().register(new TempBanCommand(platform, users, tempBans));
        context.commands().register(new MuteCommand(platform, users, mutes, config));
        context.commands().register(new KickCommand(platform, users, bans));
        context.commands().register(new JailCommand(platform, users, jails, config));
        context.commands().register(new SetJailCommand(platform, users, jails));
        context.commands().register(new DelJailCommand(platform, users, jails));
        context.commands().register(new JailsCommand(platform, users, jails));
        context.commands().register(new JailedPlayersCommand(platform, users, jails));
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        context.scheduler().syncRepeating(() -> {
            tempBans.purgeExpired();
            mutes.purgeExpired();
            jails.purgeExpired();
        }, 20L, Math.max(20L, config.jailedPlayerCheckSeconds * 20L));
    }

}
