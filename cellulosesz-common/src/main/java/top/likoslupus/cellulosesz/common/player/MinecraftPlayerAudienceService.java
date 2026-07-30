package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.text.MinecraftTextAdapter;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerAudienceService implements PlayerAudienceService {

    private final ServiceRegistry services;
    private final CellulosesZLogger logger;

    public MinecraftPlayerAudienceService(
            ServiceRegistry services,
            CellulosesZLogger logger
    ) {
        this.services = requireNonNull(services, "services");
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public String locale(CellPlayer player) {
        return services.require(LocaleResolver.class).locale(player);
    }

    @Override
    public PlatformResult<Void> send(CellPlayer player, RichText message) {
        try {
            MinecraftPlayers.requireOnline(player)
                    .sendSystemMessage(MinecraftTextAdapter.toComponent(message, logger));
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
