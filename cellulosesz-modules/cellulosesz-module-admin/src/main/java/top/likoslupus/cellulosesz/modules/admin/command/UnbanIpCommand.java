package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.modules.admin.service.IpAddresses;

import java.util.Map;

public final class UnbanIpCommand implements CellCommand {

    private final BanService bans;
    private final TempBanService temporary;

    public UnbanIpCommand(
            BanService bans,
            TempBanService temporary
    ) {
        this.bans = bans;
        this.temporary = temporary;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.unbanip";
    }

    @Override
    public String usage() {
        return "/unbanip <address>";
    }

    @Override
    public String name() {
        return "unbanip";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey(
                    "commands.admin.unban-ip.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var address = IpAddresses.normalize(invocation.args()[0]);
        if (address.isEmpty()) {
            invocation.errorKey(
                    "service.admin.invalid-address",
                    Map.of("address", invocation.args()[0])
            );
            return 0;
        }

        var actor = invocation.playerName().orElse("console");
        var value = address.orElseThrow();
        var permanent = bans.unbanIp(value, actor);
        temporary.unbanIp(value, actor).whenComplete((temporaryResult, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.admin.persistence-failed");
            } else if (temporaryResult.status()
                    == top.likoslupus.cellulosesz.api.admin.AdminStatus.PERSISTENCE_FAILURE) {
                invocation.error(temporaryResult.message());
            } else if (permanent.success() || temporaryResult.success()) {
                invocation.replyKey(
                        "commands.admin.unban-ip.success",
                        Map.of("address", value)
                );
            } else {
                invocation.errorKey(
                        "commands.admin.unban-ip.not-found",
                        Map.of("address", value)
                );
            }
        });
        return 1;
    }

}
