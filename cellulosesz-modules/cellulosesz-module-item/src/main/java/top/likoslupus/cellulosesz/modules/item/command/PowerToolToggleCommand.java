package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Locale;
import java.util.Map;

public final class PowerToolToggleCommand implements CellCommand {

    private final PlatformService platform;
    private final ItemAutomationService automation;
    private final UserService users;

    public PowerToolToggleCommand(
            PlatformService platform,
            ItemAutomationService automation,
            UserService users
    ) {
        this.platform = platform;
        this.automation = automation;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.powertooltoggle";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/powertooltoggle [on|off]";
    }

    @Override
    public String name() {
        return "powertooltoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        if (automation.powerTools(player.uuid()).isEmpty()) {
            invocation.errorKey("commands.item.powertool-toggle.empty");
            return 0;
        }
        final boolean enabled;
        if (invocation.args().length == 0) enabled = !automation.powerToolsEnabled(player.uuid());
        else enabled = switch (invocation.args()[0].toLowerCase(Locale.ROOT)) {
            case "on" -> true;
            case "off" -> false;
            default -> {
                yield !automation.powerToolsEnabled(player.uuid());
            }
        };
        if (invocation.args().length == 1
                && !invocation.args()[0].equalsIgnoreCase("on")
                && !invocation.args()[0].equalsIgnoreCase("off")) return usage(invocation);
        users.updateVoid(player.uuid(), user -> user.preferences.powerToolsEnabled = enabled)
                .whenComplete((ignored, failure) -> platform.runOnServerThread(() -> {
                    if (failure != null) invocation.errorKey("commands.item.powertool-toggle.save-failed");
                    else invocation.replyKey(
                            enabled
                                    ? "commands.item.powertool-toggle.enabled"
                                    : "commands.item.powertool-toggle.disabled"
                    );
                }));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.powertool-toggle.usage", Map.of("usage", usage()));
        return 0;
    }

}
