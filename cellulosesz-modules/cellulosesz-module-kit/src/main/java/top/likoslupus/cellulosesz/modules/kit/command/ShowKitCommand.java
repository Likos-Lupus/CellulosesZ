package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

public final class ShowKitCommand extends AbstractKitCommand {

    public ShowKitCommand(
            PlatformService platform,
            KitService kits
    ) {
        super(platform, kits);
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.show";
    }

    @Override
    public String usage() {
        return "/showkit <name>";
    }

    @Override
    public String name() {
        return "showkit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var kit = kits.kit(args[0]);
        if (kit.isEmpty()) {
            invocation.error("Kit 不存在: " + args[0]);
            return 0;
        }

        var builder = new StringBuilder("Kit ").append(kit.get().displayName).append(':');
        kit.get().items.forEach(item -> builder.append("\n- ")
                .append(item.normalizedItem())
                .append(" x")
                .append(item.count));
        invocation.reply(builder.toString());
        return 1;
    }

}
