package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BroadcastWorldCommand extends AbstractMessagingCommand {

    private final MessageRenderer renderer;

    public BroadcastWorldCommand(
            PlatformService platform,
            UserService users,
            MessagingConfig config,
            MessageRenderer renderer
    ) {
        super(platform, users, config);
        this.renderer = renderer;
    }

    @Override
    public String permission() {
        return "cellulosesz.messaging.broadcastworld";
    }

    @Override
    public String usage() {
        return "/broadcastworld <world> <message>";
    }

    @Override
    public String name() {
        return "broadcastworld";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();

        if (args.length < 2) {
            invocation.errorKey(
                    "commands.messaging.broadcast-world-command.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var world = resolveWorld(args[0]);
        if (world.isEmpty()) {
            invocation.errorKey(
                    "commands.messaging.broadcast-world-command.world",
                    Map.of("world", args[0])
            );
            return 0;
        }

        var resolvedWorld = world.orElseThrow();
        var message = join(args, 1);

        if (!validLength(invocation, message)) {
            return 0;
        }

        var recipients = platform.onlinePlayers().stream()
                .filter(player -> platform.location(player).world.equals(resolvedWorld))
                .toList();
        recipients.forEach(player ->
                platform.sendMessage(
                        player,
                        renderer.render(
                                platform.locale(player),
                                "messaging.broadcast",
                                Map.of("message", message)
                        )
                )
        );
        invocation.replyKey(
                "commands.messaging.broadcast-world-command.sent",
                Map.of(
                        "world", resolvedWorld,
                        "count", recipients.size()
                )
        );
        return 1;
    }

    private Optional<String> resolveWorld(String input) {
        var normalized = input.trim().toLowerCase(Locale.ROOT);
        return platform.worlds().stream()
                .filter(world -> world.equalsIgnoreCase(normalized)
                        || world.substring(world.indexOf(':') + 1).equalsIgnoreCase(normalized)
                )
                .findFirst();
    }

}
