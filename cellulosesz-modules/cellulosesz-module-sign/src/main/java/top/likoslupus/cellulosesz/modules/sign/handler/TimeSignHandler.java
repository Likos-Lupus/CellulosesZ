package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseContext;
import top.likoslupus.cellulosesz.modules.sign.domain.SignUseResult;

import static java.util.Objects.requireNonNull;

public final class TimeSignHandler implements SynchronousSignHandler {

    private final WorldDirectory worldDirectory;
    private final WorldService worlds;

    public TimeSignHandler(
            WorldDirectory worldDirectory,
            WorldService worlds
    ) {
        this.worldDirectory = requireNonNull(worldDirectory, "worldDirectory");
        this.worlds = requireNonNull(worlds, "worlds");
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

        if (!context.line(2).isBlank()
                && worldDirectory.resolveLoadedWorld(context.line(2)).isEmpty()
        ) {
            return SignUseResult.failure("service.sign.time-world");
        }

        return context.line(3).isBlank()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.time-format");
    }

    @Override
    public SignUseResult useSynchronously(SignUseContext context) {
        var time = SignHandlerSupport.parseTime(context.line(1));
        if (time.isEmpty()) {
            return SignUseResult.failure("service.sign.time-format");
        }

        var requestedWorld = context.line(2).isBlank()
                ? context.location().world()
                : context.line(2);
        var world = worldDirectory.resolveLoadedWorld(requestedWorld);

        if (world == null) {
            return SignUseResult.failure("service.sign.time-world");
        }

        return SignHandlerSupport.outcome(worlds.setTime(
                world,
                time.orElseThrow()
        ));
    }

}
