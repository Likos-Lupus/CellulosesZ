package top.likoslupus.cellulosesz.api.user;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface UserService {

    CompletableFuture<CellUser> load(UUID uuid);

    CompletableFuture<CellUser> loadFromPlayer(CellPlayer player);

    Optional<CellUser> cached(UUID uuid);

    default Collection<CellUser> cachedUsers() {
        return List.of();
    }

    Optional<UUID> findUuidByName(String name);

    Collection<UUID> knownUuids();

    default CompletableFuture<Void> updateVoid(
            UUID uuid,
            UnaryOperator<CellUser> mutation
    ) {
        return update(
                uuid,
                user -> UserUpdate.replacing(mutation.apply(user))
        ).thenAccept(_ -> {
        });
    }

    <T> CompletableFuture<T> update(
            UUID uuid,
            Function<CellUser, UserUpdate<T>> mutation
    );

    CompletableFuture<Void> save(UUID uuid);

    CompletableFuture<Void> saveAll();

}
