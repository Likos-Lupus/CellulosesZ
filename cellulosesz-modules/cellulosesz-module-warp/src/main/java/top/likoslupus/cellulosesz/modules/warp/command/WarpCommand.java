package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class WarpCommand extends AbstractWarpCommand {

    private static final String COOLDOWN_KEY = "warp.teleport";
    private final CooldownService cooldowns;

    public WarpCommand(
            PlatformService platform,
            WarpService warps,
            TeleportService teleports,
            WarpConfig config,
            CooldownService cooldowns
    ) {
        super(platform, warps, teleports, config);
        this.cooldowns = cooldowns;
    }

    @Override
    public List<String> aliases() {
        return List.of("warps");
    }

    @Override
    public String permission() {
        return "cellulosesz.warp.use";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/warp [name|page]";
    }

    @Override
    public String name() {
        return "warp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;
        var player = self.orElseThrow();
        var args = invocation.args();

        if (invocation.label().equalsIgnoreCase("warps") || args.length == 0
                || (args.length == 1 && numeric(args[0]))) {
            var page = args.length == 1 && numeric(args[0]) ? Integer.parseInt(args[0]) : 1;
            list(invocation, page);
            return 1;
        }
        if (args.length != 1) {
            invocation.errorKey("commands.warp.warp-command.error.usage", Map.of("usage", usage()));
            return 0;
        }
        if (!invocation.hasPermission("cellulosesz.warp.bypass-cooldown")) {
            var remaining = cooldowns.remaining(player.uuid(), COOLDOWN_KEY);
            if (!remaining.isZero()) {
                invocation.errorKey("commands.warp.cooldown", Map.of("seconds", Math.max(1L, remaining.toSeconds() + (
                        remaining.toMillisPart() > 0
                                ? 1
                                : 0))));
                return 0;
            }
        }
        try {
            warps.warp(args[0]).whenComplete((warp, failure) -> {
                if (failure != null) {
                    invocation.errorKey("service.warp.persistence-failed");
                    return;
                }
                if (warp.isEmpty()) {
                    invocation.errorKey("commands.warp.warp-command.error.warp-does-not-exist", Map.of("warp", args[0]));
                    return;
                }
                if (!allowed(invocation, warp.orElseThrow())) {
                    invocation.errorKey("commands.warp.warp-command.error.do-not-permission-use-warp");
                    return;
                }
                platform.callOnServerThread(() -> teleports.teleport(player, warp.orElseThrow().location, options(invocation)))
                        .thenCompose(value -> value)
                        .whenComplete((result, teleportFailure) -> {
                            if (teleportFailure != null) {
                                invocation.errorKey("commands.teleport.request.failed", Map.of("reason", teleportFailure.getClass()
                                        .getSimpleName()));
                            } else if (result.success()) {
                                if (!invocation.hasPermission("cellulosesz.warp.bypass-cooldown") && config.teleport.cooldownSeconds > 0) {
                                    cooldowns.start(player.uuid(), COOLDOWN_KEY, Duration.ofSeconds(config.teleport.cooldownSeconds));
                                }
                                invocation.replyKey("commands.warp.warp-command.reply.teleported-warp", Map.of("target", warp.orElseThrow().displayName));
                            } else invocation.error(result.message());
                        });
            });
            return 1;
        } catch (IllegalArgumentException _) {
            invocation.errorKey("commands.warp.warp-command.error.warp-does-not-exist", Map.of("warp", args[0]));
            return 0;
        }
    }

    private boolean numeric(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    private void list(CommandInvocation invocation, int requestedPage) {
        warps.warps().whenComplete((available, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.warp.persistence-failed");
                return;
            }
            var visible = available.stream()
                    .filter(warp -> !config.list.hideNoPermission || allowed(invocation, warp))
                    .toList();
            if (visible.isEmpty()) {
                invocation.replyKey("commands.warp.list-empty");
                return;
            }
            var pageSize = Math.max(1, config.list.pageSize);
            var pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
            if (requestedPage < 1 || requestedPage > pages) {
                invocation.errorKey("commands.warp.warp-command.error.usage", Map.of("usage", usage()));
                return;
            }
            final int from;
            try {
                from = Math.multiplyExact(requestedPage - 1, pageSize);
            } catch (ArithmeticException _) {
                invocation.errorKey("commands.warp.warp-command.error.usage", Map.of("usage", usage()));
                return;
            }
            var names = visible.subList(from, Math.min(visible.size(), from + pageSize)).stream()
                    .map(warp -> warp.displayName).toList();
            invocation.replyKey("commands.warp.list-page", Map.of(
                    "warps", String.join(", ", names), "page", requestedPage, "pages", pages));
        });
    }

    private boolean allowed(CommandInvocation invocation, Warp warp) {
        return warps.requiredPermission(warp)
                .map(invocation::hasPermission)
                .orElse(true);
    }

}
