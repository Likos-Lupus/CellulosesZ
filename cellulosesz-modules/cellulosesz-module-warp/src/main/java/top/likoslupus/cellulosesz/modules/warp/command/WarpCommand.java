package top.likoslupus.cellulosesz.modules.warp.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
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
        return "/warp <name>";
    }

    @Override
    public String name() {
        return "warp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        if (invocation.label().equalsIgnoreCase("warps")) {
            var names = warps.warps().join().stream()
                    .map(warp -> warp.name)
                    .toList();
            if (names.isEmpty()) {
                invocation.replyKey("commands.warp.list-empty");
            } else {
                invocation.replyKey(
                        "commands.warp.list",
                        Map.of("warps", String.join(", ", names))
                );
            }
            return 1;
        }

        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey(
                    "commands.warp.warp-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var warp = warps.warp(args[0]).join();
        if (warp.isEmpty()) {
            invocation.errorKey(
                    "commands.warp.warp-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        if (warp.get().permission != null
                && !warp.get().permission.isBlank()
                && !invocation.hasPermission(warp.get().permission)
        ) {
            invocation.errorKey("commands.warp.warp-command.error.3");
            return 0;
        }

        if (!invocation.hasPermission("cellulosesz.warp.bypass-cooldown")) {
            var remaining = cooldowns.remaining(self.get().uuid(), COOLDOWN_KEY);
            if (!remaining.isZero()) {
                invocation.errorKey(
                        "commands.warp.cooldown",
                        Map.of("seconds", Math.max(1L, remaining.toSeconds() + (remaining.toMillisPart() > 0 ? 1 : 0)))
                );
                return 0;
            }
        }

        teleports.teleport(self.get(), warp.get().location, options(invocation))
                .thenAccept(result -> {
                    if (result.success()) {
                        if (!invocation.hasPermission("cellulosesz.warp.bypass-cooldown")
                                && config.teleport.cooldownSeconds > 0) {
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
                        invocation.errorKey(
                                "commands.warp.warp-command.error.4",
                                Map.of("value0", result.message())
                        );
                    }
                });
        return 1;
    }

}
