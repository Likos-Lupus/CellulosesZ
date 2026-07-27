package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class ClearInventoryConfirmToggleCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;
    private final ConfirmationService confirmations;

    public ClearInventoryConfirmToggleCommand(
            PlatformService platform,
            UserService users,
            ConfirmationService confirmations
    ) {
        this.platform = platform;
        this.users = users;
        this.confirmations = confirmations;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.clearinventoryconfirmtoggle";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/clearinventoryconfirmtoggle [on|off]";
    }

    @Override
    public String name() {
        return "clearinventoryconfirmtoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        var current = users.cached(player.uuid()).map(user -> user.preferences.confirmInventoryClears).orElse(true);
        final boolean enabled;
        if (invocation.args().length == 0) enabled = !current;
        else if (invocation.args()[0].equalsIgnoreCase("on")) enabled = true;
        else if (invocation.args()[0].equalsIgnoreCase("off")) enabled = false;
        else return usage(invocation);

        users.updateVoid(player.uuid(), user -> user.preferences.confirmInventoryClears = enabled)
                .whenComplete((ignored, failure) -> platform.runOnServerThread(() -> {
                    if (failure != null) {
                        invocation.errorKey("commands.item.clearinventory-confirm.save-failed");
                        return;
                    }
                    confirmations.clear(player.uuid(), "clearinventory");
                    invocation.replyKey(enabled
                            ? "commands.item.clearinventory-confirm.enabled"
                            : "commands.item.clearinventory-confirm.disabled");
                }));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.clearinventory-confirm.usage", Map.of("usage", usage()));
        return 0;
    }

}
