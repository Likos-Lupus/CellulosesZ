package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.api.admin.AdminActor;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.Jail;
import top.likoslupus.cellulosesz.api.admin.JailedPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface JailCommandService {

    CompletableFuture<AdminResult> set(CellPlayer actor, String name);

    CompletableFuture<AdminResult> delete(String name);

    List<Jail> jails();

    List<JailedPlayer> jailedPlayers();

    CompletableFuture<AdminResult> jail(
            String player,
            String jail,
            AdminActor actor,
            Optional<Duration> duration,
            String reason
    );

    CompletableFuture<AdminResult> unjail(String player, AdminActor actor);

}
