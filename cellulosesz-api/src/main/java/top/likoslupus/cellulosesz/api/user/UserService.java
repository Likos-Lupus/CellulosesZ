package top.likoslupus.cellulosesz.api.user;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserService {

    CompletableFuture<CellUser> load(UUID uuid);

    CompletableFuture<CellUser> loadFromPlayer(Object player);

    Optional<CellUser> cached(UUID uuid);

    Optional<UUID> findUuidByName(String name);

    void markDirty(UUID uuid);

    CompletableFuture<Void> save(UUID uuid);

    CompletableFuture<Void> saveAll();

}
