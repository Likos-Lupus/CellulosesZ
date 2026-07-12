package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.List;
import java.util.Map;

public final class BroadcastCommand extends AbstractMessagingCommand {

    private final MessageRenderer renderer;

    public BroadcastCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            MessageRenderer renderer
    ) {
        super(platform, users, config);
        this.renderer = renderer;
    }

    @Override
    public List<String> aliases() {
        return List.of("broadcastworld");
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.broadcast";
    }

    @Override
    public String usage() {
        return "/broadcast <message>";
    }

    @Override
    public String name() {
        return "broadcast";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.errorKey(
                    "commands.messaging.broadcast-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var message = join(args, 0);
        if (!validLength(invocation, message)) return 0;

        platform.onlinePlayers().forEach(player -> platform.sendMessage(
                player,
                renderer.render(
                        platform.locale(player),
                        "messaging.broadcast",
                        Map.of("message", message)
                )
        ));
        invocation.replyKey("messaging.broadcast-sent");
        return 1;
    }

}
