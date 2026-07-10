package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

public final class HelpOpCommand extends AbstractMessagingCommand {

    private final PermissionService permissions;

    public HelpOpCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            PermissionService permissions
    ) {
        super(platform, users, config);
        this.permissions = permissions;
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.helpop";
    }

    @Override
    public String usage() {
        return "/helpop <message>";
    }

    @Override
    public String name() {
        return "helpop";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var message = join(args, 0);
        if (!validLength(invocation, message)) return 0;

        var sender = invocation.playerName().orElse("console");
        platform.onlinePlayers().stream()
                .filter(player -> permissions.has(player.nativeHandle(), "cellulosesz.messaging.helpop.receive"))
                .forEach(player -> platform.sendMessage(player, "[HelpOp] " + sender + ": " + message));
        invocation.reply("HelpOp 消息已发送。 ");
        return 1;
    }

}
