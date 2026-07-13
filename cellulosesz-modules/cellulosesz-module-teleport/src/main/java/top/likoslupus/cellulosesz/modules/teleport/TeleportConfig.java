package top.likoslupus.cellulosesz.modules.teleport;

public final class TeleportConfig {

    public Warmup warmup = new Warmup();
    public Requests requests = new Requests();
    public RandomTeleport randomTeleport = new RandomTeleport();

    public static final class Warmup {

        public int defaultSeconds = 0;

    }

    public static final class Requests {

        public int timeoutSeconds = 120;

    }

    public static final class RandomTeleport {

        public int minRadius = 0;
        public int maxRadius = 1000;
        public int attempts = 24;

    }

}
