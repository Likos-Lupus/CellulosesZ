package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.admin.TempBanService;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class UnbanCommand extends AbstractAdminCommand {

    private final BanService bans;
    private final TempBanService temporary;

    public UnbanCommand(
            PlatformService platform,
            UserService users,
            BanService bans,
            TempBanService temporary
    ) {
        super(platform, users);
        this.bans = bans;
        this.temporary = temporary;
    }

    @Override
    public String permission() {
        return "cellulosesz.admin.unban";
    }

    @Override
    public String usage() {
        return "/unban <player>";
    }

    @Override
    public String name() {
        return "unban";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey(
                    "commands.admin.unban.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var resolved = invocation.resolvePlayer(invocation.args()[0]);
        if (resolved.optionalUuid().isEmpty()) {
            invocation.errorKey(
                    "commands.admin.abstract-admin-command.error.player-not-found",
                    Map.of("player", invocation.args()[0])
            );
            return 0;
        }

        var permanent = bans.unban(
                resolved.optionalUuid().orElseThrow(),
                resolved.name(),
                actor(invocation)
        );
        temporary.unban(
                resolved.optionalUuid().orElseThrow(),
                resolved.name(),
                actor(invocation)
        ).whenComplete((temporaryResult, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.admin.persistence-failed");
            } else if (temporaryResult.status()
                    == top.likoslupus.cellulosesz.api.admin.AdminStatus.PERSISTENCE_FAILURE) {
                invocation.error(temporaryResult.message());
            } else if (permanent.success() || temporaryResult.success()) {
                invocation.replyKey(
                        "commands.admin.unban.success",
                        Map.of("player", resolved.name())
                );
            } else {
                invocation.errorKey(
                        "commands.admin.unban.not-found",
                        Map.of("player", resolved.name())
                );
            }
        });
        return 1;
    }

}
