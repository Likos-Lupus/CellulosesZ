package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Comparator;
import java.util.Map;

public final class PowerToolListCommand implements CellCommand {

    private static final int PAGE_SIZE = 8;
    private final PlatformService platform;
    private final ItemAutomationService automation;

    public PowerToolListCommand(
            PlatformService platform,
            ItemAutomationService automation
    ) {
        this.platform = platform;
        this.automation = automation;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.powertoollist";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/powertoollist [page]";
    }

    @Override
    public String name() {
        return "powertoollist";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        var player = platform.player(invocation).orElseThrow();
        var entries = automation.powerTools(player.uuid()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .toList();
        if (entries.isEmpty()) {
            invocation.errorKey("commands.item.powertool-list.empty");
            return 0;
        }
        var pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        var page = 1;
        if (invocation.args().length == 1) {
            try {
                page = Integer.parseInt(invocation.args()[0]);
            } catch (NumberFormatException failure) {
                return usage(invocation);
            }
        }
        if (page < 1 || page > pages) {
            invocation.errorKey("commands.item.powertool-list.invalid-page", Map.of("pages", pages));
            return 0;
        }
        invocation.replyKey("commands.item.powertool-list.header", Map.of("page", page, "pages", pages));
        entries.subList((page - 1) * PAGE_SIZE, Math.min(entries.size(), page * PAGE_SIZE)).forEach(entry ->
                invocation.replyKey("commands.item.powertool-list.entry", Map.of(
                        "item", entry.getKey(),
                        "commands", String.join(", ", entry.getValue())
                ))
        );
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.powertool-list.usage", Map.of("usage", usage()));
        return 0;
    }

}
