package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.TemporaryMobRequest;
import top.likoslupus.cellulosesz.api.entity.TemporaryMobType;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;

public final class KittyCannonCommand implements CellCommand {

    private final PlatformService platform;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public KittyCannonCommand(
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
        return "cellulosesz.command.kittycannon";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/kittycannon";
    }

    @Override
    public String name() {
        return "kittycannon";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) return usage(invocation);
        var result = entities.launchTemporaryMob(new TemporaryMobRequest(
                platform.player(invocation).orElseThrow(), TemporaryMobType.CAT,
                config.temporaryMobSpeed, config.temporaryMobLifetimeTicks,
                config.temporaryMobExplosionPower, config.explosionBlockDamage
        ));
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.kittycannon.success");
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.world.kittycannon.usage", Map.of("usage", usage()));
        return 0;
    }

}
