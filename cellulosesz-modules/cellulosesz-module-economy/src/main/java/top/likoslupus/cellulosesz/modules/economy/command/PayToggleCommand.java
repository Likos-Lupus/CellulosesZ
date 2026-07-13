package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

public final class PayToggleCommand extends AbstractEconomyCommand {

    public PayToggleCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.paytoggle";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "paytoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var user = users.load(self.get().uuid()).join();
        var previous = user.preferences.payments;
        user.preferences.payments = !previous;
        users.markDirty(self.get().uuid());
        try {
            users.save(self.get().uuid()).join();
        } catch (RuntimeException _) {
            user.preferences.payments = previous;
            users.markDirty(self.get().uuid());
            invocation.errorKey("service.user.persistence-failed");
            return 0;
        }

        invocation.replyKey(
                user.preferences.payments
                        ? "commands.economy.payments-enabled"
                        : "commands.economy.payments-disabled"
        );
        return 1;
    }

}
