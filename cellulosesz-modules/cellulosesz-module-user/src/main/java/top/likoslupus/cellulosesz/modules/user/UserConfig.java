package top.likoslupus.cellulosesz.modules.user;

import java.util.ArrayList;
import java.util.List;

public final class UserConfig {

    public int schema = 1;
    public int autosaveIntervalSeconds = 60;
    public boolean saveOnQuit = true;
    public boolean updateNameCacheOnJoin = true;
    public DisplayNameConfig displayName = new DisplayNameConfig();

    public static final class DisplayNameConfig {

        public String nicknamePrefix = "<dark_gray>~<reset>";
        public int minLength = 1;
        public int maxLength = 16;
        public String allowedPattern = "^[\\p{L}\\p{N}_-]+$";
        public List<String> blacklist = new ArrayList<>();
        public String colorPermission = "cellulosesz.playerstate.nick.color";

    }

}
