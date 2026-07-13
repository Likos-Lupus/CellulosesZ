package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class KitResetCommand extends AbstractKitCommand {

    private final UserService users;

    public KitResetCommand(
            PlatformService platform,
            KitService kits,
            UserService users
    ) {
        super(platform, kits);
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.reset";
    }

    @Override
    public String usage() {
        return "/kitreset <kit> [player]";
    }

    @Override
    public String name() {
        return "kitreset";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1 || args.length > 2) {
            invocation.errorKey(
                    "commands.kit.kit-reset-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        if (kits.kit(args[0]).isEmpty()) {
            invocation.errorKey(
                    "commands.kit.kit-reset-command.error.kit",
                    Map.of("kit", args[0])
            );
            return 0;
        }

        var targetName = args.length == 2
                ? args[1]
                : invocation.playerName().orElse("");
        if (targetName.isBlank()) {
            invocation.errorKey("commands.kit.kit-reset-command.error.player-required");
            return 0;
        }

        if (args.length == 2
                && !invocation.hasPermission("cellulosesz.kit.reset.others")
        ) {
            invocation.errorKey("commands.kit.kit-reset-command.error.others");
            return 0;
        }

        var resolved = invocation.resolvePlayer(targetName);
        var uuid = resolved.optionalUuid();
        if (uuid.isEmpty()) {
            invocation.errorKey(
                    "commands.kit.kit-reset-command.error.2",
                    Map.of("value0", targetName)
            );
            return 0;
        }

        try {
            kits.resetCooldown(uuid.get(), args[0]).join();
        } catch (RuntimeException _) {
            invocation.errorKey("service.kit.persistence-failed");
            return 0;
        }

        invocation.replyKey(
                "commands.kit.kit-reset-command.reply.1",
                Map.of(
                        "kit", args[0],
                        "player", resolved.name()
                )
        );
        return 1;
    }

}
