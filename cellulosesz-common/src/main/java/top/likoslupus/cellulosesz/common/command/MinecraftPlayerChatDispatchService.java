package top.likoslupus.cellulosesz.common.command;

import net.minecraft.network.chat.Component;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.service.PlayerChatDispatchService;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerChatDispatchService implements PlayerChatDispatchService {

    private final MinecraftServerHandle server;

    public MinecraftPlayerChatDispatchService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<Void> dispatch(CellPlayer player, String message) {
        requireNonNull(message, "message");
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        if (message.isBlank()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Chat message is blank"
            );
        }

        try {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            server.requireRunning().getPlayerList().broadcastSystemMessage(
                    Component.translatable(
                            "chat.type.text",
                            nativePlayer.getDisplayName(),
                            Component.literal(message)
                    ),
                    false
            );

            return PlatformResult.success();
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

}
