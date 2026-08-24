package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.common.admin.Expiration;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.BanRecord;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MuteService {

    CompletableFuture<AdminResult> mute(
            UUID uuid,
            String name,
            AdminActor actor,
            Expiration expiration,
            String reason
    );

    CompletableFuture<AdminResult> unmute(
            UUID uuid,
            String name,
            AdminActor actor
    );

    boolean muted(UUID uuid);

    Optional<BanRecord> record(UUID uuid);

    CompletableFuture<Integer> purgeExpired();

}
