package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MuteService {

    CompletableFuture<AdminResult> mute(
            UUID uuid,
            String name,
            String actor,
            @Nullable Long durationMillis,
            String reason
    );

    CompletableFuture<AdminResult> unmute(
            UUID uuid,
            String name,
            String actor
    );

    boolean muted(UUID uuid);

    Optional<BanRecord> record(UUID uuid);

    CompletableFuture<Integer> purgeExpired();

}
