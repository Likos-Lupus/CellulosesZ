package top.likoslupus.cellulosesz.modules.sign.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignService;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.world.SignTarget;
import top.likoslupus.cellulosesz.api.world.SignTextMutation;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.sign.SignConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditSignCommand implements CellCommand {

    private final PlatformService platform;
    private final WorldPlatformService worlds;
    private final SignService signs;
    private final SignConfig config;
    private final Map<UUID, Clipboard> clipboards = new ConcurrentHashMap<>();

    public EditSignCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            SignService signs,
            SignConfig config
    ) {
        this.platform = platform;
        this.worlds = worlds;
        this.signs = signs;
        this.config = config;
    }

    private static void sendResult(CommandInvocation invocation, SignUseResult result) {
        if (result.success()) invocation.replyKey("commands.sign.editsign.success");
        else result.optionalMessage().ifPresent(message -> invocation.errorKey(message.key(), message.placeholders()));
    }

    private static int line(CommandInvocation invocation, String raw) {
        try {
            var line = Integer.parseInt(raw);
            if (line >= 1 && line <= 4) return line - 1;
        } catch (NumberFormatException ignored) {
            // handled below
        }
        invocation.errorKey("commands.sign.editsign.invalid-line");
        return -1;
    }

    private static boolean formatAllowed(CommandInvocation invocation, String text) {
        if (text.indexOf('§') >= 0 && !invocation.hasPermission("cellulosesz.command.editsign.color")) return false;
        if (text.matches("(?s).*<#[0-9a-fA-F]{6}>.*") && !invocation.hasPermission("cellulosesz.command.editsign.rgb"))
            return false;
        return (!text.contains("<") && !text.contains(">"))
                || invocation.hasPermission("cellulosesz.command.editsign.format");
    }

    private static String side(boolean front) {
        return front ? "front" : "back";
    }

    @Override
    public String permission() {
        return "cellulosesz.command.editsign";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/editsign <set <line> <text>|clear <line>|copy|paste>";
    }

    @Override
    public String name() {
        return "editsign";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        var targetResult = worlds.targetSign(player, config.editTargetDistance);
        if (!targetResult.successful() || targetResult.value().isEmpty()) {
            invocation.errorKey("commands.sign.editsign.no-sign");
            return 0;
        }
        var target = targetResult.value().orElseThrow();
        return switch (invocation.args()[0].toLowerCase()) {
            case "copy" -> copy(invocation, target);
            case "paste" -> paste(invocation, target);
            case "clear" -> clear(invocation, target);
            case "set" -> set(invocation, target);
            default -> usage(invocation);
        };
    }

    public void clearClipboard(UUID playerUuid) {
        clipboards.remove(playerUuid);
    }

    private int copy(CommandInvocation invocation, SignTarget target) {
        if (invocation.args().length != 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        clipboards.put(player.uuid(), new Clipboard(target.front(), target.lines()));
        invocation.replyKey("commands.sign.editsign.copied", Map.of("side", side(target.front())));
        return 1;
    }

    private int paste(CommandInvocation invocation, SignTarget target) {
        if (invocation.args().length != 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        var clipboard = clipboards.get(player.uuid());
        if (clipboard == null) {
            invocation.errorKey("commands.sign.editsign.clipboard-empty");
            return 0;
        }
        return mutate(invocation, target, clipboard.lines());
    }

    private int clear(CommandInvocation invocation, SignTarget target) {
        if (invocation.args().length != 2) return usage(invocation);
        var line = line(invocation, invocation.args()[1]);
        if (line < 0) return 0;
        var replacement = new ArrayList<>(target.lines());
        replacement.set(line, "");
        return mutate(invocation, target, replacement);
    }

    private int set(CommandInvocation invocation, SignTarget target) {
        if (invocation.args().length != 3) return usage(invocation);
        var line = line(invocation, invocation.args()[1]);
        if (line < 0) return 0;
        var text = invocation.args()[2];
        if (text.length() > config.editMaximumLineLength || text.chars()
                .anyMatch(value -> value == 0 || value == '\r' || value == '\n')) {
            invocation.errorKey("commands.sign.editsign.invalid-text", Map.of("maximum", config.editMaximumLineLength));
            return 0;
        }
        if (!formatAllowed(invocation, text)) {
            invocation.errorKey("commands.sign.editsign.format-denied");
            return 0;
        }
        var replacement = new ArrayList<>(target.lines());
        replacement.set(line, text);
        return mutate(invocation, target, replacement);
    }

    private int mutate(CommandInvocation invocation, SignTarget target, List<String> requested) {
        var player = platform.player(invocation).orElseThrow();
        var allowWaxed = invocation.hasPermission("cellulosesz.command.editsign.waxed");
        if (target.waxed() && !allowWaxed) {
            invocation.errorKey("commands.sign.editsign.waxed");
            return 0;
        }
        var replacement = signs.formattedLines(List.copyOf(requested));
        var execution = signs.edit(player, target.location(), target.front(), target.lines(), replacement);
        if (!execution.handled()) {
            var result = worlds.replaceSignText(player, new SignTextMutation(target, replacement), allowWaxed);
            if (!result.successful()) {
                invocation.platformError(result.status());
                return 0;
            }
            invocation.replyKey("commands.sign.editsign.success", Map.of("side", side(target.front())));
            return 1;
        }
        execution.preparation().whenComplete((commit, preparationFailure) -> platform.runOnServerThread(() -> {
            if (preparationFailure != null) {
                invocation.errorKey("commands.sign.editsign.failed", Map.of("reason", "preparation"));
                return;
            }
            var applied = worlds.replaceSignText(player, new SignTextMutation(target, replacement), allowWaxed)
                    .successful();
            commit.complete(applied).whenComplete((result, completionFailure) -> platform.runOnServerThread(() -> {
                if (completionFailure != null || !applied) {
                    invocation.errorKey("commands.sign.editsign.failed", Map.of("reason", "commit"));
                    return;
                }
                sendResult(invocation, result);
            }));
        }));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.sign.editsign.usage", Map.of("usage", usage()));
        return 0;
    }

    private record Clipboard(
            boolean front,
            List<String> lines
    ) {

        private Clipboard {
            lines = List.copyOf(lines);
        }

    }

}
