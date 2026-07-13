package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.List;
import java.util.Map;

public final class PowerToolCommand extends AbstractItemCommand {

    public PowerToolCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("pt");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.powertool";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/powertool <command|a:command|r:command|c:message|l:|d:|toggle|list|clearall>";
    }

    @Override
    public String name() {
        return "powertool";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();

        if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            var enabled = !automation.powerToolsEnabled(self.get().uuid());
            automation.setPowerToolsEnabled(self.get().uuid(), enabled);
            invocation.replyKey(enabled
                    ? "commands.item.power-tool-command.enabled"
                    : "commands.item.power-tool-command.disabled");
            return 1;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var configured = automation.powerTools(self.get().uuid());
            if (configured.isEmpty()) {
                invocation.replyKey("commands.item.power-tool-command.reply.1");
            } else {
                invocation.replyKey(
                        "commands.item.power-tool-command.reply.2",
                        Map.of(
                                "value0",
                                configured.entrySet().stream()
                                        .map(entry ->
                                                "%s -> %s".formatted(
                                                        entry.getKey(),
                                                        String.join(" | ", entry.getValue())
                                                )
                                        )
                                        .sorted()
                                        .reduce("%s; %s"::formatted)
                                        .orElse("")
                        )
                );
            }
            return 1;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clearall")) {
            automation.powerTools(self.get().uuid()).keySet()
                    .forEach(itemId ->
                            automation.clearPowerTool(self.get().uuid(), itemId)
                    );
            invocation.replyKey("commands.item.power-tool-command.reply.3");
            return 1;
        }

        var held = items.heldItemId(self.get());
        if (held.isEmpty()) {
            invocation.errorKey("commands.item.power-tool-command.error.1");
            return 0;
        }

        var input = join(args, 0).trim();
        if (input.isEmpty()
                || input.equalsIgnoreCase("clear")
                || input.equalsIgnoreCase("d:")
        ) {
            automation.clearPowerTool(self.get().uuid(), held.get());
            invocation.replyKey(
                    "commands.item.power-tool-command.reply.4",
                    Map.of("value0", held.get())
            );
            return 1;
        }

        if (input.equalsIgnoreCase("l:")) {
            var commands = automation.powerTool(self.get().uuid(), held.get());
            invocation.replyKey(
                    "commands.item.power-tool-command.held-list",
                    Map.of(
                            "item", held.get(),
                            "commands", commands.isEmpty() ? "-" : String.join(" | ", commands)
                    )
            );
            return 1;
        }

        if (input.regionMatches(true, 0, "a:", 0, 2)) {
            var command = input.substring(2).trim();
            if (command.isEmpty()) return usageError(invocation);

            automation.addPowerTool(self.get().uuid(), held.get(), command);
            return configured(invocation, held.get(), command);
        }

        if (input.regionMatches(true, 0, "r:", 0, 2)) {
            var command = input.substring(2).trim();
            if (!automation.removePowerTool(self.get().uuid(), held.get(), command)) {
                invocation.errorKey("commands.item.power-tool-command.error.not-found");
                return 0;
            }

            invocation.replyKey(
                    "commands.item.power-tool-command.removed",
                    Map.of("command", command)
            );
            return 1;
        }

        if (input.regionMatches(true, 0, "c:", 0, 2)) {
            var message = input.substring(2).trim();
            if (message.isEmpty()) return usageError(invocation);

            automation.setPowerTool(self.get().uuid(), held.get(), "c:" + message);
            return configured(invocation, held.get(), "c:" + message);
        }

        var command = input.replaceFirst("^/+", "").trim();
        if (command.isBlank()) return usageError(invocation);

        automation.setPowerTool(self.get().uuid(), held.get(), command);
        return configured(invocation, held.get(), command);
    }

    private int usageError(CommandInvocation invocation) {
        invocation.errorKey(
                "commands.item.power-tool-command.error.2",
                Map.of("value0", usage())
        );
        return 0;
    }

    private int configured(
            CommandInvocation invocation,
            String item,
            String command
    ) {
        invocation.replyKey(
                "commands.item.power-tool-command.reply.5",
                Map.of(
                        "value0", item,
                        "value1", command
                )
        );
        return 1;
    }

}
