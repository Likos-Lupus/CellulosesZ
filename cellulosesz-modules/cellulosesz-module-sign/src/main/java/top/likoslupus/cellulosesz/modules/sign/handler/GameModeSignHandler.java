package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GameModeSignHandler implements SynchronousSignHandler {

    private static final Set<String> MODES = Set.of("survival", "creative", "adventure", "spectator");
    private final PlatformService platform;

    public GameModeSignHandler(PlatformService platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public String id() {
        return "GameMode";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return MODES.contains(context.line(1).toLowerCase(Locale.ROOT))
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.gamemode-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        return platform.setGameMode(context.player(), context.line(1))
                ? SignUseResult.success("service.sign.gamemode-success", Map.of("mode", context.line(1)))
                : SignUseResult.failure("service.sign.gamemode-failed");
    }

}
