package top.likoslupus.cellulosesz.modules.world.config;

import static java.util.Objects.requireNonNull;

/** Atomically published immutable world command settings. */
public final class WorldRuntimeSettings {

    private volatile Snapshot snapshot;

    public WorldRuntimeSettings(WorldConfig config) {
        snapshot = Snapshot.from(config);
    }

    public void configure(WorldConfig config) {
        snapshot = Snapshot.from(config);
    }

    public int defaultWeatherSeconds() {
        return snapshot.defaultWeatherSeconds();
    }

    public int defaultRemoveRadius() {
        return snapshot.defaultRemoveRadius();
    }

    public boolean destructiveCommandsEnabled() {
        return snapshot.destructiveCommandsEnabled();
    }

    public boolean explosionBlockDamage() {
        return snapshot.explosionBlockDamage();
    }

    public int targetDistance() {
        return snapshot.targetDistance();
    }

    public double maximumProjectileSpeed() {
        return snapshot.maximumProjectileSpeed();
    }

    public double defaultProjectileSpeed() {
        return snapshot.defaultProjectileSpeed();
    }

    public int projectileLifetimeTicks() {
        return snapshot.projectileLifetimeTicks();
    }

    public int spawnMobMaximumAmount() {
        return snapshot.spawnMobMaximumAmount();
    }

    public int treeTargetDistance() {
        return snapshot.treeTargetDistance();
    }

    public int spawnerMinimumDelayTicks() {
        return snapshot.spawnerMinimumDelayTicks();
    }

    public int spawnerMaximumDelayTicks() {
        return snapshot.spawnerMaximumDelayTicks();
    }

    public int spawnerDefaultDelayTicks() {
        return snapshot.spawnerDefaultDelayTicks();
    }

    public double lightningMaximumDamage() {
        return snapshot.lightningMaximumDamage();
    }

    public int antiochFuseTicks() {
        return snapshot.antiochFuseTicks();
    }

    public double antiochExplosionPower() {
        return snapshot.antiochExplosionPower();
    }

    public int antiochMaximumEntities() {
        return snapshot.antiochMaximumEntities();
    }

    public double temporaryMobSpeed() {
        return snapshot.temporaryMobSpeed();
    }

    public int temporaryMobLifetimeTicks() {
        return snapshot.temporaryMobLifetimeTicks();
    }

    public double temporaryMobExplosionPower() {
        return snapshot.temporaryMobExplosionPower();
    }

    public boolean nukeEnabled() {
        return snapshot.nukeEnabled();
    }

    public int nukeTntPerTarget() {
        return snapshot.nukeTntPerTarget();
    }

    public double nukeHeight() {
        return snapshot.nukeHeight();
    }

    public double nukeSpread() {
        return snapshot.nukeSpread();
    }

    public int nukeFuseTicks() {
        return snapshot.nukeFuseTicks();
    }

    public double nukeExplosionPower() {
        return snapshot.nukeExplosionPower();
    }

    private record Snapshot(
            int defaultWeatherSeconds,
            int defaultRemoveRadius,
            boolean destructiveCommandsEnabled,
            boolean explosionBlockDamage,
            int targetDistance,
            double maximumProjectileSpeed,
            double defaultProjectileSpeed,
            int projectileLifetimeTicks,
            int spawnMobMaximumAmount,
            int treeTargetDistance,
            int spawnerMinimumDelayTicks,
            int spawnerMaximumDelayTicks,
            int spawnerDefaultDelayTicks,
            double lightningMaximumDamage,
            int antiochFuseTicks,
            double antiochExplosionPower,
            int antiochMaximumEntities,
            double temporaryMobSpeed,
            int temporaryMobLifetimeTicks,
            double temporaryMobExplosionPower,
            boolean nukeEnabled,
            int nukeTntPerTarget,
            double nukeHeight,
            double nukeSpread,
            int nukeFuseTicks,
            double nukeExplosionPower
    ) {

        private static Snapshot from(WorldConfig source) {
            var config = new WorldConfig();
            config.copyFrom(requireNonNull(source, "config"));
            return new Snapshot(
                    config.defaultWeatherSeconds,
                    config.defaultRemoveRadius,
                    config.destructiveCommandsEnabled,
                    config.explosionBlockDamage,
                    config.targetDistance,
                    config.maximumProjectileSpeed,
                    config.defaultProjectileSpeed,
                    config.projectileLifetimeTicks,
                    config.spawnMobMaximumAmount,
                    config.treeTargetDistance,
                    config.spawnerMinimumDelayTicks,
                    config.spawnerMaximumDelayTicks,
                    config.spawnerDefaultDelayTicks,
                    config.lightningMaximumDamage,
                    config.antiochFuseTicks,
                    config.antiochExplosionPower,
                    config.antiochMaximumEntities,
                    config.temporaryMobSpeed,
                    config.temporaryMobLifetimeTicks,
                    config.temporaryMobExplosionPower,
                    config.nukeEnabled,
                    config.nukeTntPerTarget,
                    config.nukeHeight,
                    config.nukeSpread,
                    config.nukeFuseTicks,
                    config.nukeExplosionPower
            );
        }

    }

}
