package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

public final class PayConfirmToggleCommand extends AbstractEconomyCommand {

    public PayConfirmToggleCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.payconfirmtoggle";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "payconfirmtoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var user = users.load(self.get().uuid()).join();
        user.preferences.confirmLargePayments = !user.preferences.confirmLargePayments;
        users.markDirty(self.get().uuid());
        users.save(self.get().uuid());
        invocation.replyKey(
                user.preferences.confirmLargePayments
                        ? "commands.economy.pay-confirm-enabled"
                        : "commands.economy.pay-confirm-disabled"
        );
        return 1;
    }

}
