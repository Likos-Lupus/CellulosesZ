package top.likoslupus.cellulosesz.modules.teleport;

public final class TeleportConfig {

    public Warmup warmup = new Warmup();
    public Requests requests = new Requests();
    public RandomTeleport randomTeleport = new RandomTeleport();

    public void copyFrom(TeleportConfig source) {
        warmup.defaultSeconds = source.warmup.defaultSeconds;
        requests.timeoutSeconds = source.requests.timeoutSeconds;
        requests.maximumBulkTargets = source.requests.maximumBulkTargets;
        randomTeleport.minRadius = source.randomTeleport.minRadius;
        randomTeleport.maxRadius = source.randomTeleport.maxRadius;
        randomTeleport.attempts = source.randomTeleport.attempts;
    }

    public static final class Warmup {

        public int defaultSeconds = 0;

    }

    public static final class Requests {

        public int timeoutSeconds = 120;
        public int maximumBulkTargets = 200;

    }

    public static final class RandomTeleport {

        public int minRadius = 0;
        public int maxRadius = 1000;
        public int attempts = 24;

    }

}
