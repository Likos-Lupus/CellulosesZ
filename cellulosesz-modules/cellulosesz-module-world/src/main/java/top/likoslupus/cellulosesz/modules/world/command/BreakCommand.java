package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Map;

public final class BreakCommand implements CellCommand {

    private final PlatformService platform;
    private final WorldPlatformService worlds;
    private final WorldConfig config;

    public BreakCommand(
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
        return "cellulosesz.command.break";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "break";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) {
            invocation.errorKey("commands.world.break.usage", Map.of("usage", usage()));
            return 0;
        }
        var result = worlds.breakTarget(
                platform.player(invocation).orElseThrow(),
                config.targetDistance,
                invocation.hasPermission("cellulosesz.command.break.unbreakable")
        );
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.world.break.success", Map.of(
                "block", result.value().orElseThrow().blockId(),
                "drops", result.value().orElseThrow().dropsEnabled()
        ));
        return 1;
    }

}
