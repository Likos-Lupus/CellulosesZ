package top.likoslupus.cellulosesz.api.teleport;

/**
 * Mutable command-scoped teleport options. Instances are not shared between requests.
 */
public final class TeleportOptions {

    private boolean safe = true;
    private boolean rememberBack = true;
    private boolean allowCrossWorld = true;
    private boolean keepVehicle;
    private int warmupSeconds;

    public boolean safe() {
        return safe;
    }

    public TeleportOptions safe(boolean value) {
        safe = value;
        return this;
    }

    public boolean rememberBack() {
        return rememberBack;
    }

    public TeleportOptions rememberBack(boolean value) {
        rememberBack = value;
        return this;
    }

    public boolean allowCrossWorld() {
        return allowCrossWorld;
    }

    public TeleportOptions allowCrossWorld(boolean value) {
        allowCrossWorld = value;
        return this;
    }

    public boolean keepVehicle() {
        return keepVehicle;
    }

    public TeleportOptions keepVehicle(boolean value) {
        keepVehicle = value;
        return this;
    }

    public int warmupSeconds() {
        return warmupSeconds;
    }

    public TeleportOptions warmupSeconds(int value) {
        if (value < 0) throw new IllegalArgumentException("warmupSeconds must not be negative");
        warmupSeconds = value;
        return this;
    }

}
