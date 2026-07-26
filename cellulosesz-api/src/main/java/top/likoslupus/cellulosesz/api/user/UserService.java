package top.likoslupus.cellulosesz.api.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface UserService {

    CompletableFuture<CellUser> load(UUID uuid);

    CompletableFuture<CellUser> loadFromPlayer(Object player);

    Optional<CellUser> cached(UUID uuid);

    default Collection<CellUser> cachedUsers() {
        return List.of();
    }

    Optional<UUID> findUuidByName(String name);

    Collection<UUID> knownUuids();

    /**
     * Applies a mutation that has no return value without using a null sentinel.
     */
    default CompletableFuture<Void> updateVoid(UUID uuid, Consumer<CellUser> mutation) {
        return update(uuid, user -> {
            mutation.accept(user);
            return Boolean.TRUE;
        }).thenAccept(_ -> {
        });
    }

    /**
     * Applies a mutation to a defensive user copy, persists it, then atomically publishes it.
     */
    <T> CompletableFuture<T> update(UUID uuid, Function<CellUser, T> mutation);

    void markDirty(UUID uuid);

    CompletableFuture<Void> save(UUID uuid);

    CompletableFuture<Void> saveAll();

}
