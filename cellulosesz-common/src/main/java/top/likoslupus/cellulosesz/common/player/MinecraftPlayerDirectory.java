package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerDirectory implements PlayerDirectory {

    private final MinecraftServerHandle server;

    public MinecraftPlayerDirectory(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public List<CellPlayer> onlinePlayers() {
        var current = server.current();
        if (current.isEmpty()) {
            return List.of();
        }

        var active = current.orElseThrow();
        return active.getPlayerList().getPlayers().stream()
                .filter(player -> !player.hasDisconnected())
                .map(MinecraftPlayers::wrap)
                .sorted(
                        Comparator.comparing(CellPlayer::name, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(CellPlayer::uuid)
                )
                .toList();
    }

    @Override
    public @Nullable CellPlayer onlinePlayer(UUID uuid) {
        return onlinePlayers().stream()
                .filter(player -> player.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public @Nullable CellPlayer onlinePlayer(String name) {
        return name.isBlank()
                ? null
                : onlinePlayers().stream()
                        .filter(player -> player.name().equalsIgnoreCase(name))
                        .findFirst()
                        .orElse(null);
    }

    @Override
    public List<String> onlinePlayerNames() {
        return onlinePlayers().stream()
                .map(CellPlayer::name)
                .toList();
    }

}
