package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.playerstate.config.PlayerStateConfig;

import java.util.Comparator;
import java.util.Map;

public final class NearCommand implements CellCommand {

    private final PlatformService platform;
    private final VanishService vanish;
    private final DisplayNameService displayNames;
    private volatile PlayerStateConfig config;

    public NearCommand(
            PlatformService platform,
            VanishService vanish,
            DisplayNameService displayNames,
            PlayerStateConfig config
    ) {
        this.platform = platform;
        this.vanish = vanish;
        this.displayNames = displayNames;
        this.config = config;
    }

    public void configure(PlayerStateConfig config) {
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.near";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/near [radius]";
    }

    @Override
    public String name() {
        return "near";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) return 0;

        var radius = config.nearRadius;
        if (invocation.args().length > 0) {
            try {
                radius = Integer.parseInt(invocation.args()[0]);
            } catch (NumberFormatException _) {
                invocation.errorKey("commands.playerstate.near-invalid-radius");
                return 0;
            }
        }

        if (radius < 1 || radius > config.maximumNearRadius) {
            invocation.errorKey(
                    "commands.playerstate.near-radius-range",
                    Map.of("maximum", config.maximumNearRadius)
            );
            return 0;
        }

        var origin = platform.location(self.get());
        var maximumSquared = (double) radius * radius;
        var entries = platform.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(self.get().uuid()))
                .filter(player -> vanish.canSee(self.get(), player.uuid()))
                .map(player -> new Nearby(player, platform.location(player)))
                .filter(entry -> entry.location.world.equals(origin.world))
                .map(entry -> new Distance(
                        entry.player,
                        Math.sqrt(square(entry.location.x - origin.x)
                                + square(entry.location.y - origin.y)
                                + square(entry.location.z - origin.z))
                ))
                .filter(entry -> entry.distance * entry.distance <= maximumSquared)
                .sorted(Comparator.comparingDouble(Distance::distance))
                .toList();
        if (entries.isEmpty()) {
            invocation.replyKey(
                    "commands.playerstate.near-empty",
                    Map.of("radius", radius)
            );
            return 1;
        }

        var text = new StringBuilder();
        entries.forEach(entry ->
                text.append('\n')
                        .append(displayNames.plainDisplayName(entry.player))
                        .append(" (")
                        .append(Math.round(entry.distance))
                        .append("m)"));
        invocation.replyKey(
                "commands.playerstate.near-list",
                Map.of(
                        "radius", radius,
                        "entries", text.toString()
                )
        );
        return 1;
    }

    private static double square(double value) {
        return value * value;
    }

    private record Nearby(
            CellPlayer player,
            CellLocation location
    ) {

    }

    private record Distance(
            CellPlayer player,
            double distance
    ) {

    }

}
