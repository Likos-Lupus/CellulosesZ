package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.math.BigDecimal;

public final class PayCommand extends AbstractEconomyCommand {

    public PayCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.pay";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/pay <player> <amount>";
    }

    @Override
    public String name() {
        return "pay";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 2) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var sender = player(invocation);
        var target = uuid(invocation, args[0]);
        var amount = amount(invocation, args[1]);
        if (sender.isEmpty() || target.isEmpty() || amount.isEmpty()) return 0;

        var minimum = new BigDecimal(config.pay.minimum);
        if (amount.get().compareTo(minimum) < 0) {
            invocation.error("付款金额不能低于 " + format(minimum));
            return 0;
        }

        var recipientUser = users.load(target.get()).join();
        if (!recipientUser.preferences.payments) {
            invocation.error("该玩家当前不接收付款。");
            return 0;
        }

        var result = economy.transfer(
                sender.get().uuid(),
                target.get(),
                amount.get(),
                cause(invocation, "pay " + args[0])
        );
        if (!result.success()) {
            invocation.error(result.message());
            return 0;
        }

        invocation.reply("已向 %s 支付 %s。当前余额: %s".formatted(args[0], format(amount.get()), format(result.balance())));
        platform.onlinePlayer(args[0]).ifPresent(targetPlayer -> platform.sendMessage(
                targetPlayer,
                "%s 向你支付了 %s。".formatted(sender.get().name(), format(amount.get()))
        ));
        return 1;
    }

}
