package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

public final class DelKitCommand extends AbstractKitCommand {

    public DelKitCommand(
            PlatformService platform,
            KitService kits
    ) {
        super(platform, kits);
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.delete";
    }

    @Override
    public String usage() {
        return "/delkit <name>";
    }

    @Override
    public String name() {
        return "delkit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }

        kits.delete(args[0]).thenAccept(deleted -> {
            if (deleted) invocation.reply("已删除 Kit: " + args[0]);
            else invocation.error("Kit 不存在: " + args[0]);
        });
        return 1;
    }

}
