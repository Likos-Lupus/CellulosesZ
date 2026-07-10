package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

public final class MeCommand extends AbstractMessagingCommand {

    public MeCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config
    ) {
        super(platform, users, config);
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
            invocation.error("用法: " + usage());
            return 0;
        }

        var action = join(args, 0);
        if (!validLength(invocation, action)) return 0;

        platform.onlinePlayers().forEach(player -> platform.sendMessage(
                player,
                "* %s %s".formatted(self.get().name(), action)
        ));
        return 1;
    }

}
