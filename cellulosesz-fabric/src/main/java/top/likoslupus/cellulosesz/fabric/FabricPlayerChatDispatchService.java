package top.likoslupus.cellulosesz.fabric;

import net.minecraft.network.chat.Component;
import top.likoslupus.cellulosesz.api.command.service.PlayerChatDispatchService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import static java.util.Objects.requireNonNull;

public final class FabricPlayerChatDispatchService implements PlayerChatDispatchService {

    private final FabricServerAccess access;

    public FabricPlayerChatDispatchService(FabricServerAccess access) {
        this.access = requireNonNull(access, "access");
    }

    @Override
    public PlatformResult<Void> dispatch(CellPlayer player, String message) {
        requireNonNull(message, "message");
        if (!access.serverThread()) {
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
            var nativePlayer = access.player(player);
            access.requireServer().getPlayerList().broadcastSystemMessage(
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
