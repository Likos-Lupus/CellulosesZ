package top.likoslupus.cellulosesz.modules.teleport.command;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestType;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.teleport.service.TeleportRequestExecutor;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TpaAllCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestExecutor executor;
    private final UserService users;
    private final int timeoutSeconds;
    private final int maximumTargets;

    public TpaAllCommand(
            PlatformService platform,
            TeleportRequestExecutor executor,
            UserService users,
            int timeoutSeconds,
            int maximumTargets
    ) {
        this.platform = platform;
        this.executor = executor;
        this.users = users;
        this.timeoutSeconds = timeoutSeconds;
        this.maximumTargets = Math.max(1, maximumTargets);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpaall";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "tpaall";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", "/tpaall"));
            return 0;
        }
        var requester = platform.player(invocation);
        if (requester.isEmpty()) {
            invocation.errorKey("commands.teleport.tpa-command.error.2");
            return 0;
        }
        var candidates = platform.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(requester.orElseThrow().uuid()))
                .limit(maximumTargets + 1L)
                .toList();
        if (candidates.size() > maximumTargets) {
            invocation.errorKey("commands.teleport.tpaall.too-many", Map.of("maximum", maximumTargets));
            return 0;
        }
        if (candidates.isEmpty()) {
            invocation.errorKey("commands.teleport.tpaall.no-targets");
            return 0;
        }

        var checks = new ArrayList<CompletableFuture<TargetDecision>>(candidates.size());
        for (var candidate : candidates) {
            checks.add(users.load(candidate.uuid())
                    .thenApply(user -> new TargetDecision(candidate, user.preferences.teleportRequests, false))
                    .exceptionally(_ -> new TargetDecision(candidate, false, true)));
        }
        CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                .thenRun(() -> platform.runOnServerThread(() -> {
                    var sent = 0;
                    var blocked = 0;
                    var failed = 0;
                    var bypass = invocation.hasPermission("cellulosesz.teleport.tptoggle.bypass");
                    for (var check : checks) {
                        var decision = check.getNow(new TargetDecision(null, false, true));
                        if (decision.failure()) {
                            failed++;
                        } else if (!decision.enabled() && !bypass) {
                            blocked++;
                        } else if (decision.player() != null) {
                            executor.create(invocation, requester.orElseThrow(), decision.player(),
                                    TeleportRequestType.TARGET_TO_REQUESTER, timeoutSeconds);
                            sent++;
                        }
                    }
                    if (sent == 0) invocation.errorKey("commands.teleport.tpaall.no-targets");
                    else invocation.replyKey("commands.teleport.tpaall.result", Map.of(
                            "sent", sent, "blocked", blocked, "failed", failed, "total", candidates.size()));
                }));
        return 1;
    }

    private record TargetDecision(
            @Nullable CellPlayer player,
            boolean enabled,
            boolean failure
    ) {

    }

}
