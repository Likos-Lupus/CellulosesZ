package top.likoslupus.cellulosesz.modules.sign.handler;

import top.likoslupus.cellulosesz.api.sign.CellSignHandler;
import top.likoslupus.cellulosesz.api.sign.SignUseContext;
import top.likoslupus.cellulosesz.api.sign.SignUseResult;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportService;
import top.likoslupus.cellulosesz.api.teleport.RandomTeleportSettingsService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class RandomTeleportSignHandler implements CellSignHandler {

    private final WorldDirectory worlds;
    private final RandomTeleportService randomTeleports;
    private final RandomTeleportSettingsService settings;
    private final TeleportService teleports;

    public RandomTeleportSignHandler(
            WorldDirectory worlds,
            RandomTeleportService randomTeleports,
            RandomTeleportSettingsService settings,
            TeleportService teleports
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
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
        return context.line(1).isBlank()
                || worlds.resolveLoadedWorld(context.line(1)).isPresent()
                ? SignUseResult.success("service.sign.valid")
                : SignUseResult.failure("service.sign.random-teleport-world");
    }

    @Override
    public CompletableFuture<SignUseResult> use(SignUseContext context) {
        var requestedWorld = context.line(1).isBlank()
                ? context.location().world()
                : context.line(1);
        var world = worlds.resolveLoadedWorld(requestedWorld);

        if (world.isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.random-teleport-world"
            ));
        }

        var destination = randomTeleports.randomLocation(
                world.orElseThrow(),
                settings.settings(world.orElseThrow())
        );

        if (!destination.success() || destination.location().isEmpty()) {
            return CompletableFuture.completedFuture(SignUseResult.failure(
                    "service.sign.random-teleport-failed"
            ));
        }

        return teleports
                .teleport(
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
