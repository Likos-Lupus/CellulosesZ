package top.likoslupus.cellulosesz.modules.admin;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionContext;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.event.*;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.platform.admin.BanPlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
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
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "admin",
        name = "Admin",
        description = "Administration, punishments, mute, and jail services.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command", "permission"}
)
public final class AdminModule implements CellulosesZModule {

    private @Nullable AdminConfig config;
    private @Nullable BanService bans;
    private @Nullable TempBanService tempBans;
    private @Nullable MuteService mutes;
    private @Nullable JailService jails;
    private @Nullable AddressBookService addresses;
    private @Nullable MuteCommandMiddleware muteCommandPolicy;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.admin",
                AdminConfig.class,
                "modules/admin.yml",
                AdminConfig::new
        );
        requireNonNull(config, "AdminConfig has not been initialized").validate();
    }

    @SuppressWarnings("resource")
    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var banPlatform = context.services().require(BanPlatformService.class);
        var storage = context.services().require(StorageService.class);
        var users = context.services().require(UserService.class);
        var root = context.dataDirectory().getParent().resolve("admin");
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);
        var permissions = context.services().require(PermissionService.class);

        requireNonNull(config, "AdminConfig has not been initialized");

        bans = new DefaultBanService(platform, banPlatform, renderer, locales, permissions);
        tempBans = new JsonTempBanService(
                storage,
                root.resolve("temp-bans.json"),
                platform,
                users,
                renderer,
                locales,
                config.tempBanKickOnlinePlayers
        );
        addresses = new JsonAddressBookService(storage, root.resolve("addresses.json"));
        mutes = new JsonMuteService(storage, root.resolve("mutes.json"));
        jails = new JsonJailService(storage, root.resolve("jails.json"), platform, config);

        context.services().register(BanService.class, bans);
        context.services().register(TempBanService.class, tempBans);
        context.services().register(AddressBookService.class, addresses);
        context.services().register(MuteService.class, mutes);
        context.services().register(JailService.class, jails);

        muteCommandPolicy = new MuteCommandMiddleware(mutes, config);
        context.services().require(CommandMiddlewareRegistry.class).addMiddleware(muteCommandPolicy);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);
        var permissions = context.services().require(PermissionService.class);

        requireNonNull(tempBans, "TempBanService has not been initialized");
        requireNonNull(mutes, "MuteService has not been initialized");
        requireNonNull(jails, "JailService has not been initialized");

        context.events().listen(PlayerJoinEvent.class, event -> {
            var player = event.player();
            var address = platform.address(player);
            address.ifPresent(value -> requireNonNull(
                    addresses, "AddressBookService has not been initialized")
                    .remember(player.uuid(), player.name(), value)
                    .whenComplete((ignored, failure) -> {
                        if (failure != null) {
                            context.logger().error(
                                    "Failed to persist a player login address", failure);
                        }
                    }));

            var active = tempBans.active(player.uuid(), player.name())
                    .or(() -> address.flatMap(tempBans::activeIp));
            if (active.isPresent()) {
                platform.kick(
                        player,
                        renderer.render(
                                locales.locale(player),
                                "service.admin.temp-ban-kick",
                                Map.of("reason", active.orElseThrow().reason())
                        ).plainText()
                );
                return;
            }
            enforceJail(platform, jails, player);
        });

        context.events().listen(PlayerChatEvent.class, event -> {
            if (permissions.has(event.player().nativeHandle(), "cellulosesz.admin.mute.bypass")
                    || !mutes.muted(event.player().uuid())
            ) return;

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

        context.events().listen(PlayerCommandPreprocessEvent.class, event -> {
            if (permissions.has(event.player().nativeHandle(), "cellulosesz.admin.mute.bypass")
                    || !mutes.muted(event.player().uuid())
            ) return;

            var raw = event.command().trim();
            while (raw.startsWith("/")) raw = raw.substring(1);

            var separator = raw.indexOf(' ');
            var root = separator < 0 ? raw : raw.substring(0, separator);
            var namespace = root.indexOf(':');
            if (namespace >= 0) root = root.substring(namespace + 1);

            requireNonNull(muteCommandPolicy, "MuteCommandMiddleware has not been initialized");
            if (!muteCommandPolicy.blocked(root)) return;

            event.cancel();
            platform.sendMessage(event.player(), renderer.render(
                    locales.locale(event.player()),
                    "commands.admin.mute-command-middleware.error.muted-cannot-use-command",
                    Map.of()
            ));
        });

        context.events().listen(PlayerMoveEvent.class, event ->
                jail(jails, event.player())
                        .ifPresent(jail -> {
                            if (!inside(jail.location, event.to(), jailRadius())) {
                                event.to(jail.location);
                                event.cancel();
                            }
                        })
        );
        context.events().listen(PlayerRespawnEvent.class, event ->
                jail(jails, event.player())
                        .ifPresent(jail -> event.location(jail.location))
        );
        context.events().listen(PlayerWorldChangeEvent.class, event ->
                enforceJail(platform, jails, event.player())
        );
        context.events().listen(PlayerGameModeChangeEvent.class, event -> {
            if (jails.jailed(event.player().uuid()).isPresent()) {
                event.cancel();
            }
        });
        context.events().listen(PlayerAttackEvent.class, event -> {
            if (jails.jailed(event.player().uuid()).isPresent()) {
                event.cancel();
            }
        });
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        requireNonNull(bans, "BanService has not been initialized");
        requireNonNull(tempBans, "TempBanService has not been initialized");
        requireNonNull(mutes, "MuteService has not been initialized");
        requireNonNull(config, "Config has not been initialized");
        requireNonNull(jails, "JailService has not been initialized");
        requireNonNull(addresses, "AddressBookService has not been initialized");

        context.commands().register(new BanCommand(platform, users, bans));
        context.commands().register(new BanIpCommand(platform, users, bans, addresses));
        context.commands().register(new TempBanCommand(platform, users, tempBans, config));
        context.commands().register(new TempBanIpCommand(platform, users, tempBans, addresses, config));
        context.commands().register(new UnbanCommand(platform, users, bans, tempBans));
        context.commands().register(new UnbanIpCommand(bans, tempBans));
        context.commands().register(new KickAllCommand(bans));
        context.commands().register(new MuteCommand(platform, users, mutes, config));
        context.commands().register(new KickCommand(platform, users, bans));
        context.commands().register(new JailCommand(platform, users, jails, config));
        context.commands().register(new SetJailCommand(platform, users, jails));
        context.commands().register(new DelJailCommand(platform, users, jails));
        context.commands().register(new JailsCommand(platform, users, jails));
        context.commands().register(new JailedPlayersCommand(platform, users, jails));

        var playerOperations = context.services().require(PlayerStatePlatformService.class);
        var permissionService = context.services().require(PermissionService.class);
        context.commands().register(new BurnCommand(playerOperations, config));
        context.commands().register(new ExtCommand(platform, playerOperations));
        context.commands().register(new IceCommand(platform, playerOperations));
        context.commands().register(new KillCommand(playerOperations, permissionService));
        context.commands().register(new SudoCommand(
                platform,
                context.services().require(PlayerCommandDispatchService.class),
                permissionService,
                config
        ));
        context.commands().register(new SuicideCommand(platform, playerOperations));
        registerCommandPermissions(context.services().require(PermissionCatalog.class));

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
        var platform = context.services().require(PlatformService.class);

        requireNonNull(tempBans, "TempBanService has not been initialized");
        requireNonNull(mutes, "MuteService has not been initialized");
        requireNonNull(config, "Config has not been initialized");
        requireNonNull(jails, "JailService has not been initialized");

        context.scheduler().syncRepeating(
                () -> {
                    tempBans.purgeExpired().whenComplete((ignored, failure) -> {
                        if (failure != null) context.logger().error("Failed to purge expired temporary bans", failure);
                    });
                    mutes.purgeExpired().whenComplete((ignored, failure) -> {
                        if (failure != null) context.logger().error("Failed to purge expired mutes", failure);
                    });
                    jails.purgeExpired().whenComplete((ignored, failure) -> {
                        if (failure != null) context.logger().error("Failed to purge expired jail records", failure);
                    });
                    platform.onlinePlayers()
                            .forEach(player ->
                                    enforceJail(platform, jails, player)
                            );
                },
                20L,
                Math.max(20L, config.jailedPlayerCheckSeconds * 20L)
        );
    }

    @Override
    public void onReload(ModuleContext context) {
        var current = requireNonNull(config, "AdminConfig has not been initialized");
        var candidate = context.configs().require("module.admin", AdminConfig.class);
        candidate.validate();
        current.copyFrom(candidate);
        requireNonNull(muteCommandPolicy, "MuteCommandMiddleware has not been initialized")
                .configure(current);
    }

    private static void registerCommandPermissions(PermissionCatalog catalog) {
        Map.ofEntries(
                Map.entry("cellulosesz.command.ext.others", "Extinguish other players"),
                Map.entry("cellulosesz.command.ice.others", "Freeze other players"),
                Map.entry("cellulosesz.command.kill.exempt", "Exempt a player from administrative kill"),
                Map.entry("cellulosesz.command.kill.force", "Bypass kill exemptions and a prevented death"),
                Map.entry("cellulosesz.command.sudo.exempt", "Exempt a player from sudo")
        ).forEach(catalog::register);
    }

    private void enforceJail(
            PlatformService platform,
            JailService jailService,
            CellPlayer player
    ) {
        jail(jailService, player)
                .ifPresent(jail -> {
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

    private boolean inside(
            CellLocation jail,
            CellLocation actual,
            double radius
    ) {
        if (!jail.world.equals(actual.world)) return false;
        var dx = jail.x - actual.x;
        var dy = jail.y - actual.y;
        var dz = jail.z - actual.z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private double jailRadius() {
        return requireNonNull(config, "Config has not been initialized").jailConfinementRadius;
    }

}
