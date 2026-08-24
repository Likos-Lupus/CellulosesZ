package top.likoslupus.cellulosesz.modules.messaging.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.modules.messaging.domain.MessageResult;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PrivateMessageService {

    CompletableFuture<MessageResult> send(
            CellPlayer sender,
            CellPlayer target,
            String message
    );

    CompletableFuture<Optional<UUID>> lastReplyTarget(UUID uuid);

    CompletableFuture<Void> setLastReplyTarget(
            UUID uuid,
            UUID target
    );

    CompletableFuture<Boolean> ignored(
            UUID viewer,
            UUID target
    );

    CompletableFuture<Void> setIgnored(
            UUID viewer,
            UUID target,
            boolean ignored
    );

    CompletableFuture<Boolean> socialSpy(UUID uuid);

    CompletableFuture<Void> setSocialSpy(
            UUID uuid,
            boolean enabled
    );

    CompletableFuture<Void> broadcastSpy(
            String sender,
            String target,
            String message,
            Collection<UUID> excluded
    );

}
