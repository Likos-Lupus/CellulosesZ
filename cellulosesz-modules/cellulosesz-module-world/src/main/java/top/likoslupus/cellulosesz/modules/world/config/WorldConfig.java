package top.likoslupus.cellulosesz.modules.world.config;

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
        positive(defaultWeatherSeconds, "defaultWeatherSeconds");
        positive(defaultRemoveRadius, "defaultRemoveRadius");
        range(targetDistance, 1, 128, "targetDistance");
        finiteRange(defaultProjectileSpeed, 0.01D, maximumProjectileSpeed, "defaultProjectileSpeed");
        finiteRange(maximumProjectileSpeed, 0.01D, 16.0D, "maximumProjectileSpeed");
        range(projectileLifetimeTicks, 1, 12_000, "projectileLifetimeTicks");
        range(spawnMobMaximumAmount, 1, 1024, "spawnMobMaximumAmount");
        range(treeTargetDistance, 1, 128, "treeTargetDistance");
        range(spawnerMinimumDelayTicks, 1, 1_000_000, "spawnerMinimumDelayTicks");
        range(spawnerMaximumDelayTicks, spawnerMinimumDelayTicks, 1_000_000, "spawnerMaximumDelayTicks");
        range(spawnerDefaultDelayTicks, spawnerMinimumDelayTicks, spawnerMaximumDelayTicks, "spawnerDefaultDelayTicks");
        finiteRange(lightningMaximumDamage, 0.0D, 10_000.0D, "lightningMaximumDamage");
        range(antiochFuseTicks, 1, 12_000, "antiochFuseTicks");
        finiteRange(antiochExplosionPower, 0.0D, 32.0D, "antiochExplosionPower");
        range(antiochMaximumEntities, 1, 16, "antiochMaximumEntities");
        finiteRange(temporaryMobSpeed, 0.01D, maximumProjectileSpeed, "temporaryMobSpeed");
        range(temporaryMobLifetimeTicks, 1, 12_000, "temporaryMobLifetimeTicks");
        finiteRange(temporaryMobExplosionPower, 0.0D, 32.0D, "temporaryMobExplosionPower");
        range(nukeTntPerTarget, 1, 256, "nukeTntPerTarget");
        finiteRange(nukeHeight, 0.0D, 256.0D, "nukeHeight");
        finiteRange(nukeSpread, 0.0D, 128.0D, "nukeSpread");
        range(nukeFuseTicks, 1, 12_000, "nukeFuseTicks");
        finiteRange(nukeExplosionPower, 0.0D, 32.0D, "nukeExplosionPower");
        if (backup == null) throw new IllegalArgumentException("backup must not be null");
        backup.validate();
    }

    private static void positive(int value, String name) {
        if (value < 1) throw new IllegalArgumentException(name + " must be positive");
    }

    private static void range(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
    }

    private static void finiteRange(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be finite and between " + minimum + " and " + maximum);
        }
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
            if (directory == null || directory.isBlank())
                throw new IllegalArgumentException("backup.directory must not be blank");
            if (retain < 1 || retain > 10_000)
                throw new IllegalArgumentException("backup.retain must be between 1 and 10000");
        }

    }

}
