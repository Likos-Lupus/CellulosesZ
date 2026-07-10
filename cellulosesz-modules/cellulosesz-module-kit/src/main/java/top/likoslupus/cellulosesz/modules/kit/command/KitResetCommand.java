package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

public final class KitResetCommand extends AbstractKitCommand {

    private final UserService users;

    public KitResetCommand(
            PlatformService platform,
            KitService kits,
            UserService users
    ) {
        super(platform, kits);
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.reset";
    }

    @Override
    public String usage() {
        return "/kitreset <player> <kit>";
    }

    @Override
    public String name() {
        return "kitreset";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 2) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var online = platform.onlinePlayer(args[0]);
        var uuid = online.map(CellPlayer::uuid)
                .or(() -> users.findUuidByName(args[0]));
        if (uuid.isEmpty()) {
            invocation.error("找不到玩家: " + args[0]);
            return 0;
        }

        kits.resetCooldown(uuid.get(), args[1]).thenRun(() -> invocation.reply("已重置 Kit 冷却。"));
        return 1;
    }

}
