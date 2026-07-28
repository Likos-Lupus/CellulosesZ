package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class HomeCommand extends AbstractHomeCommand {

    private static final String COOLDOWN_KEY = "home.teleport";

    private final CooldownService cooldowns;

    public HomeCommand(
            PlatformService platform,
            HomeService homes,
            TeleportService teleports,
            HomeConfig config,
            CooldownService cooldowns
    ) {
        super(platform, homes, teleports, config);
        this.cooldowns = cooldowns;
    }

    @Override
    public List<String> aliases() {
        return List.of("homes");
    }

    @Override
    public String permission() {
        return "cellulosesz.home.use";
    }

    @Override
    public String usage() {
        return "/home [name] | /homes";
    }

    @Override
    public String name() {
        return "home";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;
        var player = self.orElseThrow();
        var args = invocation.args();
        if (invocation.label().equalsIgnoreCase("homes")) {
            homes.homes(player.uuid()).whenComplete((knownHomes, failure) -> {
                if (failure != null) invocation.errorKey("common.persistence-failed");
                else if (knownHomes.isEmpty()) invocation.replyKey("commands.home.list-empty");
                else invocation.replyKey("commands.home.list", Map.of("homes", String.join(", ", knownHomes.keySet())));
            });
            return 1;
        }

        var name = nameOrDefault(args);
        if (!invocation.hasPermission("cellulosesz.home.bypass-cooldown")) {
            var remaining = cooldowns.remaining(player.uuid(), COOLDOWN_KEY);
            if (!remaining.isZero()) {
                invocation.errorKey("commands.home.cooldown", Map.of("seconds", Math.max(1L, remaining.toSeconds() + (
                        remaining.toMillisPart() > 0
                                ? 1
                                : 0))));
                return 0;
            }
        }

        homes.home(player.uuid(), name).whenComplete((location, failure) -> {
            if (failure != null) {
                invocation.errorKey("common.persistence-failed");
                return;
            }
            if (location.isEmpty()) {
                invocation.errorKey("commands.home.home-command.error.home-does-not-exist", Map.of("home", name));
                return;
            }
            platform.callOnServerThread(() -> teleports.teleport(player, location.orElseThrow(), options(invocation)))
                    .thenCompose(value -> value)
                    .whenComplete((result, teleportFailure) -> {
                        if (teleportFailure != null) {
                            invocation.errorKey("commands.teleport.request.failed", Map.of("reason", teleportFailure.getClass()
                                    .getSimpleName()));
                        } else if (result.success()) {
                            if (!invocation.hasPermission("cellulosesz.home.bypass-cooldown") && config.teleport.cooldownSeconds > 0) {
                                cooldowns.start(player.uuid(), COOLDOWN_KEY, Duration.ofSeconds(config.teleport.cooldownSeconds));
                            }
                            invocation.replyKey("commands.home.home-command.reply.teleported-home", Map.of("home", name));
                        } else {
                            invocation.error(result.message());
                        }
                    });
        });
        return 1;
    }

}
