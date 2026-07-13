package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface MuteService {

    AdminResult mute(
            UUID uuid,
            String name,
            String actor,
            @Nullable Long durationMillis,
            String reason
    );

    AdminResult unmute(
            UUID uuid,
            String name,
            String actor
    );

    boolean muted(UUID uuid);

    Optional<BanRecord> record(UUID uuid);

    void purgeExpired();

}
