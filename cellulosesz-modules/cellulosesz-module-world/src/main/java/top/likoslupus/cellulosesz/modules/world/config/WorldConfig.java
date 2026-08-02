package top.likoslupus.cellulosesz.modules.world.config;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;
import static top.likoslupus.cellulosesz.api.validation.RangeChecks.requireInRange;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;


public final class WorldConfig {

    public int defaultWeatherSeconds = 600;
    public int defaultRemoveRadius = 128;

    public boolean destructiveCommandsEnabled;
    public boolean explosionBlockDamage;
    public int targetDistance = 20;
    public double maximumProjectileSpeed = 4.0D;
    public double defaultProjectileSpeed = 1.5D;
    public int projectileLifetimeTicks = 200;
    public int spawnMobMaximumAmount = 64;
    public int treeTargetDistance = 20;
    public int spawnerMinimumDelayTicks = 1;
    public int spawnerMaximumDelayTicks = 72_000;
    public int spawnerDefaultDelayTicks = 200;
    public double lightningMaximumDamage = 100.0D;

    public int antiochFuseTicks = 80;
    public double antiochExplosionPower = 4.0D;
    public int antiochMaximumEntities = 1;

    public double temporaryMobSpeed = 1.5D;
    public int temporaryMobLifetimeTicks = 100;
    public double temporaryMobExplosionPower = 2.0D;

    public boolean nukeEnabled;
    public int nukeTntPerTarget = 16;
    public double nukeHeight = 20.0D;
    public double nukeSpread = 8.0D;
    public int nukeFuseTicks = 100;
    public double nukeExplosionPower = 4.0D;

    public Backup backup = new Backup();

    public void copyFrom(WorldConfig source) {
        defaultWeatherSeconds = source.defaultWeatherSeconds;
        defaultRemoveRadius = source.defaultRemoveRadius;
        destructiveCommandsEnabled = source.destructiveCommandsEnabled;
        explosionBlockDamage = source.explosionBlockDamage;
        targetDistance = source.targetDistance;
        maximumProjectileSpeed = source.maximumProjectileSpeed;
        defaultProjectileSpeed = source.defaultProjectileSpeed;
        projectileLifetimeTicks = source.projectileLifetimeTicks;
        spawnMobMaximumAmount = source.spawnMobMaximumAmount;
        treeTargetDistance = source.treeTargetDistance;
        spawnerMinimumDelayTicks = source.spawnerMinimumDelayTicks;
        spawnerMaximumDelayTicks = source.spawnerMaximumDelayTicks;
        spawnerDefaultDelayTicks = source.spawnerDefaultDelayTicks;
        lightningMaximumDamage = source.lightningMaximumDamage;
        antiochFuseTicks = source.antiochFuseTicks;
        antiochExplosionPower = source.antiochExplosionPower;
        antiochMaximumEntities = source.antiochMaximumEntities;
        temporaryMobSpeed = source.temporaryMobSpeed;
        temporaryMobLifetimeTicks = source.temporaryMobLifetimeTicks;
        temporaryMobExplosionPower = source.temporaryMobExplosionPower;
        nukeEnabled = source.nukeEnabled;
        nukeTntPerTarget = source.nukeTntPerTarget;
        nukeHeight = source.nukeHeight;
        nukeSpread = source.nukeSpread;
        nukeFuseTicks = source.nukeFuseTicks;
        nukeExplosionPower = source.nukeExplosionPower;
        backup = source.backup.copy();
    }

    public void validate() {
        requirePositive(
                defaultWeatherSeconds,
                "defaultWeatherSeconds"
        );
        requirePositive(
                defaultRemoveRadius,
                "defaultRemoveRadius"
        );
        requireInRange(
                targetDistance,
                1,
                128,
                "targetDistance"
        );
        requireInRange(
                maximumProjectileSpeed,
                0.01D,
                16.0D,
                "maximumProjectileSpeed"
        );
        requireInRange(
                defaultProjectileSpeed,
                0.01D,
                maximumProjectileSpeed,
                "defaultProjectileSpeed"
        );
        requireInRange(
                projectileLifetimeTicks,
                1,
                12_000,
                "projectileLifetimeTicks"
        );
        requireInRange(
                spawnMobMaximumAmount,
                1,
                1_024,
                "spawnMobMaximumAmount"
        );
        requireInRange(
                treeTargetDistance,
                1,
                128,
                "treeTargetDistance"
        );
        requireInRange(
                spawnerMinimumDelayTicks,
                1,
                1_000_000,
                "spawnerMinimumDelayTicks"
        );
        requireInRange(
                spawnerMaximumDelayTicks,
                spawnerMinimumDelayTicks,
                1_000_000,
                "spawnerMaximumDelayTicks"
        );
        requireInRange(
                spawnerDefaultDelayTicks,
                spawnerMinimumDelayTicks,
                spawnerMaximumDelayTicks,
                "spawnerDefaultDelayTicks"
        );
        requireInRange(
                lightningMaximumDamage,
                0.0D,
                10_000.0D,
                "lightningMaximumDamage"
        );
        requireInRange(
                antiochFuseTicks,
                1,
                12_000,
                "antiochFuseTicks"
        );
        requireInRange(
                antiochExplosionPower,
                0.0D,
                32.0D,
                "antiochExplosionPower"
        );
        requireInRange(
                antiochMaximumEntities,
                1,
                16,
                "antiochMaximumEntities"
        );
        requireInRange(
                temporaryMobSpeed,
                0.01D,
                maximumProjectileSpeed,
                "temporaryMobSpeed"
        );
        requireInRange(
                temporaryMobLifetimeTicks,
                1,
                12_000,
                "temporaryMobLifetimeTicks"
        );
        requireInRange(
                temporaryMobExplosionPower,
                0.0D,
                32.0D,
                "temporaryMobExplosionPower"
        );
        requireInRange(
                nukeTntPerTarget,
                1,
                256,
                "nukeTntPerTarget"
        );
        requireInRange(
                nukeHeight,
                0.0D,
                256.0D,
                "nukeHeight"
        );
        requireInRange(
                nukeSpread,
                0.0D,
                128.0D,
                "nukeSpread"
        );
        requireInRange(
                nukeFuseTicks,
                1,
                12_000,
                "nukeFuseTicks"
        );
        requireInRange(
                nukeExplosionPower,
                0.0D,
                32.0D,
                "nukeExplosionPower"
        );
        backup.validate();
    }

    public static final class Backup {

        public boolean enabled = true;
        public String directory = "backups";
        public int retain = 10;

        private Backup copy() {
            var copy = new Backup();
            copy.enabled = enabled;
            copy.directory = directory;
            copy.retain = retain;
            return copy;
        }

        private void validate() {
            directory = requireNonBlank(
                    directory,
                    "backup.directory"
            );
            requireInRange(
                    retain,
                    1,
                    10_000,
                    "backup.retain"
            );
        }

    }

}
