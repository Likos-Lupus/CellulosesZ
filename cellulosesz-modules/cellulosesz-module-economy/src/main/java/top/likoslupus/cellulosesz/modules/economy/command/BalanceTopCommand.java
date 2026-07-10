package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.util.List;
import java.util.stream.IntStream;

public final class BalanceTopCommand extends AbstractEconomyCommand {

    public BalanceTopCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("baltop");
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.balancetop";
    }

    @Override
    public String usage() {
        return "/balancetop [page]";
    }

    @Override
    public String name() {
        return "balancetop";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        var page = 1;
        if (args.length >= 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException exception) {
                invocation.error("页码必须是整数。");
                return 0;
            }
        }

        var pageSize = Math.max(1, config.balanceTop.pageSize);
        var entries = economy.topBalances(page * pageSize);
        var from = (page - 1) * pageSize;
        if (from >= entries.size()) {
            invocation.error("此页没有余额排行记录。");
            return 0;
        }

        var builder = new StringBuilder("余额排行 #").append(page).append(':');
        IntStream.range(from, Math.min(entries.size(), from + pageSize)).forEach(index -> {
            var entry = entries.get(index);
            var name = users.cached(entry.uuid())
                    .filter(user -> user.lastKnownName != null)
                    .map(user -> user.lastKnownName)
                    .orElse(entry.uuid().toString());
            builder.append("\n")
                    .append(index + 1)
                    .append(". ")
                    .append(name)
                    .append(" - ")
                    .append(format(entry.balance()));
        });
        invocation.reply(builder.toString());
        return 1;
    }

}
