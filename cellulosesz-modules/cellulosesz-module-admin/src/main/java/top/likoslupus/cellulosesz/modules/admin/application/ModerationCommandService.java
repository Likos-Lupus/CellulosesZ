package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ModerationCommandService {

    CompletableFuture<AdminResult> kick(
            String player,
            AdminActor actor,
            String reason
    );

    CompletableFuture<AdminResult> kickAll(AdminActor actor, String reason);

    CompletableFuture<AdminResult> mute(
            String player,
            AdminActor actor,
            Optional<Duration> duration,
            String reason
    );

    CompletableFuture<AdminResult> unmute(String player, AdminActor actor);

}
