package top.likoslupus.cellulosesz.modules.playerstate;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.modules.playerstate.command.*;
import top.likoslupus.cellulosesz.modules.playerstate.config.PlayerStateConfig;
import top.likoslupus.cellulosesz.modules.playerstate.service.DefaultPlayerStateService;

@CellulosesModule(
        id = "playerstate",
        name = "PlayerState",
        description = "Player state commands such as fly, god, heal, feed, AFK, and nick.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class PlayerStateModule implements CellulosesZModule {

    private @Nullable PlayerStateConfig config;
    private @Nullable PlayerStateService states;

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
        states = new DefaultPlayerStateService(platform, users);
        context.services().register(PlayerStateService.class, states);
        context.services().register(DefaultPlayerStateService.class, (DefaultPlayerStateService) states);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        context.events().listen(CellulosesZBootstrap.PlayerJoinEvent.class, event -> {
            var platform = context.services().require(PlatformService.class);
            var users = context.services().require(UserService.class);
            platform.player(event.player())
                    .ifPresent(player -> users.cached(player.uuid())
                            .ifPresent(user -> {
                                if (config.persistFlyGod) {
                                    if (user.state.flying) states.setFlying(player, true);
                                    if (user.state.god) states.setGod(player, true);
                                }
                            })
                    );
        });
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);

        context.commands().register(new FlyCommand(platform, users, states));
        context.commands().register(new GodCommand(platform, users, states));
        context.commands().register(new HealCommand(platform, users, states));
        context.commands().register(new FeedCommand(platform, users, states));
        context.commands().register(new AfkCommand(platform, users, states));
        context.commands().register(new NickCommand(platform, users, states));
    }

}
