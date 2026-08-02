package top.likoslupus.cellulosesz.fabric;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.DisplayNamePlatformService;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class FabricDisplayNameOperations implements DisplayNamePlatformService {

    private final MinecraftServerHandle server;
    private final CellulosesZLogger logger;

    public FabricDisplayNameOperations(
            MinecraftServerHandle server,
            CellulosesZLogger logger
    ) {
        this.server = requireNonNull(server, "server");
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public PlatformResult<Void> setDisplayName(
            CellPlayer player,
            RichText displayName
    ) {
        requireNonNull(displayName, "displayName");
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            FabricDisplayNameBridge.displayName(
                    player.uuid(),
                    MinecraftTextAdapter.toComponent(displayName, logger)
            );

            return refresh(nativePlayer);
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    @Override
    public PlatformResult<Void> refreshPlayerInfo(CellPlayer player) {
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return refresh(MinecraftPlayers.requireOnline(server, player));
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private PlatformResult<Void> refresh(net.minecraft.server.level.ServerPlayer player) {
        var packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
        server.requireRunning().getPlayerList().getPlayers().stream()
                .filter(viewer -> !FabricVanishBridge.hiddenFrom(viewer, player))
                .forEach(viewer -> viewer.connection.send(packet));
        return PlatformResult.success();
    }

}
