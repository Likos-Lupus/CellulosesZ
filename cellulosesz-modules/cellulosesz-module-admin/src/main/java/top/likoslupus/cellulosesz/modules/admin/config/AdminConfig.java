package top.likoslupus.cellulosesz.modules.admin.config;

import java.util.LinkedHashSet;
import java.util.Set;

import static top.likoslupus.cellulosesz.api.validation.Checks.*;

import static java.util.Objects.requireNonNull;

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
            "msg",
            "tell",
            "w",
            "r",
            "reply",
            "mail",
            "me",
            "helpop"
    ));
    public long defaultJailSeconds = 0L;
    public boolean teleportOnJailRelease = true;
    public boolean tempBanKickOnlinePlayers = true;
    public int jailedPlayerCheckSeconds = 5;
    public double jailConfinementRadius = 3.0D;
    public int maximumBurnSeconds = 86_400;
    public int sudoMaximumCommandLength = 512;
    public int maximumReasonLength = 512;
    public String defaultReason = "No reason specified";

    public AdminConfig validatedCopy() {
        var copy = new AdminConfig();
        copy.copyFrom(this);
        copy.validate();
        return copy;
    }

    public void copyFrom(AdminConfig source) {
        requireNonNull(source, "source").validate();
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
        maximumReasonLength = source.maximumReasonLength;
        defaultReason = source.defaultReason;
    }

    public void validate() {
        requireNonNegative(defaultMuteSeconds, "defaultMuteSeconds");
        requirePositiveOrNegativeOne(maximumMuteSeconds, "maximumMuteSeconds");
        requirePositiveOrNegativeOne(maximumMuteSeconds, "maximumMuteSeconds");
        requirePositiveOrNegativeOne(maximumPunishmentSeconds, "maximumPunishmentSeconds");
        requireNonNegative(defaultJailSeconds, "defaultJailSeconds");
        requirePositive(jailedPlayerCheckSeconds, "jailedPlayerCheckSeconds");
        requireFinite(jailConfinementRadius, "jailConfinementRadius");
        requireNonNegative(jailConfinementRadius, "jailConfinementRadius");
        requireInRange(maximumBurnSeconds, 0, 31_536_000, "maximumBurnSeconds");
        requireInRange(sudoMaximumCommandLength, 1, 4096, "sudoMaximumCommandLeng;th");
        requireInRange(maximumReasonLength, 1, 4096, "maximumReasonLength");
        defaultReason = requireNonNull(defaultReason, "defaultReason").trim();
        if (defaultReason.isBlank() || defaultReason.length() > maximumReasonLength) {
            throw new IllegalStateException("defaultReason is invalid");
        }
        muteCommands.stream()
                .filter(command -> command.isBlank()
                        || command.chars().anyMatch(Character::isISOControl)
                )
                .forEach(_ -> {
                    throw new IllegalArgumentException("muteCommands contains invalid root");
                });
    }

}
