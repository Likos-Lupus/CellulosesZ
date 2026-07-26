package top.likoslupus.cellulosesz.modules.home.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.util.Map;

public final class SetHomeCommand extends AbstractHomeCommand {

    public SetHomeCommand(PlatformService platform, HomeService homes, TeleportService teleports, HomeConfig config) {
        super(platform, homes, teleports, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.home.set";
    }

    @Override
    public String usage() {
        return "/sethome [name]";
    }

    @Override
    public String name() {
        return "sethome";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;
        var name = nameOrDefault(invocation.args());
        if (!validName(invocation, name)) return 0;
        var uuid = self.orElseThrow().uuid();
        homes.homes(uuid).whenComplete((existing, failure) -> {
            if (failure != null) {
                invocation.errorKey("common.persistence-failed");
                return;
            }
            if (!existing.containsKey(name.toLowerCase(java.util.Locale.ROOT))
                    && existing.size() >= config.limits.defaultMaxHomes
                    && !invocation.hasPermission("cellulosesz.home.bypass-limit")) {
                invocation.errorKey("commands.home.set-home-command.error.1", Map.of("value0", config.limits.defaultMaxHomes));
                return;
            }
            platform.callOnServerThread(() -> platform.location(self.orElseThrow()))
                    .thenCompose(location -> homes.setHome(uuid, name, location))
                    .whenComplete((saved, saveFailure) -> {
                        if (saveFailure != null) invocation.errorKey("common.persistence-failed");
                        else invocation.replyKey("commands.home.set-home-command.reply.1", Map.of("value0", name));
                    });
        });
        return 1;
    }

}
