package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class PotionCommand implements CellCommand {

    private static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+(?::[a-z0-9_./-]+)?$");
    private final PlatformService platform;

    public PotionCommand(PlatformService platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.potion";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/potion <effect|clear> [duration-seconds] [amplifier]";
    }

    @Override
    public String name() {
        return "potion";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var player = platform.player(invocation);

        if (player.isEmpty()) {
            invocation.errorKey("commands.item.player-only");
            return 0;
        }

        var args = invocation.args();

        if (args.length < 1 || args.length > 3) {
            invocation.errorKey(
                    "commands.item.potion.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (args.length != 1
                    || !platform.clearHeldItemComponent(player.get(), "minecraft:potion_contents")
            ) {
                invocation.errorKey("commands.item.potion.failed");
                return 0;
            }

            invocation.replyKey("commands.item.potion.cleared");
            return 1;
        }

        var effect = args[0].toLowerCase(Locale.ROOT);

        if (!ID.matcher(effect).matches()) {
            invocation.errorKey("commands.item.potion.invalid-effect");
            return 0;
        }

        effect = effect.indexOf(':') < 0
                ? "minecraft:" + effect
                : effect;
        var seconds = 180;
        var amplifier = 0;

        try {
            if (args.length >= 2) seconds = Integer.parseInt(args[1]);
            if (args.length >= 3) amplifier = Integer.parseInt(args[2]);
        } catch (NumberFormatException _) {
            invocation.errorKey("commands.item.potion.invalid-number");
            return 0;
        }

        if (seconds < 1
                || seconds > 1_000_000
                || amplifier < 0
                || amplifier > 255
        ) {
            invocation.errorKey("commands.item.potion.invalid-number");
            return 0;
        }

        var raw = "{custom_effects:[{id:\"%s\",duration:%d,amplifier:%d}]}".formatted(effect, seconds * 20L, amplifier);
        if (!platform.setHeldItemComponent(
                player.get(),
                "minecraft:potion_contents",
                raw
        )) {
            invocation.errorKey("commands.item.potion.failed");
            return 0;
        }

        invocation.replyKey(
                "commands.item.potion.set",
                Map.of(
                        "effect", effect,
                        "seconds", seconds,
                        "amplifier", amplifier
                )
        );
        return 1;
    }

}
