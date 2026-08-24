package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.BanRecord;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TempBanService {

    CompletableFuture<AdminResult> tempBan(
            UUID uuid,
            String name,
            AdminActor actor,
            Duration duration,
            String reason
    );

    CompletableFuture<AdminResult> tempBanIp(
            InetAddress address,
            AdminActor actor,
            Duration duration,
            String reason
    );

    CompletableFuture<AdminResult> unban(
            UUID uuid,
            String name,
            AdminActor actor
    );

    CompletableFuture<AdminResult> unbanIp(InetAddress address, AdminActor actor);

    Optional<BanRecord> active(UUID uuid, String name);

    Optional<BanRecord> activeIp(InetAddress address);

    CompletableFuture<Integer> purgeExpired();

}
