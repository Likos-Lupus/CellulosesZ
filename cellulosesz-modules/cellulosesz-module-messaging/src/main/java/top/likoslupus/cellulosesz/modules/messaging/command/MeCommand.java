package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Map;

public final class MeCommand extends AbstractMessagingCommand {

    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;

    public MeCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            DisplayNameService displayNames,
            MessageRenderer renderer
    ) {
        super(platform, users, config);
        this.displayNames = displayNames;
        this.renderer = renderer;
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.me";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/me <action>";
    }

    @Override
    public String name() {
        return "me";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        var args = invocation.args();
        if (self.isEmpty()) return 0;

        if (args.length < 1) {
            invocation.errorKey(
                    "commands.messaging.me-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var action = join(args, 0);
        if (!validLength(invocation, action)) return 0;

        platform.onlinePlayers().forEach(player -> platform.sendMessage(
                player,
                renderer.render(
                        platform.locale(player),
                        "messaging.me",
                        Map.of(
                                "player", displayNames.displayName(self.get()),
                                "action", action
                        )
                )
        ));
        return 1;
    }

}
