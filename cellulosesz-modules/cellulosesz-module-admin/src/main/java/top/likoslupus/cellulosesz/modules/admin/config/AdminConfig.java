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
    public int maximumBurnSeconds = 86_400;
    public int sudoMaximumCommandLength = 512;

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
        maximumBurnSeconds = source.maximumBurnSeconds;
        sudoMaximumCommandLength = source.sudoMaximumCommandLength;
    }

    public void validate() {
        if (maximumBurnSeconds < 0 || maximumBurnSeconds > 31_536_000) {
            throw new IllegalArgumentException("maximumBurnSeconds must be between 0 and 31536000");
        }
        if (sudoMaximumCommandLength < 1 || sudoMaximumCommandLength > 4096) {
            throw new IllegalArgumentException("sudoMaximumCommandLength must be between 1 and 4096");
        }
    }

}
