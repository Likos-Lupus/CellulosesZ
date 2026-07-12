package top.likoslupus.cellulosesz.api.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserService {

    CompletableFuture<CellUser> load(UUID uuid);

    CompletableFuture<CellUser> loadFromPlayer(Object player);

    Optional<CellUser> cached(UUID uuid);

    default Collection<CellUser> cachedUsers() {
        return List.of();
    }

    Optional<UUID> findUuidByName(String name);

    void markDirty(UUID uuid);

    CompletableFuture<Void> save(UUID uuid);

    CompletableFuture<Void> saveAll();

}
