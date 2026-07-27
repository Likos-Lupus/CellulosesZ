package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.TntBurstRequest;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;

public final class AntiochCommand implements CellCommand {

    private final PlatformService platform;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public AntiochCommand(
            PlatformService platform,
            EntityPlatformService entities,
            WorldConfig config
    ) {
        this.platform = platform;
        this.entities = entities;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.antioch";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/antioch [message]";
    }

    @Override
    public String name() {
        return "antioch";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        if (!config.destructiveCommandsEnabled) {
            invocation.errorKey("commands.world.destructive-disabled");
            return 0;
        }
        var player = platform.player(invocation).orElseThrow();
        var target = platform.targetLocation(player, config.targetDistance);
        if (target.isEmpty()) {
            invocation.errorKey("commands.world.antioch.no-target");
            return 0;
        }
        var amount = Math.min(1, config.antiochMaximumEntities);
        var result = entities.spawnTnt(new TntBurstRequest(
                target.orElseThrow(), amount, config.antiochFuseTicks,
                config.antiochExplosionPower, config.explosionBlockDamage, 0.0D, 0.0D
        ));
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.antioch.success", Map.of(
                "count", result.value().orElseThrow().spawned(),
                "message", invocation.args().length == 1 ? invocation.args()[0] : ""
        ));
        return result.value().orElseThrow().spawned();
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.antioch.usage", Map.of("usage", usage()));
        return 0;
    }

}
