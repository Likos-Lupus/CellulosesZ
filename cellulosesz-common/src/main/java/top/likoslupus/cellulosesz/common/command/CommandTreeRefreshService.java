package top.likoslupus.cellulosesz.common.command;

import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import static java.util.Objects.requireNonNull;

public final class CommandTreeRefreshService {

    private final MinecraftServerHandle server;

    public CommandTreeRefreshService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    public void refreshOnlinePlayers() {
        server.current()
                .ifPresent(current -> {
                    if (!current.isSameThread()) {
                        throw new IllegalStateException(
                                "Command tree refresh must run on the server thread");
                    }
                    current.getPlayerList().getPlayers()
                            .forEach(player -> current.getCommands().sendCommands(player));
                });
    }

}
