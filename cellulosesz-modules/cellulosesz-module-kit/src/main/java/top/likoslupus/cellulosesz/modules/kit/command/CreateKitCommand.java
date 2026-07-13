package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Locale;
import java.util.Map;

public final class CreateKitCommand extends AbstractKitCommand {

    public CreateKitCommand(
            PlatformService platform,
            KitService kits
    ) {
        super(platform, kits);
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.create";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/createkit <name> <cooldownSeconds|once>";
    }

    @Override
    public String name() {
        return "createkit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length != 2) {
            invocation.errorKey(
                    "commands.kit.create-kit-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        long cooldown;
        if (args[1].equalsIgnoreCase("once")
                || args[1].equalsIgnoreCase("one-time")
        ) {
            cooldown = -1L;
        } else {
            try {
                cooldown = Long.parseLong(args[1]);
            } catch (NumberFormatException _) {
                invocation.errorKey("commands.kit.create-kit-command.error.cooldown");
                return 0;
            }
            if (cooldown < 0L) {
                invocation.errorKey("commands.kit.create-kit-command.error.cooldown");
                return 0;
            }
        }

        var inventory = platform.inventoryItems(self.get());
        if (inventory.isEmpty()) {
            invocation.errorKey("commands.kit.create-kit-command.error.empty");
            return 0;
        }

        var kit = new KitDefinition();
        kit.id = args[0].trim().toLowerCase(Locale.ROOT);
        kit.displayName = args[0];
        kit.permission = "cellulosesz.kit." + kit.id;
        kit.cooldownSeconds = cooldown;
        inventory.forEach(item -> {
            var kitItem = new KitItem(item.normalizedItem(), item.count);
            kitItem.components.putAll(item.normalizedComponents());
            kit.items.add(kitItem);
        });
        try {
            kits.save(kit).join();
        } catch (RuntimeException _) {
            invocation.errorKey("service.kit.persistence-failed");
            return 0;
        }

        invocation.replyKey(
                "commands.kit.create-kit-command.reply.1",
                Map.of("value0", kit.id)
        );
        return 1;
    }

}
