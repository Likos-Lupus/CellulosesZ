package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.List;

public final class KitCommand extends AbstractKitCommand {

    public KitCommand(
            PlatformService platform,
            KitService kits
    ) {
        super(platform, kits);
    }

    @Override
    public List<String> aliases() {
        return List.of("kits");
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.use";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/kit [name] 或 /kits";
    }

    @Override
    public String name() {
        return "kit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length == 0 || invocation.label().equalsIgnoreCase("kits")) {
            var names = kits.kits().stream()
                    .filter(kit -> kit.permission.isBlank() || invocation.hasPermission(kit.permission))
                    .map(kit -> kit.id)
                    .toList();
            invocation.reply(names.isEmpty() ? "当前没有可用 Kit。" : "Kit: " + String.join(", ", names));
            return 1;
        }

        var kit = kits.kit(args[0]);
        if (kit.isEmpty()) {
            invocation.error("Kit 不存在: " + args[0]);
            return 0;
        }
        if (!kit.get().permission.isBlank() && !invocation.hasPermission(kit.get().permission)) {
            invocation.error("你没有权限领取此 Kit。 ");
            return 0;
        }

        kits.claim(self.get(), kit.get()).thenAccept(result -> {
            if (result.success()) {
                invocation.reply(result.message());
            } else {
                invocation.error(result.message());
            }
        });
        return 1;
    }

}
