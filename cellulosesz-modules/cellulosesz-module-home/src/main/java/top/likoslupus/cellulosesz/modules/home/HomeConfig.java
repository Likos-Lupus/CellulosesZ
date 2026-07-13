package top.likoslupus.cellulosesz.modules.home;

public final class HomeConfig {

    public Limits limits = new Limits();
    public Teleport teleport = new Teleport();
    public Naming naming = new Naming();

    public static final class Limits {

        public int defaultMaxHomes = 3;
        public String permissionOptionKey = "cellulosesz.home.max";

    }

    public static final class Teleport {

        public int warmupSeconds = 3;
        public int cooldownSeconds = 5;
        public boolean safe = true;

    }

    public static final class Naming {

        public int minLength = 1;
        public int maxLength = 32;
        public String pattern = "^[a-zA-Z0-9_-]+$";

    }

}
