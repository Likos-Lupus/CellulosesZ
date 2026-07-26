package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.sign.SynchronousSignHandler;
import top.likoslupus.cellulosesz.api.world.WorldService;

import java.util.Objects;

public final class TimeSignHandler implements SynchronousSignHandler {

    private final PlatformService platform;
    private final WorldService worlds;

    public TimeSignHandler(PlatformService platform, WorldService worlds) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    @Override
    public String id() {
        return "Time";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        if (SignHandlerSupport.parseTime(context.line(1)).isEmpty()) {
            return SignUseResult.failure("service.sign.time-format");
        }
        if (!context.line(2).isBlank() && !platform.worlds().contains(context.line(2))) {
            return SignUseResult.failure("service.sign.time-world");
        }
        return context.line(3).isBlank()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.time-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var time = SignHandlerSupport.parseTime(context.line(1));
        if (time.isEmpty()) return SignUseResult.failure("service.sign.time-format");
        var world = context.line(2).isBlank() ? context.location().world : context.line(2);
        return SignHandlerSupport.admin(worlds.setTime(world, time.orElseThrow()));
    }

}
