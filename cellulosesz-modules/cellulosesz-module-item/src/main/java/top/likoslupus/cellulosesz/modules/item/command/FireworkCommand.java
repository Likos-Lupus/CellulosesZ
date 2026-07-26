package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class FireworkCommand implements CellCommand {

    private static final Set<String> SHAPES = Set.of(
            "small_ball", "large_ball", "star", "creeper", "burst"
    );
    private final PlatformService platform;

    public FireworkCommand(PlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.firework";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/firework <clear|power|effect> ...";
    }

    @Override
    public String name() {
        return "firework";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var player = platform.player(invocation);

        if (player.isEmpty()) {
            invocation.errorKey("commands.item.player-only");
            return 0;
        }

        var args = invocation.args();
        if (args.length < 1) return usage(invocation);

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                if (args.length != 1
                        || !platform.clearHeldItemComponent(player.get(), "minecraft:fireworks")
                ) {
                    invocation.errorKey("commands.item.firework.failed");
                    return 0;
                }

                invocation.replyKey("commands.item.firework.cleared");
                return 1;
            }

            case "power" -> {
                if (args.length != 2) return usage(invocation);

                try {
                    var power = Integer.parseInt(args[1]);
                    if (power < 0 || power > 4) return usage(invocation);

                    if (!platform.setHeldItemComponent(
                            player.get(),
                            "minecraft:fireworks",
                            "{flight_duration:%d}".formatted(power)
                    )) {
                        invocation.errorKey("commands.item.firework.failed");
                        return 0;
                    }

                    invocation.replyKey(
                            "commands.item.firework.power",
                            Map.of("power", power)
                    );
                    return 1;
                } catch (NumberFormatException _) {
                    return usage(invocation);
                }
            }

            case "effect" -> {
                if (args.length < 3 || args.length > 5) return usage(invocation);

                var shape = args[1].toLowerCase(Locale.ROOT);
                if (!SHAPES.contains(shape)) return usage(invocation);

                try {
                    var color = parseColor(args[2]);
                    var fade = args.length >= 4
                            ? parseColor(args[3])
                            : color;
                    var flags = args.length == 5
                            ? parseFlags(args[4])
                            : FireworkFlags.NONE;
                    var trail = flags.trail();
                    var twinkle = flags.twinkle();
                    var raw = "{flight_duration:1,explosions:[{shape:\"%s\",colors:[%d],fade_colors:[%d],has_trail:%s,has_twinkle:%s}]}"
                            .formatted(shape, color, fade, trail, twinkle);

                    if (!platform.setHeldItemComponent(
                            player.get(),
                            "minecraft:fireworks",
                            raw
                    )) {
                        invocation.errorKey("commands.item.firework.failed");
                        return 0;
                    }

                    invocation.replyKey(
                            "commands.item.firework.effect",
                            Map.of("shape", shape)
                    );
                    return 1;
                } catch (IllegalArgumentException _) {
                    return usage(invocation);
                }
            }
        }

        return usage(invocation);
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey(
                "commands.item.firework.usage",
                Map.of("usage", usage())
        );
        return 0;
    }

    private static int parseColor(String value) {
        var normalized = value.startsWith("#")
                ? value.substring(1)
                : value;
        if (normalized.length() != 6) throw new IllegalArgumentException("Invalid RGB color");
        return Integer.parseInt(normalized, 16);
    }

    static FireworkFlags parseFlags(String value) {
        var tokens = Arrays.stream(value.toLowerCase(Locale.ROOT).split("[,+]", -1))
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
        if (tokens.isEmpty()
                || tokens.contains("")
                || tokens.stream().anyMatch(token ->
                !token.equals("trail") && !token.equals("twinkle")
        )) {
            throw new IllegalArgumentException("Invalid firework flags");
        }
        return new FireworkFlags(tokens.contains("trail"), tokens.contains("twinkle"));
    }

    record FireworkFlags(
            boolean trail,
            boolean twinkle
    ) {

        private static final FireworkFlags NONE = new FireworkFlags(false, false);

    }

}
