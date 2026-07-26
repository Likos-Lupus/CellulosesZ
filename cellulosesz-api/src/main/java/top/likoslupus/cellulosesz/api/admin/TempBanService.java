package top.likoslupus.cellulosesz.api.admin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TempBanService {

    CompletableFuture<AdminResult> tempBan(
            String target,
            String actor,
            long durationMillis,
            String reason
    );

    CompletableFuture<AdminResult> tempBanIp(
            String target,
            String actor,
            long durationMillis,
            String reason
    );

    CompletableFuture<AdminResult> unban(
            UUID uuid,
            String name,
            String actor
    );

    CompletableFuture<AdminResult> unbanIp(String address, String actor);

    Optional<BanRecord> active(UUID uuid, String name);

    Optional<BanRecord> activeIp(String address);

    CompletableFuture<Integer> purgeExpired();

}
