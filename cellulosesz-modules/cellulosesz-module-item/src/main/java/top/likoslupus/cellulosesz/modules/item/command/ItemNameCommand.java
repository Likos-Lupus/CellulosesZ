package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;
import java.util.Objects;

public final class ItemNameCommand implements CellCommand {

    private final PlatformService platform;

    public ItemNameCommand(PlatformService platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.name";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/itemname <name|clear>";
    }

    @Override
    public String name() {
        return "itemname";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var player = platform.player(invocation);

        if (player.isEmpty()) {
            invocation.errorKey("commands.item.player-only");
            return 0;
        }

        var args = invocation.args();

        if (args.length != 1 || args[0].isBlank()) {
            invocation.errorKey(
                    "commands.item.itemname.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var clear = args[0].equalsIgnoreCase("clear");

        if (!platform.setHeldItemName(
                player.get(),
                clear ? null : args[0]
        )) {
            invocation.errorKey("commands.item.held-item-required");
            return 0;
        }

        invocation.replyKey(
                clear
                        ? "commands.item.itemname.cleared"
                        : "commands.item.itemname.set"
        );
        return 1;
    }

}
