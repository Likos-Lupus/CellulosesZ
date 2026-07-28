package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.Map;

public final class WorldCommand extends AbstractTeleportCommand {

    public WorldCommand(
            PlatformService platform,
            TeleportService teleports
    ) {
        super(platform, teleports);
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.world";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/world <world>";
    }

    @Override
    public String name() {
        return "world";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length == 0) {
            invocation.replyKey(
                    "commands.teleport.world-command.reply.available-worlds",
                    Map.of("worlds", String.join(", ", platform.worlds()))
            );
            return 1;
        }

        if (args.length != 1 || !platform.worlds().contains(args[0])) {
            invocation.errorKey("commands.teleport.world-command.invalid-world", Map.of("world", args.length == 0
                    ? ""
                    : args[0]));
            return 0;
        }
        var permission = "cellulosesz.teleport.world." + args[0].toLowerCase().replace(':', '.');
        if (!invocation.hasPermission(permission)) {
            invocation.errorKey("commands.teleport.world-no-permission", Map.of("world", args[0]));
            return 0;
        }
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var current = platform.location(self.get());
        var target = current.withWorld(args[0]);
        return teleport(invocation, self.get(), target);
    }

}
