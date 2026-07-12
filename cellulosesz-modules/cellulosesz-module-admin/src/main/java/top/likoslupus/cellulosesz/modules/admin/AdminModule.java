package top.likoslupus.cellulosesz.modules.admin;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionContext;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;
import top.likoslupus.cellulosesz.api.event.*;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.admin.command.*;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.*;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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

        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);
        bans = new DefaultBanService(platform, renderer, locales);
        tempBans = new JsonTempBanService(
                storage,
                root.resolve("temp-bans.json"),
                platform,
                users,
                renderer,
                locales
        );
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
        var platform = context.services().require(PlatformService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);
        var tempBanService = Objects.requireNonNull(tempBans, "TempBanService has not been initialized");
        var muteService = Objects.requireNonNull(mutes, "MuteService has not been initialized");
        var jailService = Objects.requireNonNull(jails, "JailService has not been initialized");

        context.events().listen(PlayerJoinEvent.class, event -> {
            var player = event.player();
            tempBanService.active(player.uuid(), player.name())
                    .ifPresent(record -> platform.kick(
                            player,
                            renderer.render(
                                    locales.locale(player),
                                    "service.admin.temp-ban-kick",
                                    Map.of("reason", record.reason)
                            ).plainText()
                    ));
            enforceJail(platform, jailService, player);
        });

        context.events().listen(PlayerChatEvent.class, event -> {
            if (!muteService.muted(event.player().uuid())) return;
            event.cancel();
            platform.sendMessage(
                    event.player(),
                    renderer.render(
                            locales.locale(event.player()),
                            "service.admin.muted-chat",
                            Map.of()
                    )
            );
        });

        context.events().listen(PlayerMoveEvent.class, event ->
                jail(jailService, event.player()).ifPresent(jail -> {
                    if (!inside(jail.location, event.to(), jailRadius())) {
                        event.to(jail.location);
                        event.cancel();
                    }
                })
        );
        context.events().listen(PlayerRespawnEvent.class, event ->
                jail(jailService, event.player())
                        .ifPresent(jail -> event.location(jail.location))
        );
        context.events().listen(PlayerWorldChangeEvent.class, event ->
                enforceJail(platform, jailService, event.player())
        );
        context.events().listen(PlayerGameModeChangeEvent.class, event -> {
            if (jailService.jailed(event.player().uuid()).isPresent()) {
                event.cancel();
            }
        });
        context.events().listen(PlayerAttackEvent.class, event -> {
            if (jailService.jailed(event.player().uuid()).isPresent()) {
                event.cancel();
            }
        });
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        Objects.requireNonNull(bans, "BanService has not been initialized");
        Objects.requireNonNull(tempBans, "TempBanService has not been initialized");
        Objects.requireNonNull(mutes, "MuteService has not been initialized");
        Objects.requireNonNull(config, "Config has not been initialized");
        Objects.requireNonNull(jails, "JailService has not been initialized");

        context.commands().register(new BanCommand(platform, users, bans));
        context.commands().register(new TempBanCommand(platform, users, tempBans));
        context.commands().register(new MuteCommand(platform, users, mutes, config));
        context.commands().register(new KickCommand(platform, users, bans));
        context.commands().register(new JailCommand(platform, users, jails, config));
        context.commands().register(new SetJailCommand(platform, users, jails));
        context.commands().register(new DelJailCommand(platform, users, jails));
        context.commands().register(new JailsCommand(platform, users, jails));
        context.commands().register(new JailedPlayersCommand(platform, users, jails));

        var suggestions = context.services().require(CommandSuggestionRegistry.class);
        var jailNames = (Function<CommandSuggestionContext, Collection<String>>) _ ->
                jails.jails().stream()
                        .map(jail -> jail.name)
                        .toList();
        suggestions.register("jail", "jail", jailNames);
        suggestions.register("deljail", "name", jailNames);
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        var tempBanService = Objects.requireNonNull(tempBans, "TempBanService has not been initialized");
        var muteService = Objects.requireNonNull(mutes, "MuteService has not been initialized");
        var adminConfig = Objects.requireNonNull(config, "Config has not been initialized");
        var jailService = Objects.requireNonNull(jails, "JailService has not been initialized");
        var platform = context.services().require(PlatformService.class);

        context.scheduler().syncRepeating(() -> {
            tempBanService.purgeExpired();
            muteService.purgeExpired();
            jailService.purgeExpired();
            platform.onlinePlayers().forEach(player -> enforceJail(platform, jailService, player));
        }, 20L, Math.max(20L, adminConfig.jailedPlayerCheckSeconds * 20L));
    }

    private void enforceJail(
            PlatformService platform,
            JailService jailService,
            CellPlayer player
    ) {
        jail(jailService, player).ifPresent(jail -> {
            if (!inside(jail.location, platform.location(player), jailRadius())) {
                platform.teleport(player, jail.location);
            }
        });
    }

    private Optional<Jail> jail(
            JailService jailService,
            CellPlayer player
    ) {
        return jailService.jailed(player.uuid())
                .flatMap(record -> jailService.jail(record.jail));
    }

    private boolean inside(CellLocation jail, CellLocation actual, double radius) {
        if (!jail.world.equals(actual.world)) return false;
        var dx = jail.x - actual.x;
        var dy = jail.y - actual.y;
        var dz = jail.z - actual.z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private double jailRadius() {
        return Objects.requireNonNull(config, "Config has not been initialized").jailConfinementRadius;
    }

}
