package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerConnectionService;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerConnectionService implements PlayerConnectionService {

    private final CellulosesZLogger logger;

    public MinecraftPlayerConnectionService(CellulosesZLogger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public PlatformResult<Void> disconnect(CellPlayer player, RichText reason) {
        try {
            MinecraftPlayers.requireOnline(player).connection.disconnect(MinecraftTextAdapter.toComponent(reason, logger));
            return PlatformResult.success();
        } catch (IllegalStateException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    failure.getMessage() == null
                            ? failure.getClass().getSimpleName()
                            : failure.getMessage()
            );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

}
