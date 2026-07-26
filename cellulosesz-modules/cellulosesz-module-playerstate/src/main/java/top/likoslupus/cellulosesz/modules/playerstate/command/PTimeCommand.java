package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public final class PTimeCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;

    public PTimeCommand(PlatformService platform, UserService users) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.ptime";
    }

    @Override
    public String usage() {
        return "/ptime <day|night|dawn|noon|midnight|reset|ticks> [player]";
    }

    @Override
    public String name() {
        return "ptime";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1 || args.length > 2) {
            invocation.errorKey("commands.playerstate.ptime-usage", Map.of("usage", usage()));
            return 0;
        }
        if (args.length == 2 && !invocation.hasPermission("cellulosesz.playerstate.ptime.others")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }

        final TimeSetting setting;
        try {
            setting = parse(args[0]);
        } catch (IllegalArgumentException exception) {
            invocation.errorKey("commands.playerstate.ptime-invalid", Map.of("value", args[0]));
            return 0;
        }

        var target = target(invocation, args.length == 2 ? Optional.of(args[1]) : Optional.empty());
        if (target.isEmpty()) return 0;
        var player = target.orElseThrow();
        users.update(player.uuid(), user -> {
            var previous = user.state.personalTime == null
                    ? OptionalLong.empty()
                    : OptionalLong.of(user.state.personalTime);
            user.state.personalTime = setting.value().isPresent()
                    ? setting.value().getAsLong()
                    : null;
            return previous;
        }).whenComplete((previous, failure) -> platform.runOnServerThread(() -> {
            if (failure != null) {
                invocation.errorKey("service.user.persistence-failed");
                return;
            }
            var applied = platform.setPersonalTime(
                    player,
                    setting.value().isPresent() ? setting.value().getAsLong() : null
            );
            if (applied) {
                invocation.replyKey(
                        setting.value().isEmpty()
                                ? "commands.playerstate.ptime-reset"
                                : "commands.playerstate.ptime-set",
                        Map.of(
                                "player", player.name(),
                                "time", setting.value().orElse(0L)
                        )
                );
                return;
            }
            users.update(player.uuid(), user -> {
                user.state.personalTime = previous.isPresent() ? previous.getAsLong() : null;
                return true;
            }).whenComplete((ignored, rollbackFailure) -> platform.runOnServerThread(() ->
                    invocation.errorKey(rollbackFailure == null
                            ? "commands.playerstate.ptime-failed"
                            : "commands.playerstate.ptime-rollback-failed")
            ));
        }));
        return 1;
    }

    private static TimeSetting parse(String input) {
        var value = switch (input.toLowerCase(Locale.ROOT)) {
            case "reset" -> OptionalLong.empty();
            case "day" -> OptionalLong.of(1000L);
            case "noon" -> OptionalLong.of(6000L);
            case "night" -> OptionalLong.of(13000L);
            case "midnight" -> OptionalLong.of(18000L);
            case "dawn" -> OptionalLong.of(23000L);
            default -> {
                var parsed = Long.parseLong(input);
                if (parsed < 0L) throw new IllegalArgumentException("negative time");
                yield OptionalLong.of(parsed % 24000L);
            }
        };
        return new TimeSetting(value);
    }

    private Optional<CellPlayer> target(CommandInvocation invocation, Optional<String> name) {
        if (name.isEmpty()) {
            var self = platform.player(invocation);
            if (self.isEmpty()) invocation.errorKey("commands.common.player-required");
            return self;
        }
        var target = invocation.resolvePlayer(name.orElseThrow()).online();
        if (target.isEmpty()) invocation.errorKey(
                "commands.common.player-offline", Map.of("player", name.orElseThrow())
        );
        return target;
    }

    private record TimeSetting(OptionalLong value) {

    }

}
