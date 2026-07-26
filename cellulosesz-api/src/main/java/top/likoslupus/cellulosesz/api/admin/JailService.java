package top.likoslupus.cellulosesz.api.admin;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface JailService {

    CompletableFuture<AdminResult> setJail(
            String name,
            CellLocation location,
            String actor
    );

    CompletableFuture<AdminResult> deleteJail(String name);

    Optional<Jail> jail(String name);

    Collection<Jail> jails();

    CompletableFuture<AdminResult> jailPlayer(
            CellPlayer player,
            String jail,
            String actor,
            @Nullable Long durationMillis,
            String reason
    );

    CompletableFuture<AdminResult> unjail(
            UUID uuid,
            String name,
            String actor
    );

    Optional<JailedPlayer> jailed(UUID uuid);

    Collection<JailedPlayer> jailedPlayers();

    CompletableFuture<Integer> purgeExpired();

}
