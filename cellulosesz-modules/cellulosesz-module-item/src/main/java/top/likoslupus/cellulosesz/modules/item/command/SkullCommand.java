package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.SkullRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;
import java.util.Optional;

public final class SkullCommand implements CellCommand {

    private final PlatformService platform;
    private final InventoryPlatformService inventory;

    public SkullCommand(
            PlatformService platform,
            InventoryPlatformService inventory
    ) {
        this.platform = platform;
        this.inventory = inventory;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.skull";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/skull [owner] [player]";
    }

    @Override
    public String name() {
        return "skull";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 2) return usage(invocation);
        var self = platform.player(invocation).orElseThrow();
        var owner = invocation.args().length == 0 ? self.name() : invocation.args()[0];
        final CellPlayer recipient;
        final boolean spawn;
        Optional<top.likoslupus.cellulosesz.api.item.InventoryItemSnapshot> expected = Optional.empty();
        if (invocation.args().length == 0) {
            if (!invocation.hasPermission("cellulosesz.command.skull.spawn")) return denied(invocation);
            recipient = self;
            spawn = true;
        } else if (invocation.args().length == 1) {
            if (!invocation.hasPermission("cellulosesz.command.skull.modify")) return denied(invocation);
            recipient = self;
            spawn = false;
            expected = platform.heldInventorySnapshot(self);
            if (expected.isEmpty()) {
                invocation.errorKey("commands.item.skull.held-head-required");
                return 0;
            }
        } else {
            if (!invocation.hasPermission("cellulosesz.command.skull.others")
                    || !invocation.hasPermission("cellulosesz.command.skull.spawn.others")) return denied(invocation);
            var target = invocation.resolvePlayer(invocation.args()[1]).online();
            if (target.isEmpty()) {
                invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[1]));
                return 0;
            }
            recipient = target.orElseThrow();
            spawn = true;
        }
        inventory.skull(new SkullRequest(owner, recipient, spawn, expected)).whenComplete((result, failure) ->
                platform.runOnServerThread(() -> {
                    if (failure != null || result == null) {
                        invocation.platformError(top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus.INTERNAL_ERROR);
                    } else if (!result.successful()) {
                        invocation.platformError(result.status());
                    } else {
                        invocation.replyKey("commands.item.skull.success", Map.of(
                                "owner", owner,
                                "player", recipient.name(),
                                "spawned", spawn
                        ));
                    }
                })
        );
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.skull.usage", Map.of("usage", usage()));
        return 0;
    }

    private int denied(CommandInvocation invocation) {
        invocation.errorKey("commands.common.no-permission");
        return 0;
    }

}
