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

        var args = invocation.args();
        if (invocation.label().equalsIgnoreCase("homes")) {
            var names = homes.homes(self.get().uuid()).join().keySet();
            if (names.isEmpty()) {
                invocation.replyKey("commands.home.list-empty");
            } else {
                invocation.replyKey(
                        "commands.home.list",
                        Map.of("homes", String.join(", ", names))
                );
            }
            return 1;
        }

        var name = nameOrDefault(args);
        var location = homes.home(self.get().uuid(), name).join();
        if (location.isEmpty()) {
            invocation.errorKey(
                    "commands.home.home-command.error.1",
                    Map.of("value0", name)
            );
            return 0;
        }

        if (!invocation.hasPermission("cellulosesz.home.bypass-cooldown")) {
            var remaining = cooldowns.remaining(self.get().uuid(), COOLDOWN_KEY);
            if (!remaining.isZero()) {
                invocation.errorKey(
                        "commands.home.cooldown",
                        Map.of("seconds", Math.max(1L, remaining.toSeconds() + (remaining.toMillisPart() > 0 ? 1 : 0)))
                );
                return 0;
            }
        }

        teleports.teleport(self.get(), location.get(), options(invocation))
                .thenAccept(result -> {
                    if (result.success()) {
                        if (!invocation.hasPermission("cellulosesz.home.bypass-cooldown")
                                && config.teleport.cooldownSeconds > 0
                        ) {
                            cooldowns.start(
                                    self.get().uuid(),
                                    COOLDOWN_KEY,
                                    Duration.ofSeconds(config.teleport.cooldownSeconds)
                            );
                        }
                        invocation.replyKey(
                                "commands.home.home-command.reply.1",
                                Map.of("value0", name)
                        );
                    } else {
                        invocation.errorKey(
                                "commands.home.home-command.error.2",
                                Map.of("value0", result.message())
                        );
                    }
                });

        return 1;
    }

}
