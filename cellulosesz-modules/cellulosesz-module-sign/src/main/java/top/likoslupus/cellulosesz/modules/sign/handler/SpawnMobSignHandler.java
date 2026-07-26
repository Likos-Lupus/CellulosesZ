package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;

import java.util.Map;
import java.util.Objects;

public final class SpawnMobSignHandler implements SynchronousSignHandler {

    private final PlatformService platform;

    public SpawnMobSignHandler(PlatformService platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public String id() {
        return "SpawnMob";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        if (!platform.validEntityType(context.line(1))) {
            return SignUseResult.failure("service.sign.spawnmob-format");
        }
        if (!context.line(2).isBlank()
                && SignHandlerSupport.count(context.line(2), 1, 64).isEmpty()) {
            return SignUseResult.failure("service.sign.spawnmob-format");
        }
        return context.line(3).isBlank()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.spawnmob-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var count = SignHandlerSupport.count(context.line(2), 1, 64).orElse(0);
        if (count == 0) return SignUseResult.failure("service.sign.spawnmob-format");
        var spawned = platform.spawnMob(context.player(), context.line(1), count);
        return spawned == count
                ? SignUseResult.success("service.sign.spawnmob-success", Map.of(
                "count", count, "entity", context.line(1)))
                : SignUseResult.failure("service.sign.spawnmob-failed", Map.of(
                        "spawned", spawned, "count", count));
    }

}
