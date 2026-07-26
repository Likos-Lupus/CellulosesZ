package top.likoslupus.cellulosesz.modules.playerstate.config;

public final class PlayerStateConfig {

    public int nearRadius = 200;
    public int maximumNearRadius = 1000;
    public long autoAfkSeconds = 300L;
    public long afkKickSeconds;
    public long activityCheckSeconds = 5L;
    public boolean persistAfk = true;
    public boolean persistNick = true;
    public boolean persistFlyGod = true;
    public boolean persistVanish = true;
    public boolean persistPersonalTimeWeather = true;

}
