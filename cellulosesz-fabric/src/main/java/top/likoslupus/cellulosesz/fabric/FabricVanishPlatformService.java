package top.likoslupus.cellulosesz.fabric;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.List;

public final class FabricVanishPlatformService implements VanishPlatformService {

    @Override
    public PlatformResult<Void> setVanishedState(
            CellPlayer player,
            boolean vanished
    ) {
        FabricVanishBridge.vanished(player.uuid(), vanished);
        return PlatformResult.success();
    }

    @Override
    public PlatformResult<Void> setVisible(
            CellPlayer viewer,
            CellPlayer target,
            boolean visible
    ) {
        if (!(viewer.nativeHandle() instanceof ServerPlayer viewerNative)
                || !(target.nativeHandle() instanceof ServerPlayer targetNative)
                || viewerNative.hasDisconnected() || targetNative.hasDisconnected()
        ) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    "Viewer or target is offline"
            );
        }

        if (!viewerNative.serverLevel().getServer().isSameThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    "Visibility changes require the server thread"
            );
        }

        if (viewerNative.getUUID().equals(targetNative.getUUID())) {
            return PlatformResult.success();
        }

        try {
            if (visible) {
                viewerNative.connection.send(
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(targetNative))
                );
                targetNative.startSeenByPlayer(viewerNative);
            } else {
                viewerNative.connection.send(
                        new ClientboundRemoveEntitiesPacket(targetNative.getId())
                );
                viewerNative.connection.send(
                        new ClientboundPlayerInfoRemovePacket(List.of(targetNative.getUUID()))
                );
                targetNative.stopSeenByPlayer(viewerNative);
            }

            return PlatformResult.success();
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

}
