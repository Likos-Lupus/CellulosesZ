package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.*;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ExpCommand implements CellCommand {

    private final PlatformService platform;
    private final PlayerStatePlatformService operations;

    public ExpCommand(
            PlatformService platform,
            PlayerStatePlatformService operations
    ) {
        this.platform = platform;
        this.operations = operations;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.exp";
    }

    @Override
    public String usage() {
        return "/exp <show|reset|set|give|take> [player] [amount]";
    }

    @Override
    public String name() {
        return "exp";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1 || invocation.args().length > 3) return usage(invocation);
        if (invocation.args()[0].equalsIgnoreCase("show")) return show(invocation);
        final ExperienceAction action;
        try {
            action = parseMutationAction(invocation.args()[0]);
        } catch (IllegalArgumentException failure) {
            return usage(invocation);
        }
        if (!allowed(invocation, action)) return 0;

        if (action == ExperienceAction.RESET) {
            if (invocation.args().length > 2) return usage(invocation);
            var target = target(invocation, invocation.args().length == 2 ? invocation.args()[1] : null, action);
            return target.map(player -> mutate(invocation, player, new ExperienceRequest(action, ExperienceUnit.POINTS, 0)))
                    .orElse(0);
        }
        if (invocation.args().length < 2) return usage(invocation);
        var amountIndex = invocation.args().length - 1;
        var parsed = parseAmount(invocation.args()[amountIndex]);
        if (parsed.isEmpty()) {
            invocation.errorKey("commands.playerstate.exp.invalid-amount", Map.of("amount", invocation.args()[amountIndex]));
            return 0;
        }
        var targetName = invocation.args().length == 3 ? invocation.args()[1] : null;
        var target = target(invocation, targetName, action);
        return target.map(player -> mutate(invocation, player, new ExperienceRequest(
                action,
                parsed.orElseThrow().unit(),
                parsed.orElseThrow().amount()
        ))).orElse(0);
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.playerstate.exp.usage", Map.of("usage", usage()));
        return 0;
    }

    private int show(CommandInvocation invocation) {
        if (invocation.args().length > 2) return usage(invocation);
        var target = target(invocation, invocation.args().length == 2 ? invocation.args()[1] : null, null);
        if (target.isEmpty()) return 0;
        var result = operations.experience(target.orElseThrow());
        if (!result.successful() || result.value().isEmpty()) {
            invocation.errorKey("commands.playerstate.exp.platform-failed");
            return 0;
        }
        reply(invocation, target.orElseThrow(), result.value().orElseThrow());
        return 1;
    }

    private static ExperienceAction parseMutationAction(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "reset" -> ExperienceAction.RESET;
            case "set" -> ExperienceAction.SET;
            case "give" -> ExperienceAction.GIVE;
            case "take" -> ExperienceAction.TAKE;
            default -> throw new IllegalArgumentException("Unknown action");
        };
    }

    private boolean allowed(CommandInvocation invocation, ExperienceAction action) {
        var permission = "cellulosesz.command.exp." + action.name().toLowerCase(Locale.ROOT);
        if (!invocation.hasPermission(permission)) {
            invocation.errorKey("commands.common.no-permission");
            return false;
        }
        return true;
    }

    private Optional<CellPlayer> target(CommandInvocation invocation, String name, ExperienceAction action) {
        var self = platform.player(invocation);
        if (name == null) {
            if (self.isEmpty()) {
                invocation.errorKey("commands.playerstate.exp.console-target-required");
                return Optional.empty();
            }
            return self;
        }
        var permission = action == null
                ? "cellulosesz.command.exp.others"
                : "cellulosesz.command.exp." + action.name().toLowerCase(Locale.ROOT) + ".others";
        if (!invocation.hasPermission(permission)) {
            invocation.errorKey("commands.common.no-permission");
            return Optional.empty();
        }
        var resolved = invocation.resolvePlayer(name).online();
        if (resolved.isEmpty()) invocation.errorKey("commands.common.unknown-player", Map.of("player", name));
        return resolved;
    }

    private int mutate(CommandInvocation invocation, CellPlayer target, ExperienceRequest request) {
        var result = operations.mutateExperience(target, request);
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        reply(invocation, target, result.value().orElseThrow());
        return 1;
    }

    private static Optional<ParsedAmount> parseAmount(String value) {
        var levels = value.endsWith("L") || value.endsWith("l");
        var raw = levels ? value.substring(0, value.length() - 1) : value;
        if (raw.isEmpty() || !raw.chars().allMatch(Character::isDigit)) return Optional.empty();
        try {
            return Optional.of(new ParsedAmount(
                    Integer.parseInt(raw),
                    levels ? ExperienceUnit.LEVELS : ExperienceUnit.POINTS
            ));
        } catch (NumberFormatException failure) {
            return Optional.empty();
        }
    }

    private static void reply(CommandInvocation invocation, CellPlayer target, ExperienceSnapshot snapshot) {
        invocation.replyKey("commands.playerstate.exp.result", Map.of(
                "player", target.name(),
                "total", snapshot.totalPoints(),
                "level", snapshot.level(),
                "progress", Math.round(snapshot.progress() * 1000.0D) / 10.0D,
                "next", snapshot.pointsToNextLevel()
        ));
    }

    private record ParsedAmount(
            int amount,
            ExperienceUnit unit
    ) {

    }

}
