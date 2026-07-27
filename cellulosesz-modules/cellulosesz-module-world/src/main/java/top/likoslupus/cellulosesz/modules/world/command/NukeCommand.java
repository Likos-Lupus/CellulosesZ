package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.TntBurstRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;
import java.util.Optional;

public final class NukeCommand implements CellCommand {

    private final PlatformService platform;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public NukeCommand(
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
        return "cellulosesz.command.nuke";
    }

    @Override
    public String usage() {
        return "/nuke [player]";
    }

    @Override
    public String name() {
        return "nuke";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        if (!config.destructiveCommandsEnabled || !config.nukeEnabled) {
            invocation.errorKey("commands.world.nuke.disabled");
            return 0;
        }
        var target = target(invocation);
        if (target.isEmpty()) return 0;
        var result = entities.spawnTnt(new TntBurstRequest(
                platform.location(target.orElseThrow()), config.nukeTntPerTarget,
                config.nukeFuseTicks, config.nukeExplosionPower, config.explosionBlockDamage,
                config.nukeSpread, config.nukeHeight
        ));
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        var value = result.value().orElseThrow();
        invocation.replyKey(value.spawned() == value.requested()
                ? "commands.world.nuke.success" : "commands.world.nuke.partial", Map.of(
                "player", target.orElseThrow().name(),
                "requested", value.requested(),
                "spawned", value.spawned()
        ));
        return value.spawned();
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.nuke.usage", Map.of("usage", usage()));
        return 0;
    }

    private Optional<CellPlayer> target(CommandInvocation invocation) {
        if (invocation.args().length == 0) {
            var self = platform.player(invocation);
            if (self.isEmpty()) invocation.errorKey("commands.world.nuke.console-target-required");
            return self;
        }
        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty())
            invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
        return target;
    }

}
