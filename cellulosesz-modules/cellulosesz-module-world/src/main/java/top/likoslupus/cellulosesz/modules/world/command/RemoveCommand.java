package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.EntityRemoveService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

public final class RemoveCommand extends AbstractWorldCommand {

    private final EntityRemoveService remover;

    public RemoveCommand(
            PlatformService platform,
            WorldConfig config,
            EntityRemoveService remover
    ) {
        super(platform, config);
        this.remover = remover;
    }

    @Override
    public String permission() {
        return "cellulosesz.world.remove";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/remove <selector> [radius]";
    }

    @Override
    public String name() {
        return "remove";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var radius = args.length >= 2
                ? parse(args[1], config.defaultRemoveRadius)
                : config.defaultRemoveRadius;
        var result = remover.remove(
                args[0],
                player(invocation),
                radius
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

    private int parse(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

}
