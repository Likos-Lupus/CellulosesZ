package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Map;

public final class HelpOpCommand extends AbstractMessagingCommand {

    private final PermissionService permissions;
    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;

    public HelpOpCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            PermissionService permissions,
            DisplayNameService displayNames,
            MessageRenderer renderer
    ) {
        super(platform, users, config);
        this.permissions = permissions;
        this.displayNames = displayNames;
        this.renderer = renderer;
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
            invocation.errorKey(
                    "commands.messaging.help-op-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var message = join(args, 0);
        if (!validLength(invocation, message)) return 0;

        var sender = platform.player(invocation)
                .map(displayNames::displayName)
                .orElseGet(() -> renderer.render(invocation.locale(), "common.console"));
        platform.onlinePlayers().stream()
                .filter(player ->
                        permissions.has(
                                player.nativeHandle(),
                                "cellulosesz.messaging.helpop.receive"
                        )
                )
                .forEach(player -> platform.sendMessage(
                        player,
                        renderer.render(
                                platform.locale(player),
                                "messaging.helpop",
                                Map.of(
                                        "sender", sender,
                                        "message", message
                                )
                        )
                ));
        invocation.replyKey("messaging.helpop-sent");
        return 1;
    }

}
