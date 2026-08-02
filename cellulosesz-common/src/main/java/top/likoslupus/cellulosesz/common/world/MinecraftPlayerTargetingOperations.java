package top.likoslupus.cellulosesz.common.world;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.world.PlayerTargetingService;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerTargetingOperations implements PlayerTargetingService {

    private final MinecraftServerHandle server;

    public MinecraftPlayerTargetingOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<CellLocation> targetLocation(
            CellPlayer player,
            int maximumDistance
    ) {
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        var nativePlayer = MinecraftPlayers.requireOnline(player);
        var hit = nativePlayer.pick(maximumDistance, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || hit.getType() != HitResult.Type.BLOCK
        ) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    "No block is targeted"
            );
        }

        var location = blockHit.getLocation();
        return PlatformResult.success(new CellLocation(
                nativePlayer.level().dimension().identifier().toString(),
                location.x, location.y, location.z,
                nativePlayer.getYRot(), nativePlayer.getXRot()
        ));
    }

}
