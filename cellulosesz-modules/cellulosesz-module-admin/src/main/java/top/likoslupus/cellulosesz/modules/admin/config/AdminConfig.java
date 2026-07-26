package top.likoslupus.cellulosesz.modules.admin.config;

import java.util.LinkedHashSet;
import java.util.Set;

public final class AdminConfig {

    public long defaultMuteSeconds = 3600L;
    /**
     * Negative disables the cap.
     */
    public long maximumMuteSeconds = -1L;
    /**
     * Maximum temporary ban, temporary IP ban, or jail duration. Negative disables the cap.
     */
    public long maximumPunishmentSeconds = 2_592_000L;
    public Set<String> muteCommands = new LinkedHashSet<>(Set.of(
            "msg", "tell", "w", "r", "reply", "mail", "me", "helpop"
    ));
    public long defaultJailSeconds = 0L;
    public boolean teleportOnJailRelease = true;
    public boolean tempBanKickOnlinePlayers = true;
    public int jailedPlayerCheckSeconds = 5;
    public double jailConfinementRadius = 3.0D;

    public void copyFrom(AdminConfig source) {
        defaultMuteSeconds = source.defaultMuteSeconds;
        maximumMuteSeconds = source.maximumMuteSeconds;
        maximumPunishmentSeconds = source.maximumPunishmentSeconds;
        muteCommands = new LinkedHashSet<>(source.muteCommands);
        defaultJailSeconds = source.defaultJailSeconds;
        teleportOnJailRelease = source.teleportOnJailRelease;
        tempBanKickOnlinePlayers = source.tempBanKickOnlinePlayers;
        jailedPlayerCheckSeconds = source.jailedPlayerCheckSeconds;
        jailConfinementRadius = source.jailConfinementRadius;
    }

}
