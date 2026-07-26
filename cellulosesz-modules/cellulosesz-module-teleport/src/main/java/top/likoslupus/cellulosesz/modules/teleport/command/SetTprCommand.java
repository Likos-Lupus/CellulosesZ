package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;

import java.util.Map;

public final class SetTprCommand implements CellCommand {

    private final PlatformService platform;
    private final RandomTeleportSettingsService settings;

    public SetTprCommand(PlatformService platform, RandomTeleportSettingsService settings) {
        this.platform = platform;
        this.settings = settings;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.settpr";
    }

    @Override
    public String usage() {
        return "/settpr <world> <center|minrange|maxrange> [value]";
    }

    @Override
    public String name() {
        return "settpr";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2 || args.length > 3 || !platform.worlds().contains(args[0])) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        var world = args[0];
        var action = args[1].toLowerCase(java.util.Locale.ROOT);
        var current = settings.settings(world);
        if (args.length == 2 && !action.equals("center")) {
            replyCurrent(invocation, world, current, "commands.teleport.settpr.current");
            return 1;
        }

        java.util.Optional<java.util.concurrent.CompletableFuture<Void>> update;
        try {
            if (action.equals("center")) {
                var player = platform.player(invocation);
                if (player.isEmpty()) {
                    invocation.errorKey("commands.teleport.settpr.center-player-only");
                    return 0;
                }
                var location = platform.location(player.orElseThrow());
                if (!location.world.equals(world)) {
                    invocation.errorKey("commands.teleport.settpr.wrong-world", Map.of("world", world));
                    return 0;
                }
                update = java.util.Optional.of(settings.setCenter(world, location.x, location.z));
            } else if (action.equals("minrange")) {
                update = java.util.Optional.of(settings.setMinimumRadius(world, parseRadius(args)));
            } else if (action.equals("maxrange")) {
                update = java.util.Optional.of(settings.setMaximumRadius(world, parseRadius(args)));
            } else {
                update = java.util.Optional.empty();
            }
        } catch (NumberFormatException exception) {
            invocation.errorKey("commands.teleport.invalid-integer", Map.of("name", "value", "value", args[2]));
            return 0;
        } catch (IllegalArgumentException exception) {
            invocation.errorKey("commands.teleport.settpr.invalid-range");
            return 0;
        }
        if (update.isEmpty()) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        update.orElseThrow().whenComplete((unused, failure) -> {
            if (failure != null) {
                invocation.errorKey("common.persistence-failed");
                return;
            }
            replyCurrent(invocation, world, settings.settings(world), "commands.teleport.settpr.updated");
        });
        return 1;
    }

    private void replyCurrent(
            CommandInvocation invocation,
            String world,
            top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettings value,
            String key
    ) {
        invocation.replyKey(
                key,
                Map.of(
                        "world", world,
                        "centerX", value.centerX(),
                        "centerZ", value.centerZ(),
                        "min", value.minRadius(),
                        "max", value.maxRadius()
                )
        );
    }

    private int parseRadius(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("radius is required");
        return Integer.parseInt(args[2]);
    }

}
