package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.MovementSpeedType;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SpeedCommand implements CellCommand {

    private final PlatformService platform;

    public SpeedCommand(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.speed";
    }

    @Override
    public String usage() {
        return "/speed <speed> | /speed <walk|fly> <speed> [player]";
    }

    @Override
    public String name() {
        return "speed";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1 || args.length > 3) return usage(invocation);

        final MovementSpeedType type;
        final String speedArgument;
        final Optional<CellPlayer> target;
        if (args.length == 1) {
            target = target(invocation, Optional.empty());
            if (target.isEmpty()) return 0;
            type = platform.flying(target.orElseThrow()) ? MovementSpeedType.FLY : MovementSpeedType.WALK;
            speedArgument = args[0];
        } else {
            var parsedType = parseType(args[0]);
            if (parsedType.isEmpty()) return usage(invocation);
            type = parsedType.orElseThrow();
            speedArgument = args[1];
            target = target(invocation, args.length == 3 ? Optional.of(args[2]) : Optional.empty());
            if (target.isEmpty()) return 0;
        }

        if (!invocation.hasPermission("cellulosesz.playerstate.speed." + type.name().toLowerCase(Locale.ROOT))) {
            invocation.errorKey("common.no-permission");
            return 0;
        }
        if (args.length == 3 && !invocation.hasPermission("cellulosesz.playerstate.speed.others")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }

        final double speed;
        try {
            speed = Double.parseDouble(speedArgument);
        } catch (NumberFormatException exception) {
            invocation.errorKey("commands.playerstate.speed-invalid");
            return 0;
        }
        if (!Double.isFinite(speed) || speed < 0.0001D || speed > 10.0D) {
            invocation.errorKey("commands.playerstate.speed-invalid");
            return 0;
        }

        if (!platform.setMovementSpeed(target.orElseThrow(), type, speed)) {
            invocation.errorKey("commands.playerstate.speed-failed");
            return 0;
        }
        invocation.replyKey("commands.playerstate.speed-set", Map.of(
                "player", target.orElseThrow().name(),
                "speed", speed,
                "type", type.name().toLowerCase(Locale.ROOT)
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.playerstate.speed-usage", Map.of("usage", usage()));
        return 0;
    }

    private Optional<CellPlayer> target(CommandInvocation invocation, Optional<String> name) {
        if (name.isEmpty()) {
            var self = platform.player(invocation);
            if (self.isEmpty()) invocation.errorKey("commands.common.player-required");
            return self;
        }
        var target = invocation.resolvePlayer(name.orElseThrow()).online();
        if (target.isEmpty())
            invocation.errorKey("commands.common.player-offline", Map.of("player", name.orElseThrow()));
        return target;
    }

    private Optional<MovementSpeedType> parseType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "walk", "w", "run", "r" -> Optional.of(MovementSpeedType.WALK);
            case "fly", "f" -> Optional.of(MovementSpeedType.FLY);
            default -> Optional.empty();
        };
    }

}
