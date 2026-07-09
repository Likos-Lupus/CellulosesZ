package top.likoslupus.cellulosesz.core.config;

import java.util.ArrayList;
import java.util.List;

public final class CoreConfig {

    public int schema = 1;
    public LocaleConfig locale = new LocaleConfig();
    public DebugConfig debug = new DebugConfig();
    public StorageConfig storage = new StorageConfig();
    public CommandsConfig commands = new CommandsConfig();
    public PermissionsConfig permissions = new PermissionsConfig();

    public static final class LocaleConfig {

        public String defaultLocale = "zh_cn";
        public String fallback = "en_us";

    }

    public static final class DebugConfig {

        public boolean enabled = false;
        public boolean verboseModuleLoading = false;

    }

    public static final class StorageConfig {

        public int autosaveIntervalSeconds = 60;
        public boolean prettyPrint = true;

    }

    public static final class CommandsConfig {

        public RootCommandConfig root = new RootCommandConfig();

    }

    public static final class RootCommandConfig {

        public String primary = "cellulosesz";
        public List<String> aliases = new ArrayList<>(List.of("cellz", "cz"));

    }

    public static final class PermissionsConfig {

        public int opFallbackLevel = 2;
        public boolean preferLuckPerms = true;

    }

}
