package top.likoslupus.cellulosesz.modules.permission.config;

public final class PermissionConfig {

    public Provider provider = new Provider();
    public Cache cache = new Cache();

    public static final class Provider {

        public boolean preferLuckPerms = true;
        public boolean opFallback = true;
        public int opLevel = 2;

    }

    public static final class Cache {

        public boolean enabled = true;
        public int expireSeconds = 5;

    }

}
