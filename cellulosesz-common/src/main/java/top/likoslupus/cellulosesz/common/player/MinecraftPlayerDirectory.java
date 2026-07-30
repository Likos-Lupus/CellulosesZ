package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerDirectory implements PlayerDirectory {

    private final MinecraftServerHandle server;

    public MinecraftPlayerDirectory(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public List<CellPlayer> onlinePlayers() {
        var current = server.current();
        if (current.isEmpty()) return List.of();
        try (var server = current.orElseThrow()) {
            return server.getPlayerList().getPlayers().stream()
                    .filter(player -> !player.hasDisconnected())
                    .map(MinecraftPlayers::wrap)
                    .sorted(
                            Comparator.comparing(CellPlayer::name, String.CASE_INSENSITIVE_ORDER)
                                    .thenComparing(CellPlayer::uuid)
                    )
                    .toList();
        }
    }

    @Override
    public Optional<CellPlayer> onlinePlayer(UUID uuid) {
        return onlinePlayers().stream()
                .filter(player -> player.uuid().equals(uuid))
                .findFirst();
    }

    @Override
    public Optional<CellPlayer> onlinePlayer(String name) {
        if (name.isBlank()) {
            return Optional.empty();
        }
        return onlinePlayers().stream()
                .filter(player -> player.name().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<String> onlinePlayerNames() {
        return onlinePlayers().stream()
                .map(CellPlayer::name)
                .toList();
    }

}
