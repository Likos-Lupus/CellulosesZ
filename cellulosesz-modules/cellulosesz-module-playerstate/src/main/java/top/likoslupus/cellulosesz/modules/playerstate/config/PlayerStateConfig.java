package top.likoslupus.cellulosesz.modules.playerstate.config;

public final class PlayerStateConfig {

    public int nearRadius = 200;
    public int maximumNearRadius = 1000;
    public int maximumNearResults = 50;
    public double minimumSpeed = 0.0001D;
    public double maximumSpeed = 10.0D;
    public int maximumPingLength = 256;
    public int maximumRealNameResults = 20;
    public long autoAfkSeconds = 300L;
    public long afkKickSeconds;
    public long activityCheckSeconds = 5L;
    public boolean persistAfk = true;
    public boolean persistNick = true;
    public boolean persistFlyGod = true;
    public boolean persistVanish = true;
    public boolean persistPersonalTimeWeather = true;

    public void copyFrom(PlayerStateConfig source) {
        nearRadius = source.nearRadius;
        maximumNearRadius = source.maximumNearRadius;
        maximumNearResults = source.maximumNearResults;
        minimumSpeed = source.minimumSpeed;
        maximumSpeed = source.maximumSpeed;
        maximumPingLength = source.maximumPingLength;
        maximumRealNameResults = source.maximumRealNameResults;
        autoAfkSeconds = source.autoAfkSeconds;
        afkKickSeconds = source.afkKickSeconds;
        activityCheckSeconds = source.activityCheckSeconds;
        persistAfk = source.persistAfk;
        persistNick = source.persistNick;
        persistFlyGod = source.persistFlyGod;
        persistVanish = source.persistVanish;
        persistPersonalTimeWeather = source.persistPersonalTimeWeather;
    }

}
