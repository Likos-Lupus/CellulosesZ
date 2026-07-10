package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStateService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class FeedCommand extends AbstractPlayerStateCommand {

    public FeedCommand(
            PlatformService platform,
            UserService users,
            PlayerStateService states
    ) {
        super(platform, users, states);
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.feed";
    }

    @Override
    public String usage() {
        return "/feed [player]";
    }

    @Override
    public String name() {
        return "feed";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var target = target(
                invocation,
                0,
                "cellulosesz.playerstate.feed.other"
        );
        if (target.isEmpty()) return 0;

        var result = states.feed(target.get());
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
