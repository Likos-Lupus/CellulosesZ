package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;

import java.util.Map;

public final class BurnCommand implements CellCommand {

    private final PlayerStatePlatformService players;
    private final AdminConfig config;

    public BurnCommand(PlayerStatePlatformService players, AdminConfig config) {
        this.players = players;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.burn";
    }

    @Override
    public String usage() {
        return "/burn <player> <seconds>";
    }

    @Override
    public String name() {
        return "burn";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 2) return usage(invocation);
        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty()) {
            invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
            return 0;
        }
        final int seconds;
        final int ticks;
        try {
            seconds = Integer.parseInt(invocation.args()[1]);
            if (seconds < 0 || seconds > config.maximumBurnSeconds) throw new NumberFormatException();
            ticks = Math.multiplyExact(seconds, 20);
        } catch (NumberFormatException | ArithmeticException failure) {
            invocation.errorKey("commands.admin.burn.invalid-seconds", Map.of("maximum", config.maximumBurnSeconds));
            return 0;
        }
        var result = players.setFireTicks(target.orElseThrow(), ticks);
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey(seconds == 0 ? "commands.admin.burn.extinguished" : "commands.admin.burn.success", Map.of(
                "player", target.orElseThrow().name(),
                "seconds", seconds
        ));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.admin.burn.usage", Map.of("usage", usage()));
        return 0;
    }

}
