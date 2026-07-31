package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

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
