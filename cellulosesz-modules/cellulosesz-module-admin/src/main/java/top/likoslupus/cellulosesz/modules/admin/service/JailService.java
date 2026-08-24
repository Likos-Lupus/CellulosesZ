package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.common.admin.Expiration;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.Jail;
import top.likoslupus.cellulosesz.modules.admin.domain.JailedPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface JailService {

    CompletableFuture<AdminResult> setJail(
            String name,
            CellLocation location,
            AdminActor actor
    );

    CompletableFuture<AdminResult> deleteJail(String name);

    Optional<Jail> jail(String name);

    List<Jail> jails();

    CompletableFuture<AdminResult> jailPlayer(
            CellPlayer player,
            String jail,
            AdminActor actor,
            Expiration expiration,
            String reason
    );

    CompletableFuture<AdminResult> unjail(
            UUID uuid,
            String name,
            AdminActor actor
    );

    CompletableFuture<AdminResult> completePendingRelease(CellPlayer player);

    Optional<JailedPlayer> jailed(UUID uuid);

    List<JailedPlayer> jailedPlayers();

    CompletableFuture<Integer> purgeExpired();

}
