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
import java.util.Optional;

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
        if (self.isEmpty()) {
            return 0;
        }

        var args = invocation.args();

        if (invocation.label().equalsIgnoreCase("warps")
                || args.length == 0
                || (args.length == 1 && numeric(args[0]))
        ) {
            return list(
                    invocation,
                    args.length == 1 && numeric(args[0])
                            ? Integer.parseInt(args[0])
                            : 1
            );
        }

        if (args.length != 1) {
            invocation.errorKey(
                    "commands.warp.warp-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        Optional<Warp> warp;
        try {
            warp = warps.warp(args[0]).join();
        } catch (RuntimeException _) {
            invocation.errorKey("service.warp.persistence-failed");
            return 0;
        }

        if (warp.isEmpty()) {
            invocation.errorKey(
                    "commands.warp.warp-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        if (!allowed(invocation, warp.get())) {
            invocation.errorKey("commands.warp.warp-command.error.3");
            return 0;
        }

        if (!invocation.hasPermission("cellulosesz.warp.bypass-cooldown")) {
            var remaining = cooldowns.remaining(
                    self.get().uuid(),
                    COOLDOWN_KEY
            );
            if (!remaining.isZero()) {
                invocation.errorKey(
                        "commands.warp.cooldown",
                        Map.of(
                                "seconds",
                                Math.max(1L, remaining.toSeconds() + (remaining.toMillisPart() > 0 ? 1 : 0))
                        )
                );
                return 0;
            }
        }

        teleports.teleport(
                self.get(),
                warp.get().location,
                options(invocation)
        ).thenAccept(result -> {
            if (result.success()) {
                if (!invocation.hasPermission("cellulosesz.warp.bypass-cooldown")
                        && config.teleport.cooldownSeconds > 0
                ) {
                    cooldowns.start(
                            self.get().uuid(),
                            COOLDOWN_KEY,
                            Duration.ofSeconds(config.teleport.cooldownSeconds)
                    );
                }
                invocation.replyKey(
                        "commands.warp.warp-command.reply.1",
                        Map.of("value0", warp.get().displayName)
                );
            } else {
                invocation.error(result.message());
            }
        });
        return 1;
    }

    private boolean numeric(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    private int list(CommandInvocation invocation, int requestedPage) {
        List<Warp> available;
        try {
            available = warps.warps().join();
        } catch (RuntimeException _) {
            invocation.errorKey("service.warp.persistence-failed");
            return 0;
        }

        var visible = available.stream()
                .filter(warp -> !config.list.hideNoPermission || allowed(invocation, warp))
                .toList();
        if (visible.isEmpty()) {
            invocation.replyKey("commands.warp.list-empty");
            return 1;
        }

        var pageSize = Math.max(1, config.list.pageSize);
        var pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
        var page = Math.clamp(requestedPage, 1, pages);
        var from = (page - 1) * pageSize;
        var names = visible.subList(from, Math.min(visible.size(), from + pageSize)).stream()
                .map(warp -> warp.displayName)
                .toList();
        invocation.replyKey(
                "commands.warp.list-page",
                Map.of(
                        "warps", String.join(", ", names),
                        "page", page,
                        "pages", pages
                )
        );
        return 1;
    }

    private boolean allowed(CommandInvocation invocation, Warp warp) {
        return warps.requiredPermission(warp)
                .map(invocation::hasPermission)
                .orElse(true);
    }

}
