package top.likoslupus.cellulosesz.modules.world.config;

public final class WorldConfig {

    public int defaultWeatherSeconds = 600;
    public int defaultRemoveRadius = 128;
    public Backup backup = new Backup();

    public static final class Backup {

        public boolean enabled = false;
        public String command = "";
        public boolean requireConsole = true;

    }

}
