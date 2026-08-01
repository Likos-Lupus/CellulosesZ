package top.likoslupus.cellulosesz.fabric;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.world.PlayerTargetingService;

import static java.util.Objects.requireNonNull;

public final class FabricPlayerTargetingOperations implements PlayerTargetingService {

    private final FabricServerAccess access;

    public FabricPlayerTargetingOperations(FabricServerAccess access) {
        this.access = requireNonNull(access, "access");
    }

    @Override
    public PlatformResult<CellLocation> targetLocation(
            CellPlayer player,
            int maximumDistance
    ) {
        if (!access.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        var nativePlayer = access.player(player);
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
