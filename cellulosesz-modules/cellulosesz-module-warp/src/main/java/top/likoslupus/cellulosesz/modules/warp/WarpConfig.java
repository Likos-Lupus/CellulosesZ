package top.likoslupus.cellulosesz.modules.warp;

public final class WarpConfig {

    public Teleport teleport = new Teleport();
    public ListConfig list = new ListConfig();
    public Naming naming = new Naming();
    public boolean perWarpPermission;

    public static final class Teleport {

        public int warmupSeconds = 3;
        public int cooldownSeconds = 5;
        public boolean safe = true;

    }

    public static final class ListConfig {

        public int pageSize = 10;
        public boolean hideNoPermission = true;

    }

    public static final class Naming {

        public int maxLength = 32;
        public String pattern = "^[a-zA-Z0-9_-]+$";

    }

}
