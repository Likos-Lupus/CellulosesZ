package top.likoslupus.cellulosesz.modules.admin.config;

import java.util.LinkedHashSet;
import java.util.Set;

public final class AdminConfig {

    public long defaultMuteSeconds = 3600L;
    /**
     * Negative disables the cap.
     */
    public long maximumMuteSeconds = -1L;
    public Set<String> muteCommands = new LinkedHashSet<>(Set.of(
            "msg", "tell", "w", "r", "reply", "mail", "me", "helpop"
    ));
    public long defaultJailSeconds = 0L;
    public boolean teleportOnJailRelease = true;
    public boolean tempBanKickOnlinePlayers = true;
    public int jailedPlayerCheckSeconds = 5;
    public double jailConfinementRadius = 3.0D;

}
