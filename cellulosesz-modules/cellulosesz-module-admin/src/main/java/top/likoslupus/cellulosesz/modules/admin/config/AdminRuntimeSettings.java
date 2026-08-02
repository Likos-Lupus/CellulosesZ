package top.likoslupus.cellulosesz.modules.admin.config;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Atomically published, immutable administration settings used by long-lived services.
 */
public final class AdminRuntimeSettings {

    private volatile Snapshot snapshot;

    public AdminRuntimeSettings(AdminConfig config) {
        snapshot = Snapshot.from(config);
    }

    public void configure(AdminConfig config) {
        snapshot = Snapshot.from(config);
    }

    public long defaultMuteSeconds() {
        return snapshot.defaultMuteSeconds();
    }

    public long maximumMuteSeconds() {
        return snapshot.maximumMuteSeconds();
    }

    public long maximumPunishmentSeconds() {
        return snapshot.maximumPunishmentSeconds();
    }

    public Set<String> muteCommands() {
        return snapshot.muteCommands();
    }

    public long defaultJailSeconds() {
        return snapshot.defaultJailSeconds();
    }

    public boolean teleportOnJailRelease() {
        return snapshot.teleportOnJailRelease();
    }

    public boolean tempBanKickOnlinePlayers() {
        return snapshot.tempBanKickOnlinePlayers();
    }

    public int jailedPlayerCheckSeconds() {
        return snapshot.jailedPlayerCheckSeconds();
    }

    public double jailConfinementRadius() {
        return snapshot.jailConfinementRadius();
    }

    public int maximumBurnSeconds() {
        return snapshot.maximumBurnSeconds();
    }

    public int sudoMaximumCommandLength() {
        return snapshot.sudoMaximumCommandLength();
    }

    public int maximumReasonLength() {
        return snapshot.maximumReasonLength();
    }

    public String defaultReason() {
        return snapshot.defaultReason();
    }

    private record Snapshot(
            long defaultMuteSeconds,
            long maximumMuteSeconds,
            long maximumPunishmentSeconds,
            Set<String> muteCommands,
            long defaultJailSeconds,
            boolean teleportOnJailRelease,
            boolean tempBanKickOnlinePlayers,
            int jailedPlayerCheckSeconds,
            double jailConfinementRadius,
            int maximumBurnSeconds,
            int sudoMaximumCommandLength,
            int maximumReasonLength,
            String defaultReason
    ) {

        private static Snapshot from(AdminConfig source) {
            var config = requireNonNull(source, "config").validatedCopy();
            return new Snapshot(
                    config.defaultMuteSeconds,
                    config.maximumMuteSeconds,
                    config.maximumPunishmentSeconds,
                    Set.copyOf(config.muteCommands),
                    config.defaultJailSeconds,
                    config.teleportOnJailRelease,
                    config.tempBanKickOnlinePlayers,
                    config.jailedPlayerCheckSeconds,
                    config.jailConfinementRadius,
                    config.maximumBurnSeconds,
                    config.sudoMaximumCommandLength,
                    config.maximumReasonLength,
                    config.defaultReason
            );
        }

    }

}
