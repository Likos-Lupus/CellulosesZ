package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.playerstate.GameModeKind;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class GameModeSignHandler implements SynchronousSignHandler {

    private final PlayerStatePlatformService playerState;

    public GameModeSignHandler(PlayerStatePlatformService playerState) {
        this.playerState = requireNonNull(playerState, "playerState");
    }

    @Override
    public String id() {
        return "GameMode";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return mode(context).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.gamemode-format");
    }

    private Optional<GameModeKind> mode(SignUseContext context) {
        try {
            return Optional.of(GameModeKind.valueOf(context.line(1).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var mode = mode(context);
        if (mode.isEmpty()) {
            return SignUseResult.failure("service.sign.gamemode-format");
        }

        return playerState.setGameMode(
                context.player(),
                mode.orElseThrow()
        ).successful()
                ?
                SignUseResult.success(
                        "service.sign.gamemode-success",
                        Map.of("mode", mode.orElseThrow().name().toLowerCase(Locale.ROOT))
                )
                : SignUseResult.failure("service.sign.gamemode-failed");
    }

}
