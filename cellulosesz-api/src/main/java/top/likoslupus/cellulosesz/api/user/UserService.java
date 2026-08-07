package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface UserService {

    CompletableFuture<CellUser> load(UUID uuid);

    CompletableFuture<CellUser> loadFromPlayer(CellPlayer player);

    Optional<CellUser> cached(UUID uuid);

    Collection<CellUser> cachedUsers();

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

    <T extends @Nullable Object> CompletableFuture<T> update(
            UUID uuid,
            Function<CellUser, UserUpdate<T>> mutation
    );

    CompletableFuture<Void> save(UUID uuid);

    CompletableFuture<Void> saveAll();

}
