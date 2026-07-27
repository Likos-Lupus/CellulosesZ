package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.HatAction;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;

public final class HatCommand implements CellCommand {

    private final PlatformService platform;
    private final InventoryPlatformService inventory;

    public HatCommand(
            PlatformService platform,
            InventoryPlatformService inventory
    ) {
        this.platform = platform;
        this.inventory = inventory;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.hat";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/hat [remove]";
    }

    @Override
    public String name() {
        return "hat";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1
                || (invocation.args().length == 1 && !invocation.args()[0].equalsIgnoreCase("remove"))) {
            invocation.errorKey("commands.item.hat.usage", Map.of("usage", usage()));
            return 0;
        }
        var action = invocation.args().length == 0 ? HatAction.SWAP : HatAction.REMOVE;
        var result = inventory.hat(
                platform.player(invocation).orElseThrow(),
                action,
                invocation.hasPermission("cellulosesz.command.hat.ignore-binding")
        );
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey(action == HatAction.SWAP ? "commands.item.hat.swapped" : "commands.item.hat.removed");
        return 1;
    }

}
