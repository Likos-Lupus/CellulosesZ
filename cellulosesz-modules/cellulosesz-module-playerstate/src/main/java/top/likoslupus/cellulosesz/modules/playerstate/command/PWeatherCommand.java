package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PWeatherCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;

    public PWeatherCommand(PlatformService platform, UserService users) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.pweather";
    }

    @Override
    public String usage() {
        return "/pweather <clear|rain|thunder|reset> [player]";
    }

    @Override
    public String name() {
        return "pweather";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1 || args.length > 2) {
            invocation.errorKey("commands.playerstate.pweather-usage", Map.of("usage", usage()));
            return 0;
        }
        if (args.length == 2 && !invocation.hasPermission("cellulosesz.playerstate.pweather.others")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }

        var setting = parse(args[0]);
        if (setting.isEmpty()) {
            invocation.errorKey("commands.playerstate.pweather-invalid", Map.of("value", args[0]));
            return 0;
        }
        var target = target(invocation, args.length == 2 ? Optional.of(args[1]) : Optional.empty());
        if (target.isEmpty()) return 0;
        var player = target.orElseThrow();
        var desired = setting.orElseThrow().value();

        users.update(player.uuid(), user -> {
            var previous = Optional.ofNullable(user.state.personalWeather);
            user.state.personalWeather = desired.orElse(null);
            return previous;
        }).whenComplete((previous, failure) -> platform.runOnServerThread(() -> {
            if (failure != null) {
                invocation.errorKey("service.user.persistence-failed");
                return;
            }
            if (platform.setPersonalWeather(player, desired.orElse(null))) {
                invocation.replyKey(
                        desired.isEmpty()
                                ? "commands.playerstate.pweather-reset"
                                : "commands.playerstate.pweather-set",
                        Map.of("player", player.name(), "weather", desired.orElse("reset"))
                );
                return;
            }
            users.update(player.uuid(), user -> {
                user.state.personalWeather = previous.orElse(null);
                return true;
            }).whenComplete((ignored, rollbackFailure) -> platform.runOnServerThread(() ->
                    invocation.errorKey(rollbackFailure == null
                            ? "commands.playerstate.pweather-failed"
                            : "commands.playerstate.pweather-rollback-failed")
            ));
        }));
        return 1;
    }

    private Optional<WeatherSetting> parse(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "reset" -> Optional.of(new WeatherSetting(Optional.empty()));
            case "clear", "rain", "thunder" -> Optional.of(
                    new WeatherSetting(Optional.of(input.toLowerCase(Locale.ROOT)))
            );
            default -> Optional.empty();
        };
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

    private record WeatherSetting(Optional<String> value) {

    }

}
