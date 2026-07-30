package top.likoslupus.cellulosesz.common.command;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.source.MinecraftCommandPolicyContext;

import java.util.Optional;

public final class CommandSources {

    private CommandSources() {
    }

    public static Optional<CellPlayer> player(
            MinecraftCommandPolicyContext policy,
            PlayerDirectory directory
    ) {
        return policy.playerUuid().flatMap(directory::onlinePlayer);
    }

}
