package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;

public final class KickAllCommand implements CellCommand {

    private final BanService bans;

    public KickAllCommand(BanService bans) {
        this.bans = bans;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.kickall";
    }

    @Override
    public String usage() {
        return "/kickall [reason]";
    }

    @Override
    public String name() {
        return "kickall";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var result = bans.kickAll(
                invocation.playerName().orElse("console"),
                String.join(" ", invocation.args())
        );
        if (result.success()) {
            invocation.reply(result.message());
        } else {
            invocation.error(result.message());
        }
        return result.success() ? 1 : 0;
    }

}
