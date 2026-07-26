package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class WorkstationCommand implements CellCommand {

    private final PlatformService platform;
    private final String name;
    private final List<String> aliases;
    private final String workstation;

    public WorkstationCommand(
            PlatformService platform,
            String name,
            List<String> aliases,
            String workstation
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.name = requireNonNull(name, "name");
        this.aliases = List.copyOf(requireNonNull(aliases, "aliases"));
        this.workstation = requireNonNull(workstation, "workstation");
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }

    @Override
    public String permission() {
        return "cellulosesz.item.workstation." + workstation;
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var player = platform.player(invocation);

        if (player.isEmpty()) {
            invocation.errorKey("commands.item.player-only");
            return 0;
        }

        if (!platform.openWorkstation(player.get(), workstation)) {
            invocation.errorKey(
                    "commands.item.workstation.failed",
                    Map.of("workstation", workstation)
            );
            return 0;
        }

        invocation.replyKey(
                "commands.item.workstation.opened",
                Map.of("workstation", workstation)
        );
        return 1;
    }

}
