package top.likoslupus.cellulosesz.modules.playerstate;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerJoinEvent;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.playerstate.command.*;
import top.likoslupus.cellulosesz.modules.playerstate.config.PlayerStateConfig;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultPlayerStateService;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultVanishService;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "playerstate",
        name = "PlayerState",
        description = "Player state commands including persistent network-level vanish.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "permission", "command"}
)
public final class PlayerStateModule implements CellulosesZModule {

    private @Nullable PlayerStateConfig config;
    private @Nullable PlayerStateService states;
    private @Nullable VanishService vanish;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.playerstate",
                PlayerStateConfig.class,
                "modules/playerstate.yml",
                PlayerStateConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var permissions = context.services().require(PermissionService.class);
        var displayNames = context.services().require(DisplayNameService.class);

        states = new DefaultPlayerStateService(platform, users, displayNames);
        vanish = new DefaultVanishService(platform, users, permissions, displayNames);

        context.services().register(PlayerStateService.class, states);
        context.services().register(DefaultPlayerStateService.class, (DefaultPlayerStateService) states);
        context.services().register(VanishService.class, vanish);
        context.services().register(DefaultVanishService.class, (DefaultVanishService) vanish);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        context.events().listen(PlayerJoinEvent.class, event ->
                restoreJoinedState(context, event.player(), 0)
        );
        context.events().listen(PlayerDisconnectEvent.class, event ->
                context.services().require(PlatformService.class)
                        .setVanishedState(event.player(), false)
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        requireNonNull(states, "PlayerStateService has not been initialized");
        requireNonNull(vanish, "VanishService has not been initialized");

        context.commands().register(new FlyCommand(platform, users, states));
        context.commands().register(new GodCommand(platform, users, states));
        context.commands().register(new HealCommand(platform, users, states));
        context.commands().register(new FeedCommand(platform, users, states));
        context.commands().register(new AfkCommand(platform, users, states));
        context.commands().register(new NickCommand(
                platform,
                users,
                states,
                context.services().require(DisplayNameService.class)
        ));
        context.commands().register(new VanishCommand(
                platform,
                users,
                states,
                vanish,
                context.services().require(MessageRenderer.class),
                context.services().require(LocaleResolver.class)
        ));
    }

    private void restoreJoinedState(
            ModuleContext context,
            CellPlayer player,
            int attempt
    ) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        if (platform.onlinePlayers().stream()
                .noneMatch(online -> online.uuid().equals(player.uuid()))
        ) return;

        var loaded = users.cached(player.uuid());
        if (loaded.isEmpty()) {
            if (attempt < 100) {
                context.scheduler().syncLater(
                        () -> restoreJoinedState(context, player, attempt + 1),
                        1L
                );
            } else {
                context.logger().warn("Timed out waiting for player data before restoring state: " + player.name());
            }
            return;
        }

        requireNonNull(config, "PlayerStateConfig has not been initialized");
        requireNonNull(states, "PlayerStateService has not been initialized");
        requireNonNull(vanish, "VanishService has not been initialized");

        var user = loaded.get();
        context.services().require(DisplayNameService.class).refresh(player);
        if (config.persistFlyGod) {
            if (user.state.flying) states.setFlying(player, true);
            if (user.state.god) states.setGod(player, true);
        }

        if (user.state.vanished && config.persistVanish) {
            vanish.setVanished(player, true);
        } else {
            if (user.state.vanished) {
                user.state.vanished = false;
                users.markDirty(player.uuid());
            }
            platform.setVanishedState(player, false);
        }

        vanish.synchronizeViewer(player);
    }

}
