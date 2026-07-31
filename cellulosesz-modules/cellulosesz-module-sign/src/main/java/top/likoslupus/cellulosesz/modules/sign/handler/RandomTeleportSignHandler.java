package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class RandomTeleportSignHandler implements CellSignHandler {

    private final PlatformService platform;
    private final RandomTeleportService randomTeleports;
    private final RandomTeleportSettingsService settings;
    private final TeleportService teleports;

    public RandomTeleportSignHandler(
            PlatformService platform,
            RandomTeleportService randomTeleports,
            RandomTeleportSettingsService settings,
            TeleportService teleports
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.randomTeleports = requireNonNull(randomTeleports, "randomTeleports");
        this.settings = requireNonNull(settings, "settings");
        this.teleports = requireNonNull(teleports, "teleports");
    }

    @Override
    public String id() {
        return "RandomTeleport";
    }

    @Override
    public SignUseResult validate(SignUseContext context) {
        return context.line(1).isBlank() || platform.worlds().contains(context.line(1))
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.random-teleport-world");
    }


    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var world = context.line(1).isBlank()
                ? context.location().world
                : context.line(1);

        if (!platform.worlds().contains(world)) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.random-teleport-world"
            ));
        }

        var destination = randomTeleports.randomLocation(
                world,
                settings.settings(world)
        );
        if (!destination.success() || destination.location().isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.random-teleport-failed"
            ));
        }

        return teleports.teleport(
                        context.player(),
                        destination.location().orElseThrow(),
                        TeleportOptions.defaults().withSafe(true).withWarmup(0)
                )
                .thenApply(result -> result.success()
                        ? SignUseResult.success(result.message())
                        : SignUseResult.failure(result.message())
                );
    }

}
