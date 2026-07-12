package top.likoslupus.cellulosesz.modules.messaging.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;

import java.util.Map;

public final class ListCommand extends AbstractMessagingCommand {

    private final DisplayNameService displayNames;
    private final MessageRenderer renderer;

    public ListCommand(
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
        return "cellulosesz.messaging.list";
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var players = platform.onlinePlayers().stream()
                .filter(player -> invocation.resolvePlayer(player.uuid().toString()).online().isPresent())
                .toList();
        var list = RichText.empty();
        for (var index = 0; index < players.size(); index++) {
            if (index > 0) list = list.append(renderer.renderInline(
                    invocation.locale(),
                    "<primary>, "
            ));
            list = list.append(displayNames.displayName(players.get(index)));
        }

        invocation.reply(renderer.render(
                invocation.locale(),
                "player.list",
                Map.of(
                        "count", players.size(),
                        "players", list
                )
        ));
        return 1;
    }

}
