package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.Map;

public final class CommandCostMiddleware implements CommandMiddleware {

    private final CommandCostService costs;
    private final ServerThreadExecutor serverThread;

    public CommandCostMiddleware(
            CommandCostService costs,
            ServerThreadExecutor serverThread
    ) {
        this.costs = costs;
        this.serverThread = serverThread;
    }

    @Override
    public int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        var cost = costs.cost(descriptor.canonicalName());
        if (cost.signum() <= 0) return continuation.proceed();
        var playerUuid = context.playerUuid();
        if (playerUuid.isEmpty()) return continuation.proceed();

        costs.charge(playerUuid.orElseThrow(), descriptor.canonicalName())
                .whenComplete((charged, failure) ->
                        serverThread.execute(() -> {
                            if (failure != null || !Boolean.TRUE.equals(charged)) {
                                context.error(LocalizedMessage.of(
                                        GeneratedMessageKeys.COMMON_COMMAND_COST_FAILED,
                                        Map.of("cost", cost.toPlainString())
                                ));
                                return;
                            }
                            continuation.proceed();
                        }));
        return 1;
    }

}
