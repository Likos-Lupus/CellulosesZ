package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.world.LightningRequest;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;

public final class LightningCommand implements CellCommand {

    private final PlatformService platform;
    private final WorldPlatformService worlds;
    private final WorldConfig config;

    public LightningCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            WorldConfig config
    ) {
        this.platform = platform;
        this.worlds = worlds;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.lightning";
    }

    @Override
    public String usage() {
        return "/lightning [player] [damage]";
    }

    @Override
    public String name() {
        return "lightning";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 2) return usage(invocation);
        final CellLocation location;
        var self = platform.player(invocation);
        if (invocation.args().length == 0) {
            if (self.isEmpty()) {
                invocation.errorKey("commands.world.lightning.console-target-required");
                return 0;
            }
            var target = platform.targetLocation(self.orElseThrow(), config.targetDistance);
            if (target.isEmpty()) {
                invocation.errorKey("commands.world.lightning.no-target");
                return 0;
            }
            location = target.orElseThrow();
        } else {
            if (!invocation.hasPermission("cellulosesz.command.lightning.others")) {
                invocation.errorKey("commands.common.no-permission");
                return 0;
            }
            var target = invocation.resolvePlayer(invocation.args()[0]).online();
            if (target.isEmpty()) {
                invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
                return 0;
            }
            location = platform.location(target.orElseThrow());
        }
        var damage = 0.0D;
        if (invocation.args().length == 2) {
            try {
                damage = Double.parseDouble(invocation.args()[1]);
                if (!Double.isFinite(damage) || damage < 0.0D || damage > config.lightningMaximumDamage) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException failure) {
                invocation.errorKey("commands.world.lightning.invalid-damage", Map.of("maximum", config.lightningMaximumDamage));
                return 0;
            }
        }
        var result = worlds.strikeLightning(new LightningRequest(location, false, damage));
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.lightning.success", Map.of(
                "world", location.world,
                "damage", damage,
                "visual", false
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.lightning.usage", Map.of("usage", usage()));
        return 0;
    }

}
