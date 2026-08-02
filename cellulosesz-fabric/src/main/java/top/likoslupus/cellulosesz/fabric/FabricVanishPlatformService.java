package top.likoslupus.cellulosesz.fabric;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.playerstate.VanishPlatformService;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class FabricVanishPlatformService implements VanishPlatformService {

    private final MinecraftServerHandle server;

    public FabricVanishPlatformService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<Void> setVanishedState(CellPlayer player, boolean vanished) {
        FabricVanishBridge.vanished(player.uuid(), vanished);
        return PlatformResult.success();
    }

    @Override
    public PlatformResult<Void> setVisible(CellPlayer viewer, CellPlayer target, boolean visible) {
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Visibility changes require the server thread"
            );
        }

        final ServerPlayer viewerNative;
        final ServerPlayer targetNative;
        try {
            viewerNative = MinecraftPlayers.requireOnline(server, viewer);
            targetNative = MinecraftPlayers.requireOnline(server, target);
        } catch (IllegalStateException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    failure.getMessage() == null
                            ? "Viewer or target is offline"
                            : failure.getMessage()
            );
        }

        if (viewerNative.getUUID().equals(targetNative.getUUID())) {
            return PlatformResult.success();
        }

        try {
            if (visible) {
                viewerNative.connection.send(
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(
                                targetNative))
                );
                targetNative.startSeenByPlayer(viewerNative);
            } else {
                viewerNative.connection.send(new ClientboundRemoveEntitiesPacket(targetNative.getId()));
                viewerNative.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(
                        targetNative.getUUID()
                )));
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
