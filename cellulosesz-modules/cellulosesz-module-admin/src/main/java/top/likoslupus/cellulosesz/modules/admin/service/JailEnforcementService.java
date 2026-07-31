package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.admin.JailService;
import top.likoslupus.cellulosesz.api.admin.JailState;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class JailEnforcementService {

    private final JailService jails;
    private final PlayerLocationPlatformService locations;
    private final TeleportService teleports;
    private final AdminConfig config;

    public JailEnforcementService(
            JailService jails,
            PlayerLocationPlatformService locations,
            TeleportService teleports,
            AdminConfig config
    ) {
        this.jails = requireNonNull(jails, "jails");
        this.locations = requireNonNull(locations, "locations");
        this.teleports = requireNonNull(teleports, "teleports");
        this.config = requireNonNull(config, "config");
    }

    public CompletableFuture<Boolean> enforce(CellPlayer player) {
        var jail = activeJail(player);
        if (jail.isEmpty()
                || inside(jail.orElseThrow().location(), locations.currentLocation(player))
        ) {
            return CompletableFuture.completedFuture(true);
        }
        return teleports
                .teleport(
                        player,
                        jail.orElseThrow().location(),
                        TeleportOptions.defaults().withoutBackMemory()
                )
                .thenApply(result -> result.success());
    }

    public Optional<Jail> activeJail(CellPlayer player) {
        return jails.jailed(player.uuid())
                .filter(value -> value.state() == JailState.ACTIVE)
                .flatMap(value -> jails.jail(value.jail()));
    }

    public boolean inside(CellLocation jail, CellLocation actual) {
        if (!jail.world.equals(actual.world)) return false;

        var dx = jail.x - actual.x;
        var dy = jail.y - actual.y;
        var dz = jail.z - actual.z;
        var radius = config.jailConfinementRadius;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

}
