package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;

public final class CompassCommand implements CellCommand {

    private static final String[] DIRECTION_KEYS = {
            "south", "south-west", "west", "north-west",
            "north", "north-east", "east", "south-east"
    };
    private final PlatformService platform;

    public CompassCommand(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.compass";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return "compass";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 0) {
            invocation.errorKey("commands.playerstate.compass.usage", Map.of("usage", usage()));
            return 0;
        }
        var player = platform.player(invocation).orElseThrow();
        var degrees = normalizeDegrees(platform.location(player).yaw);
        invocation.replyKey(directionMessageKey(degrees), Map.of(
                "degrees", Math.round(degrees * 10.0D) / 10.0D
        ));
        return 1;
    }

    static double normalizeDegrees(double yaw) {
        var value = yaw % 360.0D;
        return value < 0.0D ? value + 360.0D : value;
    }

    private static String directionMessageKey(double yaw) {
        return switch (directionKey(yaw)) {
            case "south" -> "commands.playerstate.compass.south";
            case "south-west" -> "commands.playerstate.compass.south-west";
            case "west" -> "commands.playerstate.compass.west";
            case "north-west" -> "commands.playerstate.compass.north-west";
            case "north" -> "commands.playerstate.compass.north";
            case "north-east" -> "commands.playerstate.compass.north-east";
            case "east" -> "commands.playerstate.compass.east";
            case "south-east" -> "commands.playerstate.compass.south-east";
            default -> throw new IllegalStateException("Unknown compass direction");
        };
    }

    static String directionKey(double yaw) {
        var degrees = normalizeDegrees(yaw);
        return DIRECTION_KEYS[(int) Math.floor((degrees + 22.5D) / 45.0D) & 7];
    }

}
