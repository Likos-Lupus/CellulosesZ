package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class ItemLoreCommand implements CellCommand {

    private final PlatformService platform;
    private final ItemConfig config;

    public ItemLoreCommand(
            PlatformService platform,
            ItemConfig config
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.lore";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/itemlore <clear|text separated by \\n>";
    }

    @Override
    public String name() {
        return "itemlore";
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
                    "commands.item.itemlore.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var lines = args[0].equalsIgnoreCase("clear")
                ? List.<String>of()
                : Arrays.stream(args[0].split("\\\\n", -1))
                        .map(String::trim)
                        .toList();

        if (lines.size() > config.maxLoreLines
                || lines.stream().anyMatch(String::isBlank)
        ) {
            invocation.errorKey(
                    "commands.item.itemlore.invalid",
                    Map.of("maximum", config.maxLoreLines)
            );
            return 0;
        }

        if (!platform.setHeldItemLore(player.get(), lines)) {
            invocation.errorKey("commands.item.held-item-required");
            return 0;
        }

        invocation.replyKey(
                lines.isEmpty()
                        ? "commands.item.itemlore.cleared"
                        : "commands.item.itemlore.set",
                Map.of("lines", lines.size())
        );
        return 1;
    }

}
