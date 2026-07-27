package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;

import java.util.Map;
import java.util.Optional;

public final class GetPosCommand implements CellCommand {

    private final PlatformService platform;
    private final VanishService vanish;

    public GetPosCommand(
            PlatformService platform,
            VanishService vanish
    ) {
        this.platform = platform;
        this.vanish = vanish;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.getpos";
    }

    @Override
    public String usage() {
        return "/getpos [player]";
    }

    @Override
    public String name() {
        return "getpos";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.playerstate.getpos.usage", Map.of("usage", usage()));
            return 0;
        }
        var viewer = platform.player(invocation);
        final CellPlayer target;
        if (invocation.args().length == 0) {
            if (viewer.isEmpty()) {
                invocation.errorKey("commands.playerstate.getpos.console-target-required");
                return 0;
            }
            target = viewer.orElseThrow();
        } else {
            if (!invocation.hasPermission("cellulosesz.command.getpos.others")) {
                invocation.errorKey("commands.common.no-permission");
                return 0;
            }
            var resolved = invocation.resolvePlayer(invocation.args()[0]);
            if (resolved.online().isEmpty() || hidden(viewer, resolved.online().orElseThrow())) {
                invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
                return 0;
            }
            target = resolved.online().orElseThrow();
        }

        var location = platform.location(target);
        var distance = distance(viewer, target);
        invocation.replyKey("commands.playerstate.getpos.result", Map.ofEntries(
                Map.entry("player", target.name()),
                Map.entry("world", location.world),
                Map.entry("blockX", (int) Math.floor(location.x)),
                Map.entry("blockY", (int) Math.floor(location.y)),
                Map.entry("blockZ", (int) Math.floor(location.z)),
                Map.entry("x", round(location.x)),
                Map.entry("y", round(location.y)),
                Map.entry("z", round(location.z)),
                Map.entry("yaw", round(location.yaw)),
                Map.entry("pitch", round(location.pitch)),
                Map.entry("distance", distance.map(GetPosCommand::round).map(Object::toString).orElse("-"))
        ));
        return 1;
    }

    private boolean hidden(Optional<CellPlayer> viewer, CellPlayer target) {
        return viewer.isPresent() && !vanish.canSee(viewer.orElseThrow(), target.uuid());
    }

    private Optional<Double> distance(Optional<CellPlayer> viewer, CellPlayer target) {
        if (viewer.isEmpty() || viewer.orElseThrow().uuid().equals(target.uuid())) return Optional.empty();
        var from = platform.location(viewer.orElseThrow());
        var to = platform.location(target);
        if (!from.world.equals(to.world)) return Optional.empty();
        return Optional.of(Math.sqrt(
                Math.pow(from.x - to.x, 2.0D)
                        + Math.pow(from.y - to.y, 2.0D)
                        + Math.pow(from.z - to.z, 2.0D)
        ));
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

}
