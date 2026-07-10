package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

public final class CreateKitCommand extends AbstractKitCommand {

    private final ItemService items;

    public CreateKitCommand(
            PlatformService platform,
            KitService kits,
            ItemService items
    ) {
        super(platform, kits);
        this.items = items;
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.create";
    }

    @Override
    public String usage() {
        return "/createkit <name> <item> [count]";
    }

    @Override
    public String name() {
        return "createkit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2 || args.length > 3) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var descriptor = items.parse(args[1] + (args.length == 3 ? " " + args[2] : ""));
        if (descriptor.isEmpty()) {
            invocation.error("物品格式错误。 ");
            return 0;
        }

        var kit = new KitDefinition();
        kit.id = args[0];
        kit.displayName = args[0];
        kit.permission = "cellulosesz.kit." + args[0].toLowerCase();
        kit.items.add(new KitItem(descriptor.get().normalizedItem(), descriptor.get().count));
        kits.save(kit).thenRun(() -> invocation.reply("已创建 Kit: " + kit.id));
        return 1;
    }

}
