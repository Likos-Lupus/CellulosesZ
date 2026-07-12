package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;

public final class CommandCostMiddleware implements CommandMiddleware {

    private final PlatformService platform;
    private final CommandCostService costs;

    public CommandCostMiddleware(
            PlatformService platform,
            CommandCostService costs
    ) {
        this.platform = platform;
        this.costs = costs;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation continuation
    ) {
        var cost = costs.cost(command.name());
        if (cost.signum() <= 0) return continuation.proceed();

        var player = platform.player(invocation);
        if (player.isEmpty()) return continuation.proceed();

        if (!costs.charge(player.get().uuid(), command.name())) {
            invocation.errorKey(
                    "common.command-cost-failed",
                    Map.of("cost", cost.toPlainString())
            );
            return 0;
        }

        return continuation.proceed();
    }

}
